import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Memory {

    public ObservableList<LineData> lineData;

    public static int WAIT = -93902354;
    private int lineLength = 4;
    private int size;
    private int[][] data; // Data is a 2d array of lines, each with LINE_LENGTH words
    private int delay = 3;

    private String currentlyWaiting = "";
    private int currWait = 0;

    public Memory(int numLines, int lineLength) {
        this.size = numLines * lineLength;
        data = new int[numLines][lineLength];

        ArrayList<LineData> lineList = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = 0;
            }

            lineList.add(new LineData(i * 4));
        }

        lineData = FXCollections.observableList(lineList);
    }

    public Memory(int numLines, int lineLength, int delay) {
        this.lineLength = lineLength;
        this.delay = delay;
        this.size = numLines * lineLength;
        data = new int[numLines][lineLength];
        ArrayList<LineData> lineList = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                data[i][j] = 0;
            }

            lineList.add(new LineData(i * 4));
        }

        lineData = FXCollections.observableList(lineList);

    }

    private boolean needsToWait(String callingFrom, int address) {
        String waitTag = callingFrom + address;

        if (currentlyWaiting.isEmpty()) {
            currWait = delay;
            currentlyWaiting = waitTag;
        }

        if (currentlyWaiting.equals(waitTag)) {
            if (currWait == 0) {
                currentlyWaiting = "";
                return false;
            }

            currWait--;
        }

        return true;

    }

    public int read(String callingFrom, int address) {
        if (needsToWait(callingFrom, address)) {
            delay();
            return Memory.WAIT;
        }

        address = address % size; // Wrap around if needed
        int offset = address % lineLength;
        int lineNum = (address - offset)  / lineLength;

        return data[lineNum][offset];
    }

    public int[] getLine(String callingFrom, int address) {
        if (needsToWait(callingFrom, address)) {
            delay();
            return new int[]{Memory.WAIT};
        }

        address = address % size; // Wrap around if needed
        int offset = address % lineLength;
        int lineNum = (address - offset)  / lineLength;

        return data[lineNum];
    }

    public int write(String callingFrom, int address, int value) {
        if (needsToWait(callingFrom, address)) {
            delay();
            return Memory.WAIT;
        }

        address = address % size; // Wrap around if needed
        int offset = address % 4;
        int lineNum = (address - offset)  / lineLength;

        data[lineNum][offset] = value;
        lineData.get(lineNum).write(offset, value);

        return 1;
    }

    public int writeLine(String callingFrom, int lineNum, int[] line) {
        if (needsToWait(callingFrom, lineNum)) {
            delay();
            return Memory.WAIT;
        }

        data[lineNum] = line;
        lineData.get(lineNum).writeLine(line);

        return 1;
    }

    public int writeLinePartial(String callingFrom,int lineNum,int[] line, int eleNum){
        if (needsToWait(callingFrom, lineNum))
            return Memory.WAIT;
        for(int i = 0; i < eleNum;i++){
            data[lineNum][i] = line[i];
        }
        return 1;
    }

    public void writeSingleValueInCache(int tag,int offset,int value) {
        data[tag][offset] = value;
        lineData.get(tag).write(offset, value);
    }

    //Method of direct access during write back since the cache line data is known
    public int readCache(String callingFrom, int tag , int offset) {


        return data[tag][offset];
    }

    public void writeCache(String callingFrom, int tag,int offset,int value) {
         data[tag][offset] = value;
    }

    public int[] getCacheLine(int tag) {
        return data[tag];
    }

    public void printData() {
        for (int i = 0; i < data.length; i++) {
            int[] row = data[i];
            System.out.println((i * 4) +  ": " + Arrays.toString(row));
        }
    }

    public void printData(int fromAddr, int toAddr) {
        if (fromAddr == -1) fromAddr = 0;
        if (toAddr == -1) toAddr = data.length * 4;

        for (int i = fromAddr / 4; i < toAddr / 4 + 1; i++) {
            int[] row = data[i];
            System.out.println((i * 4) +  ": " + Arrays.toString(row));
        }
    }

    public int getSize(){
        return size;
    }

    private void delay() {
        try {
            Thread.sleep(3);
        } catch (Exception e) {}
    }

    // Holds cache line data to display in table
    public class LineData {
        public SimpleIntegerProperty lineAddr, word1, word2, word3, word4;

        public LineData(int lineAddr) {
            this.lineAddr = new SimpleIntegerProperty(lineAddr);
            this.word1 = new SimpleIntegerProperty(0);
            this.word2 = new SimpleIntegerProperty(0);
            this.word3 = new SimpleIntegerProperty(0);
            this.word4 = new SimpleIntegerProperty(0);
        }

        public void writeLine(int[] line) {
            for (int i = 0; i < line.length; i++) {
                write(i, line[i]);
            }
        }

        public void write(int offset, int value) {
            switch (offset) {
                case 0:
                    word1 = new SimpleIntegerProperty(value);
                    break;
                case 1:
                    word2 = new SimpleIntegerProperty(value);
                    break;
                case 2:
                    word3 = new SimpleIntegerProperty(value);
                    break;
                case 3:
                    word4 = new SimpleIntegerProperty(value);
                    break;
            }
        }

        public int getLineAddr() {
            return lineAddr.get();
        }

        public int getWord1() {
            return word1.get();
        }

        public int getWord2() {
            return word2.get();
        }

        public int getWord3() {
            return word3.get();
        }

        public int getWord4() {
            return word4.get();
        }

        public SimpleIntegerProperty word1Property() {
            return word1;
        }

        public SimpleIntegerProperty word2Property() {
            return word2;
        }

        public SimpleIntegerProperty word3Property() {
            return word3;
        }

        public SimpleIntegerProperty word4Property() {
            return word4;
        }
    }
}