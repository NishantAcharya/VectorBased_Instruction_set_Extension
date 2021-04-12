import com.sun.deploy.util.StringUtils;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class VectorRegisters {
    ArrayList<ArrayList<Integer>> vectorData;
    //Set up the UI for Vector Registers

    public VectorRegisters(int size, int vectorSize){
        vectorData = new ArrayList<ArrayList<Integer>>();
        for(int i = 0; i < size;i++){
            vectorData.add(new ArrayList<Integer>());
            for(int j =0; j < vectorSize;j++){
                vectorData.get(i).add(null);
            }
        }
        //Need to set up observable list of arrays for the UI
    }

    public void set(int register, ArrayList<Integer> value){
        //Making a deep copy
        for(int i = 0;i < value.size(); i++){
            vectorData.get(register).set(i,value.get(i)); // the remaining values are going to be zero, since sorting that out is the compiler's job
        }
    }

    //For individual load(since we don't have a vector immediate to populate the memory)
    public void loadSet(int register, int value){
        ArrayList<Integer> vRegister = vectorData.get(register);
        int i;
        for( i = 0; i < vRegister.size();i++){
            if(vRegister.get(i) == null){
                break;
            }
        }
        if(i == vRegister.size()){
            System.out.println("Vector size limit reached");
        }
        else{
            vRegister.set(i,value);
        }
    }

    public void print(int register) {
        System.out.println("V" + register + ": [" + get(register).stream().map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]");
    }


    public ArrayList<Integer> get(int register){
        return vectorData.get(register);
    }
}
