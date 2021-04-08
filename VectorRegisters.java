public class VectorRegisters {
    int[][]data;
    //Set up the UI for Vector Registers

    public VectorRegisters(int size, int vectorSize){
        data = new int[size][vectorSize];

        //Need to set up observale list of arrays for the UI
    }

    public void set(int register, int[] value){
        //Making a deep copy
        for(int i = 0;i < value.length; i++){
            data[register][i] = value[i]; // the remaining values are going to be zero, since sorting that out is the compiler's job
        }
    }

    public int[] get(int register){
        return data[register];
    }
}
