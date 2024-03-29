import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

public class Cache extends Memory {

    private final Memory nextMemory;

    private final int[] tags;
    private final int[][] lru;
    private final boolean[] dirty;
    private final boolean[] valid;
    public ObservableList<LineData> lineData;

    //Cache lines need to be multiples of 4
    public Cache(int numLines, Memory nextMemory) {
        super(numLines, 4, 0);//Line length might change in the future
        this.nextMemory = nextMemory;

        tags = new int[numLines];
        lru = new int[4][numLines/4];
        dirty = new boolean[numLines];//dirty bit per line
        valid = new boolean[numLines];//line based valid bit is acceptable because the whole line is pushed out or pushed in the cache at the same time

        ArrayList<LineData> lineArrayList = new ArrayList<>();

        for (int i = 0; i < 4;i++){
            Arrays.fill(lru[i], -1);
        }

        for (int i = 0; i < numLines; i++) {
            tags[i] = -1;
            valid[i] = false;
            dirty[i] = false;

            lineArrayList.add(new LineData(-1, -1, -1, -1, -1, -1));
        }
        lineData = FXCollections.observableList(lineArrayList);

    }

    @Override
    public int read(String callingFrom, int address) {
        int offset = address % 4;
        int tag = address - offset; //getting start of the line
        int tagLoc = -1;

        //Finding the set of the address
        int set = (address/16) % 4; //Setting set to cycle of 16 words

        // Check if tag is in cache
        for (int i = set * 4; i < (set * 4) + lru[set].length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        if (tagLoc >= 0) { // Cache hit
            // Read word from cache in location of tag

            if (!valid[(tagLoc)]){
                //If valid bit is set to invlaid and tag exists then treat it as a miss
                System.out.println("Invalid Bit encountered");
                int[] line = nextMemory.getLine(callingFrom, address);

                if (line[0] == Memory.WAIT)
                    return Memory.WAIT;
                // address added for writeback in case of dirty bit found on 1
                writeToCache(tag, line, address, callingFrom, false);
                return line[offset];
            }

            if (lru[set][tagLoc%4] != 0) {
                for (int i = 0; i < lru[set].length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
                    if(lru[set][i] != -1) {
                        lru[set][i] = (tagLoc % 4 == i) ? 0 : ((tags[i + (set * 4)] == 0 && !valid[i + (set * 4)]) ? -1 : lru[set][i] + 1);
                    }
                    lineData.get(i+(set*4)).setLru(lru[set][i]);
                }
            }

            return super.read(callingFrom, tagLoc * 4 + offset);
        } else { // Cache miss
            // Read from next memory, wait if needed
            int[] line = nextMemory.getLine(callingFrom, address);

            if (line[0] == Memory.WAIT)
                return Memory.WAIT;
            // address added for writeback in case of dirty bit found on 1
            writeToCache(tag, line, address, callingFrom, false);
            return line[offset];
        }
    }

    //for debugging
    public int instrRead(int PC,String name){
        return nextMemory.instrRead(PC,name);
    }

    public void writeToCache(int tag, int[] line, int address, String callingFrom, boolean isDirty) {
        int nextLoc = -1;

        //Finding the set of the address
        int set = (address / 16)%4; //Setting set to cycle of 16 words

        //Setting maxLRULoc to the start of the set
        int maxLRULoc = set*4;

        // Look for empty spot and find least recently used full spot
        for (int i = set*4; i < (set*4)+lru[set].length; i++)
            if (tags[i] == -1 && !valid[i]) {
                nextLoc = i;
                break;
            } else if (lru[set][i%4] > lru[set][maxLRULoc%4])
                maxLRULoc = i;


        if (nextLoc == -1) // Cache is full, take LRU
            nextLoc = maxLRULoc;

        // Checking the dirty bits and writing back to New Memory if the bit is true
        if (dirty[nextLoc]){
            int[] oldLine = super.getLine(callingFrom, nextLoc * 4);
            int out = Memory.WAIT;

            System.out.println("Trying to writeback line to memory at address " + (tags[nextLoc]));
            while (out == Memory.WAIT) {
                out = nextMemory.writeLine(callingFrom, tags[nextLoc] / 4, oldLine);
                System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
            }
        }

        // Set tag and write line to cache in chosen location
        tags[nextLoc] = tag;
        valid[nextLoc] = true; // Setting the new cacheline as valid

        writeLine(callingFrom, nextLoc, line);
        lineData.set(nextLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));
        lineData.get(nextLoc).v.set(1);

        //Setting the dbits on the cache line, this works because we will just write a whole line when writing a single word in the cache
        dirty[nextLoc] = isDirty;
        lineData.get(nextLoc).setDirty(isDirty ? 1 : 0);

        for (int i = 0; i < lru[set].length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
            if(lru[set][i] != -1) {
                lru[set][i] = (nextLoc % 4 == i) ? 0 : ((tags[i + (set * 4)] == 0 && !valid[i + (set * 4)]) ? -1 : lru[set][i] + 1);
            }
            if( i == nextLoc%4){
                lru[set][i] = 0;
            }
            lineData.get(i+(set*4)).setLru(lru[set][i]);
        }

    }

    //Direct Write to cache
    public void directWrite(int tag, int[] line, int address, String callingFrom, boolean isDirty){
        int tagLoc = -1;

        //Finding the set of the address
        int set = (address / 16)%4; //Setting set to cycle of 16 words

        // Check if tag is in cache
        for (int i = set * 4; i < (set * 4) + lru[set].length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        if (tagLoc >= 0) {
            lineData.set(tagLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));

            // Update LRU (0 for tagLoc, +1 for everything else smaller than tagLoc(original val))
            int prevLocVal = lru[set][tagLoc % 4];
            for (int i = 0; i < lru[set].length; i++) {
                if (i == tagLoc % lru[set].length) {
                    lru[set][i] = 0;
                } else if (lru[set][i] < prevLocVal) {
                    lru[set][i] += 1;
                }

                lineData.get(i+(set*4)).setLru(lru[set][i]);
            }

            //Checking the dirty bits and writing back to Next Memory if the bit is true
            if (dirty[tagLoc]) {
                int[] oldLine = super.getLine("", tagLoc * 4);
                int out = Memory.WAIT;
                System.out.println("Trying to writeback line to memory at address " + (tag));
                while (out == Memory.WAIT) {
                    out = nextMemory.writeLine(callingFrom, (tag / 4), oldLine);
                    System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                }
            }

            writeLine(callingFrom, tagLoc, line);
            tags[tagLoc] = tag;

            valid[tagLoc] = true; // Setting the new cacheline as valid
            lineData.get(tagLoc).v.set(1);

            //Setting the dbits on the cache line, this works because we will just write a whole line when writing a single word in the cache
            dirty[tagLoc] = isDirty;
            lineData.get(tagLoc).setDirty(isDirty ? 1 : 0);
        } else {
            writeToCache(tag, line, address, callingFrom, isDirty);
        }
    }

    @Override
    public int write(String callingFrom, int address, int val) {
        int tagLoc = -1;
        int tag = address - address % 4;

        //Finding the set of the address
        int set = (address / 16) % 4; //Setting set to cycle of 16 words

        // Check if tag is in cache
        for (int i = set * 4; i < (set * 4) + lru[set].length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        //Write to cache after ejecting dirty bit
        if(tagLoc >= 0) {
            //Checking the dirty bits and writing back to Next Memory if the bit is true
            if (dirty[tagLoc]) {
                int[] oldLine = super.getLine("", tagLoc * 4);
                int out = Memory.WAIT;
                System.out.println("Trying to writeback line to memory at address " + (tag));
                while (out == Memory.WAIT) {
                    out = nextMemory.writeLine(callingFrom, (tag / 4), oldLine);
                    System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                }
            }

            this.writeSingleValueInCache(tagLoc, address % 4, val);
            dirty[tagLoc] = true;
            //for ui
            int[] line = getCacheLine(tagLoc);
            lineData.set(tagLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));
            lineData.get(tagLoc).v.set(1);

            return 1;
        } else {
            return nextMemory.write("Cache", address, val);
        }
    }

    @Override
    public int[] getLine(String callingFrom, int address){
        int offset = address % 4;
        int tag = address - offset; //getting start of the line
        int tagLoc = -1;
        //Finding the set of the address
        int set = (address / 16) % 4; //Setting set to cycle of 16 words

        // Check if tag is in cache
        for (int i = set * 4; i < (set * 4) + lru[set].length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        // If tag is in cache
        if(tagLoc >= 0){
            if (lru[set][tagLoc%4] != 0) {
                for (int i = 0; i < lru[set].length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
                    if(lru[set][i] != -1) {
                        lru[set][i] = (tagLoc % 4 == i) ? 0 : ((tags[i + (set * 4)] == 0 && !valid[i + (set * 4)]) ? -1 : lru[set][i] + 1);
                    }
                    lineData.get(i+(set*4)).setLru(lru[set][i]);
                }
            }
            //Only returning the requested amount of words
            lru[set][3] =1;
            int[] cacheLine =  this.getCacheLine(tagLoc);
            int[] returnLine = new int[4];
            for(int i = 0; i < 4; i++){
                returnLine[i] = cacheLine[i];
            }
            return returnLine;
        }
        else{
            int nextLoc = -1;

            //Finding the set of the address
            set = (address / 16)%4; //Setting set to cycle of 16 words

            //Setting maxLRULoc to the start of the set
            int maxLRULoc = set*4;

            // Look for empty spot and find least recently used full spot
            for (int i = set*4; i < (set*4)+lru[set].length; i++)
                if (tags[i] == -1 && !valid[i]) {
                    nextLoc = i;
                    break;
                } else if (lru[set][i%4] > lru[set][maxLRULoc%4])
                    maxLRULoc = i;

            lru[set][3] =1;
            if (nextLoc == -1) // Cache is full, take LRU
                nextLoc = maxLRULoc;
            int[] line = nextMemory.getLine(callingFrom, address);

            if (line[0] == Memory.WAIT)
                return new int[]{Memory.WAIT};
            // address added for writeback in case of dirty bit found on 1
            super.writeLine("Cache",nextLoc,line);
            lineData.set(nextLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));
            lineData.get(nextLoc).v.set(1);
            return line;
        }
    }

    @Override
    public int writeLinePartial(String callingFrom, int address, int[] line,int size) {
        int offset = address % 4;
        int tag = address - offset; //getting start of the line
        int tagLoc = -1;

        //Finding the set of the address
        int set = (address / 16) % 4; //Setting set to cycle of 16 words

        // Check if tag is in cache
        for (int i = set * 4; i < (set * 4) + lru[set].length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        //If tag is in the cache
        if (tagLoc >= 0) {
            //If the dirty bit is set to true writeback to memory
            if (dirty[tagLoc]) {
                int[] oldLine = super.getLine("", tagLoc * 4);
                int out = Memory.WAIT;
                System.out.println("Trying to writeback line to memory at address " + (tag));
                while (out == Memory.WAIT) {
                    out = nextMemory.writeLine(callingFrom, (tag / 4), oldLine);
                    System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
                }
            }

            lru[set][3] =1;
            super.writeLinePartial(callingFrom,address,line,size);
            dirty[tagLoc] = true;
            //for demo
            int[] cLine = getCacheLine(tagLoc);

            lineData.set(tagLoc, new LineData(0, tag, cLine[0], cLine[1], cLine[2], cLine[3]));
            lineData.get(tagLoc).v.set(1);

            return 1;
        }else{
            int check = nextMemory.writeLinePartial("Cache",address,line,size);
            if (check == Memory.WAIT)
                return Memory.WAIT;
            // address added for writeback in case of dirty bit found on 1
            return check;
        }
    }

    // Holds cache line data to display in table
    public class LineData {
        public SimpleIntegerProperty lru, tag, v, word1, word2, word3, word4, dirty;

        public LineData(int lru, int tag, int word1, int word2, int word3, int word4) {
            this.lru = new SimpleIntegerProperty(lru);
            this.tag = new SimpleIntegerProperty(tag);
            this.word1 = new SimpleIntegerProperty(word1);
            this.word2 = new SimpleIntegerProperty(word2);
            this.word3 = new SimpleIntegerProperty(word3);
            this.word4 = new SimpleIntegerProperty(word4);

            this.v = new SimpleIntegerProperty(0);
            this.dirty = new SimpleIntegerProperty(0);
        }

        public void setDirty(int dirty) {
            this.dirty.set(dirty);
        }

        public void setLru(int lru) {
            this.lru.set(lru);
        }

        public int getLru() {
            return lru.get();
        }

        public int getTag() {
            return tag.get();
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

        public int getV() {
            return v.get();
        }

        public int getDirty() {
            return dirty.get();
        }
    }
}
