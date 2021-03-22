public class Registers {

    private int[] data;

    public Registers(int size) {
        data = new int[size];
    }

    // Can access registers 0 -> 13, 14/15 are special registers
    // 14 is Linking register
    // 15 is Program Counter
    public void set(int register, int value) {
        data[register] = value;
    }

    public int get(int register) {
       return data[register];
    }
}
