import java.util.Arrays;

public class Instruction {

    private String strValue;
    private Type type;
    private String[] params;

    public Instruction(String str) {
        this.strValue = str;
        String[] values = str.split(" ");
        this.type = Type.valueOf(values[0]);
        this.params = Arrays.copyOfRange(values, 1, values.length);
    }

    public enum Type {
        ADD, LOAD, STORE;
    }

    @Override
    public String toString() {
        return strValue;
    }
}
