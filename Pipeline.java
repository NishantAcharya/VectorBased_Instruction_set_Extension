import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.swing.text.TableView;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class Pipeline implements NotifyAvailable {

    private final Stage[] stages;

    private boolean firstStageAvailable = true;
    private boolean runningProgram = false;
    private boolean usePipeline = true;

    private Cache cache;//Access to the cache and memory operations
    private Registers registers;
    private VectorRegisters vectorRegisters;
    private Memory RAM;

    private Runnable completed;

    private ArrayList<Instruction> currInstructions;

    private int endID = Integer.MAX_VALUE;

    public Pipeline(Registers registers, VectorRegisters vectorRegisters, Cache cache, Memory RAM) {
        this.cache = cache;
        this.registers = registers;
        this.vectorRegisters = vectorRegisters;
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
        registers.setPC(programAddress);
        runNewInstruction();
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
            Main.print("INSTR_" + i.id + ": Running at " + name + ": " + i);

            // Checking which version of run to go with
            switch (name) {
                case "Fetch":
                    int out = Memory.WAIT;
                    int PC = registers.getPC();

                    // Gets instruction in memory from address in PC
                    while (out == Memory.WAIT) {
                        out = RAM.read(name, PC);
                    }

                    if (instruction.checkIfHalt(out)) // Check if halt instruction
                        break;

                    instruction.instructionToBinaryString(out);
                    instruction.addStage(name);

                    registers.setPC(PC + 1);
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
                                case 1: // Subtract
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) - registers.get(r_2), true);
                                    break;
                                case 2: // Multiply
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) * registers.get(r_2), true);
                                    break;
                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) / registers.get(r_2), true);
                                    break;
                                case 8: // Modulo(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) % registers.get(r_2), true);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    r_2 = registers.get(params.get(2));

                                    int cmp = compare(r_1, r_2);
                                    instruction.saveToWriteBack(13, cmp, true);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                            break;
                        case 1://Data Processing Register Indirect(both) with 3 operands (rd = ValueAt(r1) + ValueAt(r2))
                            switch(opCode){
                                case 0://Add

                                case 1: // Subtract

                                case 2: // Multiply

                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)

                                case 8: // Modulo(Not the processor's job to catch the dividing by zero error)
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);

                                    instruction.saveToMemAccess(registers.get(r_1),registers.get(r_2),r_d,opCode,type);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    r_2 = registers.get(params.get(2));

                                    instruction.saveToMemAccess(registers.get(r_1),registers.get(r_2),13,opCode,type);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                        case 2://Data Processing Register Indirect(one) with 3 operands (rd = ValueAt(r1) + r2)
                            //The second parameter is an immediate not a register value
                            switch(opCode){
                                case 0://Add

                                case 1: // Subtract

                                case 2: // Multiply

                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)

                                case 8: // Modulo(Not the processor's job to catch the dividing by zero error)
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);

                                    instruction.saveToMemAccess(registers.get(r_1),r_2,r_d,opCode,type);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    r_2 = registers.get(params.get(2));

                                    instruction.saveToMemAccess(registers.get(r_1),r_2,13,opCode,type);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                        case 3: // Data Processing with operand and immediate (rd = r1 + 3)
                            switch (opCode) {
                                case 0: // Add
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) + imm, true);
                                    break;
                                case 1: // Subtract
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) - registers.get(imm), true);
                                    break;
                                case 2: // Multiply
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) * registers.get(imm), true);
                                    break;
                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) / registers.get(imm), true);
                                    break;
                                case 8: // Modulo(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    imm = params.get(2);

                                    instruction.saveToWriteBack(r_d, registers.get(r_1) % registers.get(imm), true);
                                    break;
                                case 12: // Compare
                                    r_1 = registers.get(params.get(1));
                                    imm = params.get(2);

                                    int cmp = compare(r_1, imm);
                                    instruction.saveToWriteBack(13, cmp, true);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                            break;
                        case 5: // Load/Store
                            switch (opCode) {
                                case 13:
                                    //All Loads other than immediate load happen in memory stage
                                    instruction.saveToWriteBack(params.get(0), registers.get(params.get(1)), false);
                                    break;
                                case 14:
                                    //Store gets executed in the write back or memory access stage
                                    int address = registers.get(params.get(0));
                                    int value = registers.get(params.get(1));
                                    instruction.saveToWriteBack(address, value, false);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
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
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                            break;
                        case 8:// Vector Load/Store
                            switch (opCode) {
                                case 13:
                                    //Only direct load happen in execute stage, all memory based loads happen in memory stage
                                    ArrayList<Integer> param = vectorRegisters.get(params.get(1));
                                    int[] v1 = new int[param.size()];
                                    for(int k = 0; k < param.size();k++){
                                        v1[k] = param.get(k);
                                    }
                                    instruction.vectorSaveToWriteBack(params.get(0), v1, false);
                                    break;
                                case 14:
                                    //Store gets executed in the write back or memory access stage
                                    int address = registers.get(params.get(0));
                                    int value = registers.get(params.get(1));
                                    instruction.saveToWriteBack(address, value, false);
                                    break;
                                case 7:
                                    //Append value onto vector
                                    instruction.saveToWriteBack(params.get(0), params.get(1), true);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;

                            }
                            break;
                        case 9:
                            switch (opCode) {
                                case 0: // Add
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);

                                    int len = instruction.getVectorLength();//number of elements
                                    ArrayList<Integer> v1 = vectorRegisters.get(r_1);
                                    ArrayList<Integer> v2 = vectorRegisters.get(r_2);
                                    int[] vd = new int[len];

                                    for(int element = 0; element < len; element++){
                                        vd[element] = v1.get(element) + v2.get(element);
                                    }

                                    instruction.vectorSaveToWriteBack(r_d, vd, true);

                                    break;
                                case 1: // Subtract
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    v2 = vectorRegisters.get(r_2);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)-v2.get(element);
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                case 2: // Multiply
                                    //Vector Multiply needs work
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    v2 = vectorRegisters.get(r_2);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)*v2.get(element);
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    v2 = vectorRegisters.get(r_2);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)/v2.get(element);
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                            break;
                        case 10://Vector Data processing with 2 operands and an immediate (vd = v1 * 3)
                            switch (opCode) {
                                case 0: // Add
                                    int r_d = params.get(0);
                                    int r_1 = params.get(1);
                                    int r_2 = params.get(2);

                                    int len = instruction.getVectorLength();//number of elements
                                    ArrayList<Integer> v1 = vectorRegisters.get(r_1);
                                    int[] vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)+r_2;
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                case 1: // Subtract
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)-r_2;
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                case 2: // Multiply
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)*r_2;
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                case 4: // Divide(Not the processor's job to catch the dividing by zero error)
                                    r_d = params.get(0);
                                    r_1 = params.get(1);
                                    r_2 = params.get(2);

                                    len = instruction.getVectorLength();//number of elements
                                    v1 = vectorRegisters.get(r_1);
                                    vd = new int[len];

                                    for(int element = 0; element < len;element++){
                                        vd[element] = v1.get(element)/r_2;
                                    }
                                    instruction.vectorSaveToWriteBack(r_d, vd, true);
                                    break;
                                default:
                                    System.out.println("Invalid OPcode: "+opCode);
                                    break;
                            }
                            break;
                        default:
                            System.out.println("Invalid Type code" + type);
                            break;
                    }

                    break;
                }
                case "Memory Access": {
                    //Load from memory needs tp happen here, i.e, needs to access the data here, Load from immediate needs to happen in write back
                    ArrayList<Integer> params = instruction.getParams();

                    //For Indirect access
                    for (Instruction.AddressPair ap: instruction.getAPtoMemAccess()) {
                        if(ap.typ == 1){
                            switch(ap.opcode){
                                case 0://Add
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) + cache.read("Memory Access",ap.address_2) , true);
                                    break;
                                case 1://Subtract
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) - cache.read("Memory Access",ap.address_2) , true);
                                    break;
                                case 2://Multiply
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) * cache.read("Memory Access",ap.address_2) , true);
                                    break;
                                case 4://Divide
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) / cache.read("Memory Access",ap.address_2) , true);
                                    break;
                                case 8://Mod
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) % cache.read("Memory Access",ap.address_2) , true);
                                    break;
                                case 12://Compare
                                    int cmp = compare(cache.read("Memory Access",ap.address_1), cache.read("Memory Access",ap.address_2));
                                    instruction.saveToWriteBack(13, cmp, true);
                                    break;
                            }
                        }
                        else if(ap.typ == 2){
                            switch(ap.opcode){
                                case 0://Add
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) + ap.address_2, true);
                                    break;
                                case 1://Subtract
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) - ap.address_2, true);
                                    break;
                                case 2://Multiply
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) * ap.address_2, true);
                                    break;
                                case 4://Divide
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) / ap.address_2, true);
                                    break;
                                case 8://Mod
                                    instruction.saveToWriteBack(ap.destination, cache.read("Memory Access",ap.address_1) % ap.address_2, true);
                                    break;
                                case 12://Compare
                                    int cmp = compare(cache.read("Memory Access",ap.address_1), ap.address_2);
                                    instruction.saveToWriteBack(13, cmp, true);
                                    break;
                            }
                        }
                    }

                    for (Instruction.AddressValuePair avp: instruction.getAVPsToWriteBack(false)) {
                        //Loading from memory at a given register
                        if(avp.typ == 5 && avp.opcode == 13){
                            instruction.saveToWriteBack(avp.address, cache.read("Memory Access", avp.value), true);
                        }
                        else if(avp.typ == 8){
                            if(avp.opcode == 0){//Vector Load from memory(note, the passed result in vd is an int[],change it to Arraylist)
                                int len = instruction.getVectorLength();
                                int start = avp.value;
                                int cacheLen = 4;
                                int fullreads = len/cacheLen;
                                int partialread = len%cacheLen;
                                int[] vd = new int[len];
                                int j = 0; //Value to keep a track of item in vd

                                //Reading full lines
                                for(int readNum=0; readNum < fullreads;readNum++){
                                    int[] line = cache.cacheLineRead(start,4);
                                    //Copying the values in vd
                                    for(int k = 0; k < 4;k++){
                                        vd[j] = line[k];
                                        j++;
                                    }
                                    start += 4;
                                }
                                //Reading partial lines
                                int[] line = cache.cacheLineRead(start,partialread);
                                for(int k = 0; k < partialread;k++){
                                    vd[j] = line[k];
                                    j++;
                                }
                                instruction.vectorSaveToWriteBack(avp.address, vd, true);
                            }
                            else{//Vector store in memory
                                int len = instruction.getVectorLength();
                                int start = avp.value;
                                int cacheLen = 4;
                                int fullstore = len/cacheLen;
                                int partialstore = len%cacheLen;
                                int address = avp.address;

                                //Need to add a store
                            }
                        }
                        else{
                            cache.processorWrite(avp.address, avp.value,"Memory Access");
                        }
                    }

                    if (instruction.isBranchingInstruction()) {
                        int cond = instruction.getCondCode();
                        if(cond != 7) {
                            // If true, branch to PC, else do nothing
                            if (instruction.checkCond(registers.getCND())) {
                                PC = registers.get(15);
                                registers.setPC(PC + params.get(0) - 1);
                            }
                        } else { //looping back
                             int lr = registers.getLR();
                             registers.setPC(lr);
                        }
                    }

                    break;
                }
                case "Write Back": {

                    for (Instruction.AddressValuePair avp: instruction.getAVPsToWriteBack(true)) {
                        int opCode = instruction.getOpCode();
                        int type = instruction.getType();

                        if (type == 8 && opCode == 7) { // Append immediate for vectors(sort of)
                            vectorRegisters.append(avp.address, avp.value);

                            continue;
                        }

                        registers.set(avp.address, avp.value);
                    }
                    for (Instruction.VectorValuePair vp: instruction.getVPtoWriteBack(true)) {
                        ArrayList<Integer> val = new ArrayList<>();
                        for (int k = 0; k < vp.value.length; k++) {
                            val.add(vp.value[k]);
                        }
                        vectorRegisters.set(vp.address, val);
                    }
                    break;
                }
            }

            instruction.addStage(name);
            finishedRun = true;

            if (instruction.toString().equals("HALT")) {
                System.out.println("Reached HALT Instruction (INSTR_" + instruction.id + ")");
                Main.print("Reached HALT Instruction (INSTR_" + instruction.id + ")");
                endID = instruction.id;

                this.instruction = null;
                this.finishedRun = true;

                runningProgram = false;
                return;
            }

            if (dependsOnInstr != null) {
                System.out.println("INSTR_" + instruction.id + ": Stalled until INSTR_" + dependsOnInstr.id + " writes back");
                Main.print("INSTR_" + instruction.id + ": Stalled until INSTR_" + dependsOnInstr.id + " writes back");

                dependsOnInstr.addCallback("Write Back", () -> {
                    System.out.println("INSTR_" + instruction.id + ": No longer stalled");
                    Main.print("INSTR_" + instruction.id + ": No longer stalled");

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
                Main.print("INSTR_" + instruction.id + ": Pipeline finished");
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

        private int compare(int a, int b) {
            String binStr = "";

            binStr = (a == b ? "1" : "0") + binStr; // EQ
            binStr = (a != b ? "1" : "0") + binStr; // NE
            binStr = (a > b ? "1" : "0") + binStr;  // GT
            binStr = (a >= b ? "1" : "0") + binStr; // GTE
            binStr = (a < b ? "1" : "0") + binStr;  // LT
            binStr = (a <= b ? "1" : "0") + binStr; // LTE

            return Integer.parseInt(binStr, 2);
        }
    }
}

interface NotifyAvailable {
    void nextStageAvailable();
}