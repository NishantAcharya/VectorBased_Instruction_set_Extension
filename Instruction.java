import java.util.ArrayList;
import java.util.HashMap;

public class Instruction {

    public static final int HALT = 0b00001111000000000000000000000000;

    private HashMap<Integer, String> opMap;
    private HashMap<Integer, String> condMap;
    private final HashMap<String, Runnable> callbacks;

    private final ArrayList<AddressValuePair> writebackRegisters;
    private final ArrayList<AddressValuePair> writebackMem;

    private String binaryValue;
    private String strValue;
    private int type;
    private int opCode;
    private int condCode;
    private final ArrayList<Integer> params;
    private final ArrayList<String> stagesDone;
    private int offset; // for branching, the number of lines to skip on branching

    public int id;
    public final ArrayList<Integer> stallRegisters;
    private final ArrayList<Integer> dependsOnRegisters;

    public Instruction(int id) {
        this.id = id;
        this.binaryValue = "";
        this.strValue = "";
        this.stagesDone = new ArrayList<>();
        this.params = new ArrayList<>();

        this.callbacks = new HashMap<>();
        callbacks.put("Fetch", null);
        callbacks.put("Decode", null);
        callbacks.put("Execute", null);
        callbacks.put("Memory Access", null);
        callbacks.put("Write Back", null);

        writebackRegisters = new ArrayList<>();
        writebackMem = new ArrayList<>();
        stallRegisters = new ArrayList<>();
        dependsOnRegisters = new ArrayList<>();

        this.buildOpMap();
        this.buildCondMap();
    }

    public boolean dependsOn(Instruction other) {
        for (Integer reg : dependsOnRegisters)
            if (!other.stagesDone.contains("Write Back") && other.stallRegisters.contains(reg))
                return true;

        if (isBranchingInstruction() && other.getOpCode() == 12)
            return true;

        return false;
    }

    public void saveToWriteBack(int address, int value, boolean inRegister) {
        AddressValuePair avp = new AddressValuePair(address, value);

        if (inRegister) {
            writebackRegisters.add(avp);
        } else {
            writebackMem.add(avp);
        }
    }

    public ArrayList<AddressValuePair> getAVPsToWriteBack(boolean inRegister) {
        return inRegister ? writebackRegisters : writebackMem;
    }

    public void instructionToBinaryString(int instr) {
        this.binaryValue = Long.toBinaryString( Integer.toUnsignedLong(instr) | 0x100000000L).substring(1);
    }

    public boolean isBranchingInstruction() {
        try {
            int instr = Integer.parseInt(binaryValue, 2);
            return (instr & 0b00001111000000000000000000000000) >> 24 == 7;
        } catch (Exception e) {
            return false;
        }
    }

    public void addCallback(String stage, Runnable r) {
        if (stagesDone.contains(stage))
            r.run();
        else
            callbacks.put(stage, r);
    }

    public void runCallbacks(String stage) {
        Runnable r = callbacks.get(stage);

        if (r != null) {
            callbacks.put(stage, null);
            r.run();
        }
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

        this.type =   (instr & 0b00001111000000000000000000000000) >> 24;
        this.opCode = (instr & 0b00000000111100000000000000000000) >> 20;

        int r_d, r_1, r_2, imm, sign;

        switch(type){
            case 0: // Data Processing with 3 operands (rd = r1 + r2)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                r_2 = (instr & 0b00000000000000000000011110000000) >> 7;
                condCode = (instr & 0b11110000000000000000000000000000) >> 28;

                params.add(r_d);
                params.add(r_1);
                params.add(r_2);

                stallRegisters.add(r_d);
                dependsOnRegisters.add(r_1);
                dependsOnRegisters.add(r_2);

                this.strValue = opMap.get(opCode) + " R" + r_d + " R" + r_1 + " R" + r_2;
                break;
            case 3: // Data Processing with operand and immediate (rd = r1 + 3)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                imm = (instr & 0b00000000000000000000011111111000) >> 3;
                condCode = (instr & 0b11110000000000000000000000000000) >> 28;

                params.add(r_d);
                params.add(r_1);
                params.add(imm);

                stallRegisters.add(r_d);
                dependsOnRegisters.add(r_1);

                this.strValue = opMap.get(opCode) + " R" + r_d + " R" + r_1 + " " + imm;
                break;
            case 5: // Load/Store
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr  & 0b0000000000000000111100000000000) >> 11;

                params.add(r_d);
                params.add(r_1);

                dependsOnRegisters.add(r_1);

                if (opCode == 13) {
                    stallRegisters.add(r_d);
                } else {
                    dependsOnRegisters.add(r_d);
                }

                this.strValue = opMap.get(opCode) + " R" + r_d + " R" + r_1;
                break;
            case 6: // Load/Store immediate
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                imm = (instr  & 0b00000000000000000111111111111000) >> 3;

                params.add(r_d);
                params.add(imm);

                if (opCode == 13)
                    stallRegisters.add(r_d);
                else
                    dependsOnRegisters.add(r_d);

                this.strValue = opMap.get(opCode) + " R" + r_d + " " + imm;
                break;
            case 7:
                condCode = (instr & 0b11110000000000000000000000000000) >> 28; //Condition
                sign = (instr & 0b00000000010000000000000000000000) >> 22;
                r_1 = (instr & 0b00000000001111111111111111111111); //Offset/number of lines of code to jump

                r_1 *= (sign == 1 ? -1 : 1);
                params.add(r_1);

                strValue = "BRANCH " + r_1 + " IF " + condMap.get(condCode);
                break;
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
        opMap.put(7, "BRANCH");
    }

    private void buildCondMap() {
        condMap = new HashMap<>();
        condMap.put(0, "EQ");
        condMap.put(1, "NE");
        condMap.put(2, "GT");
        condMap.put(4, "LT");
        condMap.put(8, "CARRY OUT");
        condMap.put(3, "ZERO");
        condMap.put(5, "NON-ZERO");
        condMap.put(6, "TRANSPOSE");
        condMap.put(7, "NO COND");
    }

    public int getCondCode() {
        return condCode;
    }

    public void setCondCode(int condCode) {
        this.condCode = condCode;
    }

    public class AddressValuePair {
        int address, value;

        public AddressValuePair(int address, int value) {
            this.address = address;
            this.value = value;
        }
    }
}
