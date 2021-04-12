import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class Assembler {

    private static HashMap<String, Integer> opMap, opMapV, condMap;

    public static int toBinary(String line) {
        System.out.println(line);
        setupHashMaps();

        String[] tokens = line.toUpperCase().split(" ");

        boolean isVectorInst = isVectorInstruction(tokens);
        return  isVectorInst ? getVectorInstrBinary(tokens) : getInstrBinary(tokens);
    }

    private static int getInstrBinary(String[] tokens) {
        int op = opMap.get(tokens[0]);
        int type = getType(op, tokens);

        int binary = 0;
        binary = binary | (type << 24);

        if (op == 7) { // Branching instructions
            if (tokens.length > 2) {
                // Insert condition code
                int cond = condMap.get(tokens[3]);
                binary = binary | (cond << 28);
            }

            // Insert sign bit
            int s_bit = tokens[1].startsWith("-") ? 1 : 0;
            binary = binary | (s_bit << 22);

            // Insert branch offset
            binary = binary | Integer.parseInt(tokens[1].replace("-", ""));

            return binary;
        }

        // Insert op code
        binary = binary | (op << 20);

        int op1 = tokens.length < 2 ? -1 : Integer.parseInt(tokens[1].replaceAll("[^\\d.]", ""));
        int op2 = tokens.length < 3 ? -1 : Integer.parseInt(tokens[2].replaceAll("[^\\d.]", ""));
        int op3 = tokens.length < 4 ? -1 : Integer.parseInt(tokens[3].replaceAll("[^\\d.]", ""));

        if (op == 12) { // Compare
            if (type == 0) {
                binary = binary | (op1 << 11);
                binary = binary | (op2 << 7);
            } else if (type == 3) {
                binary = binary | (op1 << 11);
                binary = binary | (op2 << 3);
            }
        } else {
            if (type == 0) {
                binary = binary | (op1 << 15);
                binary = binary | (op2 << 11);
                binary = binary | (op3 << 7);
            } else if (type == 3) {
                binary = binary | (op1 << 15);
                binary = binary | (op2 << 11);
                binary = binary | (op3 << 3);
            } else if (type == 5) {
                binary = binary | (op1 << 15);
                binary = binary | (op2 << 11);
            } else if (type == 6) {
                binary = binary | (op1 << 15);
                binary = binary | (op2 << 3);
            }
        }

        return binary;
    }

    private static int getVectorInstrBinary(String[] tokens) {
        int op = opMapV.get(tokens[0]);
        int type = getVectorType(op, tokens);

        int binary = 0;
        binary = binary | (type << 24);
        binary = binary | (op << 20);

        int op1 = tokens.length < 2 ? -1 : Integer.parseInt(tokens[1].replaceAll("[^\\d.]", ""));
        int op2 = tokens.length < 3 ? -1 : Integer.parseInt(tokens[2].replaceAll("[^\\d.]", ""));
        int op3 = tokens.length < 4 ? -1 : Integer.parseInt(tokens[3].replaceAll("[^\\d.]", ""));
        int op4 = tokens.length < 5 ? -1 : Integer.parseInt(tokens[4].replaceAll("[^\\d.]", ""));

        if (type == 8) {
            binary = binary | (op1 << 15);
            binary = binary | (op2 << 11);

            if (op3 != -1)
                binary = binary | (op3 << 6);
        } else if (type == 9) {
            binary = binary | (op1 << 15);
            binary = binary | (op2 << 11);
            binary = binary | (op3 << 7);

            if (op4 != -1)
                binary = binary | (op4 << 2);
        } else if (type == 10) {
            binary = binary | (op1 << 15);
            binary = binary | (op2 << 11);
            binary = binary | (op3 << 5);

            if (op4 != -1)
                binary = binary | op4;
        }

        return binary;
    }

    private static int getType(int op, String[] tokens) {
        if (op == 7) // Branch instruction
            return 7;
        else if (op == 13 || op == 14) // Load/Store instruction
            return tokens[2].startsWith("R") ? 5 : 6;
        else { // Data processing instruction
            if (op == 12) {
                return tokens[2].startsWith("R") ? 0 : 3;
            }

            return tokens[3].startsWith("R") ? 0 : 3;
        }
    }

    private static int getVectorType(int op, String[] tokens) {
        if (op == 13 || op == 14 || op == 7)
            return 8;
        else
            return tokens[3].startsWith("V") ? 9 : 10;
    }

    private static void setupHashMaps() {
        opMap = new HashMap<>();
        opMap.put("ADD", 0);
        opMap.put("SUBTRACT", 1);
        opMap.put("MULTIPLY", 2);
        opMap.put("DIVIDE", 4);
        opMap.put("MODULO", 8);
        opMap.put("AND", 3);
        opMap.put("OR", 5);
        opMap.put("NOT", 9);
        opMap.put("XOR", 10);
        opMap.put("COMPARE", 12);
        opMap.put("SET FLAG", 6);
        opMap.put("SHIFT", 7);
        opMap.put("SWAP", 11);
        opMap.put("LOAD", 13);
        opMap.put("STORE", 14);
        opMap.put("BRANCH", 7);

        opMapV = new HashMap<>();
        opMapV.put("ADD", 0);
        opMapV.put("SUBTRACT", 1);
        opMapV.put("MULTIPLY", 2);
        opMapV.put("DIVIDE", 4);
        opMapV.put("LOAD", 13);
        opMapV.put("STORE", 14);
        opMapV.put("APPEND", 7);

        condMap = new HashMap<>();
        condMap.put("EQ", 0);
        condMap.put("NE", 1);
        condMap.put("GT", 2);
        condMap.put("GTE", 3);
        condMap.put("LT", 4);
        condMap.put("LTE", 5);
    }

    private static boolean isVectorInstruction(String[] tokens) {
        for (int i = 1; i < tokens.length; i++)
            if (tokens[i].startsWith("V") && !tokens[i].startsWith("VALAT"))
                return true;

        return false;
    }

    public static void main(String[] args) {
        System.out.println(toBinary("LOAD R0 0"));
        System.out.println(toBinary("ADD R0 R0 1"));
        System.out.println(toBinary("COMPARE R0 5"));
        System.out.println(toBinary("BRANCH -2 IF LT"));
        System.out.println(toBinary("STORE R1 R0"));
    }

}
