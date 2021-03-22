import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class Registers {

    private int[] data;
    public ObservableList<SimpleIntegerProperty> registerData;

    public Registers(int size) {
        data = new int[size];

        // Setup observable list for table
        ArrayList<SimpleIntegerProperty> dataList = new ArrayList<>();
        for (int i = 0; i < 16; i++)
            dataList.add(new SimpleIntegerProperty(0));
        registerData = FXCollections.observableList(dataList);
    }

    // Can access registers 0 -> 13, 14/15 are special registers
    // 14 is Linking register
    // 15 is Program Counter
    public void set(int register, int value) {
        data[register] = value;
        registerData.get(register).set(value);
    }

    public int get(int register) {
       return data[register];
    }
}
