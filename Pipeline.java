import java.util.ArrayList;

public class Pipeline implements NotifyAvailable {

    private final Stage[] stages;

    private boolean firstStageAvailable = true;
    private boolean runningProgram = false;
    private boolean usePipeline = true;

    private Cache cache;//Access to the cache and memory operations
    private Registers registers;
    private Memory RAM;

    private Runnable completed;

    private ArrayList<Instruction> currInstructions;

    private int endID = Integer.MAX_VALUE;

    public Pipeline(Registers registers, Cache cache, Memory RAM) {
        this.cache = cache;
        this.registers = registers;
        this.RAM = RAM;

        // Replace this with actual stage classes once built
        Stage stage5 = new Stage("Write Back", null);
        Stage stage4 = new Stage("Memory Access", stage5);
        Stage stage3 = new Stage("Execute", stage4);
        Stage stage2 = new Stage("Decode", stage3);
        Stage stage1 = new Stage("Fetch", stage2);

        stages = new Stage[] { stage1, stage2, stage3, stage4, stage5 };
    }

    int instrID = 0;

    public void run(int programAddress, boolean usePipeline, Runnable completed) {
        this.usePipeline = usePipeline;

        stages[0].setToNotify(usePipeline ? this : null);
        stages[1].setToNotify(usePipeline ? stages[0] : null);
        stages[2].setToNotify(usePipeline ? stages[1] : null);
        stages[3].setToNotify(usePipeline ? stages[2] : null);
        stages[4].setToNotify(usePipeline ? stages[3] : this);

        runningProgram = true;
        this.completed = completed;

        // Pre-set variables to track instructions
        instrID = 0;
        endID = Integer.MAX_VALUE;
        currInstructions = new ArrayList<>();

        // Set PC to address of program
        registers.set(15, programAddress);

        if (firstStageAvailable) {
            runNewInstruction();
        }
    }

    @Override
    public void nextStageAvailable() {
        if (runningProgram && instrID < endID) {
            runNewInstruction();
        }
    }

    private void runNewInstruction() {
        int currID = instrID;
        instrID++;

        Instruction instr = new Instruction(currID);
        currInstructions.add(instr);

        firstStageAvailable = false;
        stages[0].run(instr);
    }

    //Need to add conditional elements to each instruction
    // Each stage extends this class
    //Use the next stage to figure out which stage you are in
    //Since there are only 5 stages and they are known, make 5 conditions for run and seperate the different stages
    //fetch - instruction gets assigned to variable,add wait to simulate fetch
    //decode - decode instruction and figure out the instruction
    //Execute - do the operation
    //Access - Check for any memory access,Load,store and branch completion
    //Writeback - Write to destination registers
    private class Stage implements NotifyAvailable {
        protected Instruction instruction;
        protected Stage nextStage;
        private String name;
        private NotifyAvailable toNotify;
        private boolean nextStageAvailable;
        private boolean finishedRun;
        private boolean stalled = false;

        public Stage(String name, Stage nextStage) {
            this.name = name;
            this.nextStage = nextStage;

            nextStageAvailable = true;
            finishedRun = true;
        }

        public void run(Instruction i) {
            if (i.id >= endID) return;

            this.finishedRun = false;
            this.instruction = i;

            Instruction dependsOnInstr = null;

            System.out.println("INSTR_" + i.id + ": Running at " + name + ": " + i);

            // Checking which version of run to go with
            switch (name) {
                case "Fetch":
                    int out = Memory.WAIT;
                    int PC = registers.get(15);

                    // Gets instruction in memory from address in PC
                    while (out == Memory.WAIT) {
                        out = RAM.read(name, PC);
                    }

                    if (instruction.checkIfHalt(out)) // Check if halt instruction
                        break;

                    instruction.instructionToBinaryString(out);
                    instruction.addStage(name);

                    registers.set(15, PC + 1);
                    break;
                case "Decode":
                    instruction.decode();

                    // Check if dependant on another instruction
                    for (Object obj:  currInstructions.toArray()) {
                        Instruction instr = (Instruction) obj;
                        if (instruction.id == instr.id) continue;

                        if (instruction.dependsOn(instr)) {
                            stalled = true;

                            if (dependsOnInstr == null || instr.id > dependsOnInstr.id)
                                dependsOnInstr = instr;
                        }
                    }
                    break;
                case "Execute": {
                    int type = instruction.getType();
                    int opCode = instruction.getOpCode();
                    ArrayList<Integer> params = instruction.getParams();

                    switch (type) {
                        case 0: // Data Processing with 3 operands (rd = r1 + r2)
                            switch (opCode) {
                                case 0: // Add
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) + registers.get(r_2), true);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    r_2 = registers.get(params.get(2));

                                    int cond = r_1 < r_2 ? 4 : -1;
                                    instruction.saveToWriteBack(13, cond, true);
                                    break;
                            }
                            break;
                        case 3: // Data Processing with operand and immediate (rd = r1 + 3)
                            switch (opCode) {
                                case 0: // Add
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) + imm, true);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    imm = params.get(2);

                                    int cond = r_1 < imm ? 4 : -1;
                                    instruction.saveToWriteBack(13, cond, true);
                                    break;
                            }
                            break;
                        case 5: // Load/Store
                            switch (opCode) {
                                case 13:
                                    //Only direct load happen in execute stage, all memory based loads happen in memory stage
                                    instruction.saveToWriteBack(params.get(0), registers.get(params.get(1)), true);
                                    break;
                                case 14:
                                    //Store gets executed in the write back or memory access stage
                                    int address = registers.get(params.get(0));
                                    int value = registers.get(params.get(1));
                                    instruction.saveToWriteBack(address, value, false);
                                    break;
                            }
                            break;
                        case 6: // Load/Store Immediate
                            switch (opCode) {
                                case 13:
                                    //Only direct load happen in execute stage, all memory based loads happen in memory stage
                                    instruction.saveToWriteBack(params.get(0), params.get(1), true);
                                    break;
                                case 14:
                                    //Store gets executed in the write back or memory access stage
                                    int address = registers.get(params.get(0));
                                    int value = params.get(1);
                                    instruction.saveToWriteBack(address, value, false);
                                    break;
                            }
                            break;
                        default:
                            break;
                    }

                    break;
                }
                case "Memory Access": {
                    // Needs to be implemented Branch,Load and Store
                    ArrayList<Integer> params = instruction.getParams();

                    for (Instruction.AddressValuePair avp: instruction.getAVPsToWriteBack(false)) {
                        cache.processorWrite(avp.address, avp.value, name);
                    }

                    if (instruction.isBranchingInstruction()) {
                        int cond = instruction.getCondCode();

                        if (registers.get(13) == cond) {
                            PC = registers.get(15);
                            registers.set(15, PC + params.get(0) - 1);
                        }
                    }

                    break;
                }
                case "Write Back": {
                    for (Instruction.AddressValuePair avp: instruction.getAVPsToWriteBack(true))
                        registers.set(avp.address, avp.value);
                    break;
                }
            }

            instruction.addStage(name);
            finishedRun = true;

            if (instruction.toString().equals("HALT")) {
                System.out.println("Reached HALT Instruction (INSTR_" + instruction.id + ")");
                endID = instruction.id;

                this.instruction = null;
                this.finishedRun = true;

                runningProgram = false;
                return;
            }

            if (dependsOnInstr != null) {
                System.out.println("INSTR_" + instruction.id + ": Stalled until INSTR_" + dependsOnInstr.id + " writes back");

                dependsOnInstr.addCallback("Write Back", () -> {
                    System.out.println("INSTR_" + instruction.id + ": No longer stalled");

                    stalled = false;
                    stageFinished();
                });
            } else {
                stageFinished();
            }
        }

        private void stageFinished() {
            // Checks if there are any callbacks associated with current stage
            instruction.runCallbacks(name);

            // Wait for next stage to be available
            if (nextStage != null) {
                if (nextStageAvailable || !usePipeline) {
                    runOnNextStage();
                }
            } else {
                currInstructions.remove(instruction);

                // Last stage of pipeline
                System.out.println("INSTR_" + instruction.id + ": Pipeline finished");
                notifyLastStage();

                if (instruction.id >= endID - 1)
                    if (completed != null)
                        completed.run();
            }
        }

        public void runOnNextStage() {
            if (stalled || instruction == null) return;

            nextStageAvailable = false;
            Instruction instrForNextStage = instruction;
            instruction = null;

            if (instrForNextStage.isBranchingInstruction() && name.equals("Fetch")) {
                instrForNextStage.addCallback("Memory Access", () -> notifyLastStage());
            } else {
                notifyLastStage();
            }

            new Thread(() -> nextStage.run(instrForNextStage)).start();
        }

        private void notifyLastStage() {
            if (toNotify != null)
                toNotify.nextStageAvailable();
        }

        public void setToNotify(NotifyAvailable na) {
            this.toNotify = na;
        }

        @Override
        public void nextStageAvailable() {
            nextStageAvailable = true;

            if (instruction != null && finishedRun && !stalled) {
                runOnNextStage();
            }
        }
    }
}

interface NotifyAvailable {
    void nextStageAvailable();
}