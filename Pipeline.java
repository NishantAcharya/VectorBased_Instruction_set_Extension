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

    public void run(int programAddress) {
        runningProgram = true;
        registers.set(15, programAddress);

        if (firstStageAvailable) {
            stages[0].run(new Instruction());
            firstStageAvailable = false;
        }
    }

    @Override
    public void nextStageAvailable() {
        if (runningProgram) {
            stages[0].run(new Instruction());
            firstStageAvailable = false;
        }
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
            this.instruction = i;
            System.out.println("Running at " + name + ": " + i);

            // Checking which version of run to go with
            if (name.equals("Fetch")){
                int out = Memory.WAIT;
                int PC = registers.get(15);

                while (out == Memory.WAIT) {
                    out = RAM.read(name, PC);
                }

                instruction.instructionToBinaryString(out);
                instruction.addStage(name);

                registers.set(15, PC + 1);
            } else if(name.equals("Decode")){
                instruction.decode();
            } else if(name.equals("Execute")){
                this.instruction = i;
                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer> params = instruction.getParams();

                switch (type){
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
                    case 6:
                        switch(opCode){

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
            } else if (name.equals("Memory Access")){
                this.instruction = i;
                //Needs to be implemented Bracnh,Load and Store
                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer>params = instruction.getParams();
                switch(type){
                    case 5:
                        if(opCode == 14){
                            int address = registers.get(params.get(0));
                            int offset = address%4;
                            int tag = address - offset;
                            int out = Memory.WAIT;

                            //getting line
                            while (out == Memory.WAIT) {
                                out = cache.read("Pipeline", address);
                                System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                            }

                            int[] line = {cache.read(name,tag), cache.read(name,tag+1), cache.read(name,tag+2), cache.read(name,tag+3)};
                            line[offset] = registers.get(params.get(1));
                            System.out.println("Param to store is " + line[offset]);
                            cache.directWrite(tag,line,address,name,true);
                        }
                        break;
                    case 6:
                        if(opCode == 14){
                            int address = registers.get(params.get(0));
                            int offset = address%4;
                            int tag = address - offset;
                            int out = Memory.WAIT;

                            //getting line
                            while (out == Memory.WAIT) {
                                out = cache.read("Pipeline", address);
                                System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                            }

                            int[] line = {cache.read(name,tag), cache.read(name,tag+1), cache.read(name,tag+2), cache.read(name,tag+3)};
                            line[offset] = params.get(1);
                            System.out.println("Param to store is " + line[offset]);
                            cache.directWrite(tag,line,address,name,true);
                        }
                        break;
                    case 7:
                        break;
                }
            } else if(name.equals("Write Back")){
                this.instruction = i;

                int type = instruction.getType();
                int opCode = instruction.getOpCode();
                ArrayList<Integer> params = instruction.getParams();

                switch (type) {
                    case 0:
                        switch(opCode){
                            case 0:
                                registers.set(params.get(0),params.get(3));
                                break;
                        }
                        break;
                    case 6:
                        switch(opCode){

                            case 13:

                                break;
                            case 14:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }

            instruction.addStage(name);
            finishedRun = true;

            if (instruction.getStrValue().equals("END")) {
                System.out.println("Reached END Instruction");
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
                System.out.println("Pipeline finished for: " + instruction);
                toNotify.nextStageAvailable();
            }
        }

        public void runOnNextStage() {
            nextStageAvailable = false;
            Instruction instrForNextStage = instruction;
            instruction = null;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    nextStage.run(instrForNextStage);
                }
            }).start();

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