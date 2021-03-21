import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;

public class Instruction {

    private String strValue;
    private int type;
    private int opCode;
    private int[] params;
    private ArrayList<String> stagesDone;

    public Instruction(String str) {
        this.strValue = str;
        //The following code will be done in the decode stage
        /*
        String[] values = str.split(" ");
        this.type = Type.valueOf(values[0]);
        this.params = Arrays.copyOfRange(values, 1, values.length);
        */

    }

    public void addStage(String pipStg){
        this.stagesDone.add(pipStg);
    }

    public void setType(int type){
        this.type = type;
    }

    public void setOpCode(int opCode){
        this.opCode = opCode;
    }

    public void setParams(int[] params){
        this.params = params;
    }

    public String getLastStage(){
        return this.stagesDone.get(stagesDone.size()-1);
    }

    public int getType(){
        return this.type;
    }

    public int getOpCode(){
        return this.opCode;
    }


    public enum Type {
        ADD, LOAD, STORE;
    }

    @Override
    public String toString() {
        return strValue;
    }
}
