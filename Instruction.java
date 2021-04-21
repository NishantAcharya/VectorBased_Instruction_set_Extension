import java.util.ArrayList;
import java.util.HashMap;

public class Instruction {

    public static final int HALT = 0b00001111000000000000000000000000;

    private HashMap<Integer, String> opMap;
    private HashMap<Integer, String> opMapV;
    private HashMap<Integer, String> condMap;
    private final HashMap<String, Runnable> callbacks;

    private  ArrayList<AddressValuePair> writebackRegisters;
    private  ArrayList<AddressValuePair> writebackMem;
    private  ArrayList<VectorValuePair> vectorWritebackRegisters;
    private  ArrayList<VectorValuePair> vectorWritebackMem;
    private  ArrayList<AddressPair> memoryAccessRegisters;

    private String binaryValue;
    private String strValue;
    private int type = -1;
    private int opCode = -1;
    private int condCode;
    private int linkCode = -1; //Setting the link code to an invalid value
    private int vectorLength = -1;
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
        memoryAccessRegisters = new ArrayList<>();
        vectorWritebackMem = new ArrayList<>();
        vectorWritebackRegisters = new ArrayList<>();
        stallRegisters = new ArrayList<>();
        dependsOnRegisters = new ArrayList<>();

        this.buildOpMap();
        this.buildOpMapV();
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

    public void saveToMemAccess(int add_1,int add_2,int destination, int opCode, int type){
        AddressPair ap = new AddressPair(add_1,add_2,destination,opCode,type);
        memoryAccessRegisters.add(ap);
    }

    public ArrayList<AddressPair> getAPtoMemAccess(){
        return memoryAccessRegisters;
    }

    public void vectorSaveToWriteBack(int address,int[] value,boolean inRegister){
        VectorValuePair avp = new VectorValuePair(address, value);

        if (inRegister) {
            vectorWritebackRegisters.add(avp);
        } else {
            vectorWritebackMem.add(avp);
        }
    }

    public ArrayList<VectorValuePair> getVPtoWriteBack(boolean inRegister){
        return inRegister ? vectorWritebackRegisters : vectorWritebackMem;
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

    public int getVectorLength(){return this.vectorLength;}

    public int getHalt(){return this.linkCode;}

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
        int instr = Integer.parseInt(binaryValue, 2);

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

                this.strValue = opMap.get(opCode) + (opCode == 12 ? "" : " R" + r_d) + " R" + r_1 + " R" + r_2;
                break;
            case 1:// Data Processing Register Indirect(both) with 3 operands (rd = ValueAt(r1) + ValueAt(r2))
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

                this.strValue = opMap.get(opCode) + (opCode == 12 ? "" : " R" + r_d) + " ValAt(R" + r_1 + ") ValAt(R" + r_2+")";
                break;
            case 2://Data Processing Register Indirect(one) with 3 operands (rd = ValueAt(r1) + r2)
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

                this.strValue = opMap.get(opCode) + (opCode == 12 ? "" : " R" + r_d) + " ValAt(R" + r_1 + ") R" + r_2;
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

                this.strValue = opMap.get(opCode) + (opCode == 12 ? "" : " R" + r_d) + " R" + r_1 + " " + imm;
                break;
            case 4:// Data processing Indirect with 2 operands and an immediate (rd = ValueAt(r1) + 3)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                imm = (instr & 0b00000000000000000000011111111000) >> 3;
                condCode = (instr & 0b11110000000000000000000000000000) >> 28;

                params.add(r_d);
                params.add(r_1);
                params.add(imm);

                stallRegisters.add(r_d);
                dependsOnRegisters.add(r_1);

                this.strValue = opMap.get(opCode) + (opCode == 12 ? "" : " R" + r_d) + " ValAt(R" + r_1 + ") " + imm;
                break;
            case 5: // Load/Store (Load value from address into rd / store value in r1 at address in rd)
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
            case 6: // Load/Store immediate (Load 3 into register rd / Store 3 into the address in rd)
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
            case 7://Branch Instruction
                condCode = (instr & 0b11110000000000000000000000000000) >> 28; //Condition
                sign =     (instr & 0b00000000010000000000000000000000) >> 22;
                r_1 =      (instr & 0b00000000001111111111111111111111); //Offset/number of lines of code to jump
                linkCode = (instr & 0b00000000100000000000000000000000) >> 23;
                r_1 *= (sign == 1 ? -1 : 1);
                params.add(r_1);

                strValue = "BRANCH " + r_1 + " IF " + condMap.get(condCode);
                break;
            case 8: // Vector Load/Store Load vector from address into rd / store vector in rd into address)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr  & 0b0000000000000000111100000000000) >> 11;
                vectorLength = (instr  & 0b0000000000000000000011111000000) >> 6;

                params.add(r_d);
                params.add(r_1);

                if (opCode != 7) // Immediate if append
                    dependsOnRegisters.add(r_1 + 100);

                if (opCode == 13 || opCode == 7) {
                    stallRegisters.add(r_d + 100);
                } else if (opCode == 14) {
                    dependsOnRegisters.add(r_d + 100);
                }

                this.strValue = opMapV.get(opCode) + " V" + r_d + (opCode == 7 ? " " : " V") + r_1;
                break;
            case 9: // Vector Data Processing with 3 operands (Vd = V1 + V2)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                r_2 = (instr & 0b00000000000000000000011110000000) >> 7;
                condCode = (instr & 0b11110000000000000000000000000000) >> 28;
                vectorLength = (instr & 0b00000000000000000000000001111100) >> 2;

                params.add(r_d);
                params.add(r_1);
                params.add(r_2);

                stallRegisters.add(r_d + 100);
                dependsOnRegisters.add(r_1 + 100);
                dependsOnRegisters.add(r_2 + 100);

                this.strValue = opMapV.get(opCode) + (opCode == 12 ? "" : " V" + r_d) + " V" + r_1 + " V" + r_2;
                break;
            case 10: // Vector Data Processing with operand and immediate (rd = r1 + 3)
                r_d = (instr & 0b00000000000001111000000000000000) >> 15;
                r_1 = (instr & 0b00000000000000000111100000000000) >> 11;
                imm = (instr & 0b00000000000000000000011111100000) >> 5;
                condCode =     (instr & 0b11110000000000000000000000000000) >> 28;
                vectorLength = (instr & 0b00000000000000000000000000011111);

                params.add(r_d);
                params.add(r_1);
                params.add(imm);

                stallRegisters.add(r_d + 100);
                dependsOnRegisters.add(r_1 + 100);

                this.strValue = opMapV.get(opCode) + (opCode == 12 ? "" : " V" + r_d) + " V" + r_1 + " " + imm;
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

    private void buildOpMapV() {
        opMapV = new HashMap<>();
        opMapV.put(0, "ADD");
        opMapV.put(1, "SUBTRACT");
        opMapV.put(2, "MULTIPLY");
        opMapV.put(4, "DIVIDE");
        opMapV.put(13, "LOAD");
        opMapV.put(14, "STORE");
        opMapV.put(7, "APPEND");
    }

    private void buildCondMap() {
        condMap = new HashMap<>();
        condMap.put(0, "EQ");
        condMap.put(1, "NE");
        condMap.put(2, "GT");
        condMap.put(3, "GTE");
        condMap.put(4, "LT");
        condMap.put(5, "LTE");
    }

    public boolean checkCond(int cmp) {
        return ((cmp >> condCode) & 1) == 1;
    }

    public int getCondCode() {
        return condCode;
    }

    public class AddressValuePair {
        int address, value,opcode,typ;

        public AddressValuePair(int address, int value) {
            this.address = address;
            this.value = value;
            this.opcode = opCode;
            this.typ = type;
        }
    }

    public class AddressPair{
        int address_1,address_2,destination,opcode,typ;

        public AddressPair(int address_1,int address_2,int destination,int opcode, int type){
            this.address_1 = address_1;
            this.address_2 = address_2;
            this.destination = destination;
            this.opcode = opcode;
            this.typ = type;
        }
    }

    public class VectorValuePair{
        int address;
        int[] value;

        public VectorValuePair(int address, int[] value) {
            this.address = address;
            this.value = value;
        }
    }
}
