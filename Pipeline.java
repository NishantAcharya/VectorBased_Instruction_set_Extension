import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Pipeline implements NotifyAvailable {

    private final ArrayList<Instruction> toRun; // Instructions that haven't been through any stage yet
    private final Stage[] stages;
    private boolean firstStageAvailable = true;
    private Cache memAccess;//Access to the cache and memory operations
    ArrayList<Instruction> instructionList;
    private Registers registers;

    public Pipeline(Cache mem) {
        toRun = new ArrayList<>();
        this.memAccess = mem;
        registers = new Registers(16);

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

    public void run(ArrayList<Instruction> instructions) {
        this.instructionList = instructions;
        toRun.addAll(instructions);

        if (firstStageAvailable) {
            Instruction instr = toRun.remove(0);
            stages[0].run(instr);
            firstStageAvailable = false;
        }
    }

    @Override
    public void nextStageAvailable() {
        if (toRun.isEmpty()) {
            firstStageAvailable = true;
            return;
        }

        Instruction instr = toRun.remove(0);
        stages[0].run(instr);
        firstStageAvailable = false;
    }

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
            this.finishedRun = false;
            System.out.println("Running at " + name + ": " + i);
            //Checking which version of run to go with
            if(name.equals("Fetch")){
                int memDelay = 3;
                //Simulating wait for accessing instruction stored in memory
                while(memDelay > 0){
                    System.out.println("Waiting on Memory Access");
                    memDelay--;
                }
                //"Fetched" The instruction
                this.instruction = i;
                instruction.addStage(name);
            }
            else if(name.equals("Decode")){
                this.instruction = i;
                int instr = Integer.parseInt(instruction.getStrValue(),2);
                int type = (instr & 0b00001111000000000000000000000000)>>24;
                int opCode = (instr & 0b00000000111100000000000000000000) >> 20;
                instruction.setOpCode(opCode);
                instruction.setType(type);
                //Defining parameters, all of them may not be used
                ArrayList<Integer> params = new ArrayList<>();
                int r_d=0;
                int r_1=0;
                int r_2=0;
                int offset = 0; //number of program lines in the branch

                switch(type){
                    case 0:
                        switch(opCode){
                            case 0:
                                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                                r_2 = (instr & 0b00000000000000000000011110000000) >> 7;
                                params.add(r_d);
                                params.add(r_1);
                                params.add(r_2);
                                instruction.setParams(params);
                                break;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        switch(opCode){

                            case 13:
                                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                                r_1 = (instr  & 0b00000000000000000111111111111000) >> 3;
                                params.add(r_d);
                                params.add(r_1);
                                instruction.setParams(params);

                                break;
                            case 14:
                                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                                r_1 = (instr  & 0b00000000000000000111111111111000) >> 3;
                                params.add(r_d);
                                params.add(r_1);
                                instruction.setParams(params);
                                System.out.println(r_1);
                                break;
                        }
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 15:
                        break;
                }
                instruction.addStage(name);
            }
            else if(name.equals("Execute")){
                this.instruction = i;
                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer>params = instruction.getParams();
                switch(type){
                    case 0:
                        switch(opCode){
                            case 0:
                                int r_1 = params.get(1);
                                int r_2 = params.get(2);
                                params.add(registers.get(r_1)+ registers.get(r_2));
                                System.out.println(params.get(3));
                                break;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        switch(opCode){

                            case 13:
                                //Only direct load happen in execute stage, all memory based loads happen in memory stage
                                registers.set(params.get(0),params.get(1));
                                break;
                            case 14:
                                //Store gets executed in the writeback or memmory access stage
                                break;
                        }
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 15:
                        break;
                }
                instruction.addStage(name);
            }
            else if(name.equals("Memory Access")){
                this.instruction = i;
                //Needs to be implemented Bracnh,Load and Store
                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer>params = instruction.getParams();
                switch(type){
                    case 5:

                        break;
                    case 6:
                        if(opCode == 14){
                            int address = registers.get(params.get(0));
                            int offset = address%4;
                            int tag = address - offset;
                            int out = Memory.WAIT;

                            //getting line
                            while (out == Memory.WAIT) {
                                out = memAccess.read("Pipeline", address);
                                System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                            }

                            int[] line = {memAccess.read(name,tag),memAccess.read(name,tag+1),memAccess.read(name,tag+2),memAccess.read(name,tag+3)};
                            line[offset] = params.get(1);
                            System.out.print("Param to store is " + line[offset]);
                            memAccess.directWrite(tag,line,address,name,true);
                        }
                        break;
                    case 7:
                        break;
                }
                instruction.addStage(name);
            }
            else if(name.equals("Write Back")){
                this.instruction = i;
                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer>params = instruction.getParams();
                switch(type){
                    case 0:
                        switch(opCode){
                            case 0:
                                registers.set(params.get(0),params.get(3));
                                break;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        switch(opCode){

                            case 13:

                                break;
                            case 14:

                                break;
                        }
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        break;
                    case 10:
                        break;
                    case 15:
                        break;
                }
                instruction.addStage(name);
            }

            // Simulate stage taking time
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    finishedRun = true;

                    // Wait for next stage to be available
                    if (nextStage != null) {
                        if (nextStageAvailable) {
                            runOnNextStage();
                        }
                    } else {
                        // Last stage of pipeline
                        System.out.println("Pipeline finished for: " + instruction);
                        toNotify.nextStageAvailable();
                    }
                }
            }, 1000);
        }

        public void runOnNextStage() {
            nextStageAvailable = false;
            nextStage.run(instruction);
            instruction = null;

            toNotify.nextStageAvailable();
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