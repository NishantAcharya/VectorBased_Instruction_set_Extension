import java.util.ArrayList;
import java.util.HashMap;

public class Instruction {

    public static final int HALT = 0b00001111000000000000000000000000;

    private HashMap<Integer, String> opMap;
    private String binaryValue;
    private String strValue;
    private int type;
    private int opCode;
    private ArrayList<Integer> params;
    private ArrayList<String> stagesDone;
    private int offset; // for branching, the number of lines to skip on branching
    public int id;

    public Instruction(int id) {
        this.id = id;
        this.binaryValue = "";
        this.strValue = "";
        this.stagesDone = new ArrayList<>();
        this.params = new ArrayList<>();

        this.buildOpMap();
    }

    public void instructionToBinaryString(int instr) {
        this.binaryValue = Long.toBinaryString( Integer.toUnsignedLong(instr) | 0x100000000L).substring(1);
    }

    public int getOffset(){
        return this.offset;
    }

    public void setOffset(int offset){
        this.offset = offset;
    }

    public void addStage(String pipStg){
        this.stagesDone.add(pipStg);
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
        return strValue.isEmpty() ? binaryValue : strValue;
    }

    public void decode() {
        int instr = 0;

        instr = Integer.parseInt(binaryValue, 2);

        this.type = (instr & 0b00001111000000000000000000000000) >> 24;
        this.opCode = (instr & 0b00000000111100000000000000000000) >> 20;

        //Defining parameters, all of them may not be used
        int r_d, r_1, r_2;

        switch(type){
            case 0: // Data Processing with 3 operands (rd = r1 + r2)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                r_2 = (instr & 0b00000000000000000000011110000000) >> 7;
                params.add(r_d);
                params.add(r_1);
                params.add(r_2);

                this.strValue = opMap.get(opCode) + " R" + r_d + " R" + r_1 + " R" + r_2;
                break;
            case 5: // Load/Store
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr  & 0b0000000000000000111100000000000) >> 11;
                params.add(r_d);
                params.add(r_1);

                this.strValue = opMap.get(opCode) + " R" + r_d + " R" + r_1;
                break;
            case 6: // Load/Store immediate
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr  & 0b00000000000000000111111111111000) >> 3;
                params.add(r_d);
                params.add(r_1);

                this.strValue = opMap.get(opCode) + " R" + r_d + " " + r_1;
                break;
            case 7:
                r_d = (instr & 0b11110000000000000000000000000000) >> 28;//Condition
                r_1 = (instr & 0b00000000011111111111111111111111);//Offset/number of lines of code to jump
                params.add(r_d);
                params.add(r_1);
            default:
                this.strValue = "INVALID TYPE";
                break;
        }
    }

    public boolean checkIfHalt(int instr) {
        if (instr == Instruction.HALT) {
            strValue = "HALT";
            return true;
        }

        return false;
    }

    private void buildOpMap() {
        opMap = new HashMap<>();
        opMap.put(0, "ADD");
        opMap.put(1, "SUBTRACT");
        opMap.put(2, "MULTIPLY");
        opMap.put(4, "DIVIDE");
        opMap.put(8, "MODULO");
        opMap.put(3, "AND");
        opMap.put(5, "OR");
        opMap.put(9, "NOT");
        opMap.put(10, "XOR");
        opMap.put(12, "COMPARE");
        opMap.put(6, "SET FLAG");
        opMap.put(7, "SHIFT");
        opMap.put(11, "SWAP");
        opMap.put(13, "LOAD");
        opMap.put(14, "STORE");
        opMap.put(15, "BRANCH");
    }
}
