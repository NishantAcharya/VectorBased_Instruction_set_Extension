import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;

public class Instruction {

    private String strValue;
    private int type;
    private int opCode;
    private ArrayList<Integer> params;
    private ArrayList<String> stagesDone;
    private int offset; //for branching, the number of lines to skip on branching

    public Instruction(String str) {
        this.strValue = str;
        this.stagesDone = new ArrayList<>();
        this.params = new ArrayList<>();


    }

    public int getOffset(){
        return this.offset;
    }

    public void setOffset(int offset){
        this.offset = offset;
    }

    public String getStrValue(){
        return this.strValue;
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

    public void setParams(ArrayList<Integer> params){
        this.params = params;
    }

    public ArrayList<Integer> getParams(){
        return this.params;
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

    @Override
    public String toString() {
        return strValue;
    }
}
