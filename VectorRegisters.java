import com.sun.deploy.util.StringUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class VectorRegisters {
    ArrayList<ArrayList<Integer>> vectorData;
    public ObservableList<ObservableList<SimpleIntegerProperty>> registerData;

    //Set up the UI for Vector Registers

    public VectorRegisters(int size, int vectorSize){
        vectorData = new ArrayList<>();
        registerData = FXCollections.observableList(new ArrayList<>());

        for (int i = 0; i < size;i++) {
            vectorData.add(new ArrayList<>());
            registerData.add(FXCollections.observableList(new ArrayList<>()));

            for(int j =0; j < vectorSize;j++) {
                vectorData.get(i).add(null);
                registerData.get(i).add(new SimpleIntegerProperty(-1));
            }

        }

    }

    public void set(int register, ArrayList<Integer> value){
        //Making a deep copy
        for(int i = 0;i < value.size(); i++) {
            vectorData.get(register).set(i, value.get(i)); // the remaining values are going to be zero, since sorting that out is the compiler's job
            registerData.get(register).get(i).set(value.get(i));
        }
    }

    //For individual load(since we don't have a vector immediate to populate the memory)
    public void append(int register, int value){
        ArrayList<Integer> vRegister = vectorData.get(register);
        int length = getLength(register);

        if (length == vRegister.size()){
            System.out.println("Vector size limit reached");
        } else{
            vRegister.set(length,value);
            registerData.get(register).get(length).set(value);
        }
    }

    private int getLength(int register) {
        ArrayList<Integer> vRegister = vectorData.get(register);

        for(int i = 0; i < vRegister.size(); i++)
            if(vRegister.get(i) == null)
                return i;

        return vRegister.size();
    }

    public void print(int register) {
        System.out.println("V" + register + ": [" + get(register).stream().map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]");
    }


    public ArrayList<Integer> get(int register){
        return vectorData.get(register);
    }
}
