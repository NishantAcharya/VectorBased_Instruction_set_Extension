//import com.sun.deploy.util.StringUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class VectorRegisters {
    ArrayList<ArrayList<Integer>> vectorData;
    public ObservableList<VRData> vrData;

    //Set up the UI for Vector Registers

    public VectorRegisters(int size, int vectorSize){
        vectorData = new ArrayList<>();
        ArrayList<VRData> uiData = new ArrayList<>();

        for (int i = 0; i < size;i++) {
            vectorData.add(new ArrayList<>());
            uiData.add(new VRData("V" + i));

            for(int j =0; j < vectorSize;j++) {
                vectorData.get(i).add(null);
            }
        }

        vrData = FXCollections.observableList(uiData);
    }

    public void set(int register, ArrayList<Integer> value){
        //Making a deep copy
        for(int i = 0;i < value.size(); i++) {
            vectorData.get(register).set(i, value.get(i)); // the remaining values are going to be zero, since sorting that out is the compiler's job
            vrData.get(register).set(i, value.get(i));
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
            vrData.get(register).set(length, value);
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

    public class VRData {
        // Each value in a single register
        public SimpleStringProperty label, r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15;
        private SimpleStringProperty[] regs;

        public VRData(String label) {
            this.label = new SimpleStringProperty(label);
            r0 = new SimpleStringProperty("");
            r1 = new SimpleStringProperty("");
            r2 = new SimpleStringProperty("");
            r3 = new SimpleStringProperty("");
            r4 = new SimpleStringProperty("");
            r5 = new SimpleStringProperty("");
            r6 = new SimpleStringProperty("");
            r7 = new SimpleStringProperty("");
            r8 = new SimpleStringProperty("");
            r9 = new SimpleStringProperty("");
            r10 = new SimpleStringProperty("");
            r11 = new SimpleStringProperty("");
            r12 = new SimpleStringProperty("");
            r13 = new SimpleStringProperty("");
            r14 = new SimpleStringProperty("");
            r15 = new SimpleStringProperty("");

            regs = new SimpleStringProperty[] { r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15 };
        }

        private void set(int i, int val) {
            regs[i].set(val + "");
        }

        public SimpleStringProperty r0Property() {
            return r0;
        }
        public SimpleStringProperty r1Property() {
            return r1;
        }
        public SimpleStringProperty r2Property() {
            return r2;
        }
        public SimpleStringProperty r3Property() {
            return r3;
        }
        public SimpleStringProperty r4Property() {
            return r4;
        }
        public SimpleStringProperty r5Property() {
            return r5;
        }
        public SimpleStringProperty r6Property() {
            return r6;
        }
        public SimpleStringProperty r7Property() {
            return r7;
        }
        public SimpleStringProperty r8Property() {
            return r8;
        }
        public SimpleStringProperty r9Property() {
            return r9;
        }
        public SimpleStringProperty r10Property() {
            return r10;
        }
        public SimpleStringProperty r11Property() {
            return r11;
        }
        public SimpleStringProperty r12Property() {
            return r12;
        }
        public SimpleStringProperty r13Property() {
            return r13;
        }
        public SimpleStringProperty r14Property() {
            return r14;
        }
        public SimpleStringProperty r15Property() {
            return r15;
        }
        public String getLabel() {
            return label.get();
        }

    }
}
