import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Pipeline implements NotifyAvailable {

    private final ArrayList<Instruction> toRun; // Instructions that haven't been through any stage yet
    private final Stage[] stages;
    private boolean firstStageAvailable = true;
    private Cache memAccess;//Access to the cache and memory operations

    public Pipeline(Cache mem) {
        toRun = new ArrayList<>();
        this.memAccess = mem;

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
    //Access - Check the memory location for writeback
    //Writeback - Write
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
                }

                this.instruction = i;
            }
            else if(name.equals("Decode")){

            }
            else if(name.equals("Execute")){}
            else if(name.equals("Memory Access")){}
            else if(name.equals("Write Back")){}
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