import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Pipeline implements NotifyAvailable {

    private final Stage[] stages;

    private boolean firstStageAvailable = true;
    private boolean runningProgram = false;

    private Cache cache;//Access to the cache and memory operations
    private Registers registers;
    private Memory RAM;

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

        // Allows stage to notify previous stage when it is available
        stage5.setToNotify(stage4);
        stage4.setToNotify(stage3);
        stage3.setToNotify(stage2);
        stage2.setToNotify(stage1);
        stage1.setToNotify(this);

        stages = new Stage[] { stage1, stage2, stage3, stage4, stage5 };

    }

    int instrID = 0;

    public void run(int programAddress) {
        runningProgram = true;

        // Pre-set variables to track instructions
        instrID = 0;
        endID = Integer.MAX_VALUE;

        // Set PC to address of program
        registers.set(15, programAddress);

        if (firstStageAvailable) {
            int currID = instrID;
            instrID++;

            stages[0].run(new Instruction(currID));
            firstStageAvailable = false;
        }
    }

    @Override
    public void nextStageAvailable() {
        if (runningProgram && instrID < endID) {
            int currID = instrID;
            instrID++;

            stages[0].run(new Instruction(currID));
            firstStageAvailable = false;
        }
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
            int PC;
            System.out.println("INSTR_" + i.id + ": Running at " + name + ": " + i);

            // Checking which version of run to go with
            switch (name) {
                case "Fetch":
                    int out = Memory.WAIT;
                    PC = registers.get(15);

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
                    break;
                case "Execute": {
                    this.instruction = i;
                    int type = instruction.getType();
                    int opCode = instruction.getOpCode();
                    ArrayList<Integer> params = instruction.getParams();

                    switch (type) {
                        case 0: // Data Processing with 3 operands (rd = r1 + r2)
                            switch (opCode) {
                                case 0:
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);
                                    params.add(registers.get(r_1) + registers.get(r_2));
                                //    System.out.println(params.get(3));
                                    break;
                                case 12:
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);
                                    int cond = params.get(0);//Getting condition code
                                    if(r_1 < r_2){
                                        registers.set(13,4);
                                    }
                                    break;
                            }
                            break;
                        case 6: // Load/Store Immediate
                            switch (opCode) {
                                case 13:
                                    //Only direct load happen in execute stage, all memory based loads happen in memory stage
                                    registers.set(params.get(0), params.get(1));
                                    break;
                                case 14:
                                    //Store gets executed in the write back or memory access stage
                                    break;
                            }
                            break;
                        default:
                            break;
                    }

                    break;
                }
                case "Memory Access": {
                    this.instruction = i;
                    //Needs to be implemented Bracnh,Load and Store
                    int type = instruction.getType();
                    int opCode = instruction.getOpCode();
                    ArrayList<Integer> params = instruction.getParams();
                    switch (type) {
                        case 5: // Load/Store
                            if (opCode == 14) { // Store
                                int address = registers.get(params.get(0));
                                out = Memory.WAIT;

                                //getting line
                                while (out == Memory.WAIT) {
                                    out = RAM.write("Pipeline", address, registers.get(params.get(1)));
                                    System.out.println("Memory returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                                }

  //                              int[] line = {cache.read(name, tag), cache.read(name, tag + 1), cache.read(name, tag + 2), cache.read(name, tag + 3)};
    //                            line[offset] = registers.get(params.get(1));
 //                               System.out.println("Param to store is " + line[offset]);
      //                          cache.directWrite(tag, line, address, name, true);
                            }
                            break;
                        case 6: // Load/Store immediate
                            if (opCode == 14) { // Store
                                int address = registers.get(params.get(0));
                                out = Memory.WAIT;

                                //getting line
                                while (out == Memory.WAIT) {
                                    out = RAM.write("Pipeline", address,registers.get(params.get(1)));
                                    System.out.println("Memory returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                                }

      //                          int[] line = {cache.read(name, tag), cache.read(name, tag + 1), cache.read(name, tag + 2), cache.read(name, tag + 3)};
    //                            line[offset] = params.get(1);
 //                               System.out.println("Param to store is " + line[offset]);
   //                             cache.directWrite(tag, line, address, name, true);
                            }
                            break;
                        case 7:
                            int cond = params.get(0);//Getting condition code
                            switch (cond){
                                //Check CSPR val and see if it equal to cond
                                case 0:
                                    break;
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    if(cond == registers.get(13)){
                                        PC = registers.get(15);
                                        registers.set(14,PC);
                                        registers.set(15, PC + params.get(1));
                                    }
                                    break;
                                case 15://Ask about how to deal with function calls in function calls
                                    PC = registers.get(15);
                                    registers.set(14,PC);
                                    registers.set(15, PC + params.get(1));
                                    break;
                            }
                            break;

                    }
                    break;
                }
                case "Write Back": {
                    this.instruction = i;

                    int type = instruction.getType();
                    int opCode = instruction.getOpCode();
                    ArrayList<Integer> params = instruction.getParams();

                    switch (type) {
                        case 0:
                            switch (opCode) {
                                case 0:
                                    registers.set(params.get(0), params.get(3));
                                    break;
                            }
                            break;
                        case 6:
                            switch (opCode) {
                                case 13:
                                    break;
                                case 14:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
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

            // Wait for next stage to be available
            if (nextStage != null) {
                if (nextStageAvailable) {
                    runOnNextStage();
                }
            } else {
                // Last stage of pipeline
                System.out.println("INSTR_" + instruction.id + ": Pipeline finished");
                toNotify.nextStageAvailable();
            }
        }

        public void runOnNextStage() {
            nextStageAvailable = false;
            Instruction instrForNextStage = instruction;
            instruction = null;

            toNotify.nextStageAvailable();
            new Thread(() -> nextStage.run(instrForNextStage)).start();
        }

        public void setToNotify(NotifyAvailable na) {
            this.toNotify = na;
        }

        @Override
        public void nextStageAvailable() {
            nextStageAvailable = true;

            if (instruction != null && finishedRun) {
                runOnNextStage();
            }
        }
    }
}

interface NotifyAvailable {
    void nextStageAvailable();
}