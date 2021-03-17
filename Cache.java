import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Observable;

public class Cache extends Memory {

    private Memory nextMemory;

    private int[] tags;
    private int[] lru;
    private boolean[] dirty;
    private boolean[] valid;
    public ObservableList<LineData> lineData;

    public Cache(int numLines, Memory nextMemory) {
        super(numLines, 4, 0);//Line length might change in the future
        this.nextMemory = nextMemory;

        tags = new int[numLines];
        lru = new int[numLines];
        dirty = new boolean[numLines];//dirty bit per line
        valid = new boolean[numLines];//line based valid bit is acceptable because the whole line is pushed out or pushed in the cache at the same time


        ArrayList<LineData> lineArrayList = new ArrayList<>();

        for (int i = 0; i < numLines; i++) {
            tags[i] = -1;
            lru[i] = -1;
            valid[i] = false;
            dirty[i] = false;

            lineArrayList.add(new LineData(-1, -1, -1, -1, -1, -1));
        }

        lineData = FXCollections.observableList(lineArrayList);
    }

    @Override
    public int read(String callingFrom, int address) {
        int offset = address % 4;
        int tag = address - offset;//getting start of the line
        int tagLoc = -1;

        // Check if tag is in cache
        for (int i = 0; i < tags.length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }


        if (tagLoc >= 0) { // Cache hit
            // Read word from cache in location of tag

            if (!valid[tagLoc]){
                return 404; //Return 404 if the reading an invalid bit
            }

            if (lru[tagLoc] != 0) {
                for (int i = 0; i < lru.length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
                    lru[i] = (tagLoc == i) ? 0 : ((tags[i] == -1) ? -1 : lru[i] + 1);
                    lineData.get(i).setLru(lru[i]);
                }
            }

            return super.read(callingFrom, tagLoc + offset);
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

    public void writeToCache(int tag, int[] line, int address, String callingFrom, boolean isDirty) {
        int nextLoc = -1;
        int maxLRULoc = 0;

        // Look for empty spot and find least recently used full spot
        for (int i = 0; i < tags.length; i++)
            if (tags[i] == -1) {
                nextLoc = i;
                break;
            } else if (lru[i] > lru[maxLRULoc])
                maxLRULoc = i;

        if (nextLoc == -1) // Cache is full, take LRU
            nextLoc = maxLRULoc;

        // Set tag and write line to cache in chosen location
        tags[nextLoc] = tag;
        valid[nextLoc] = true; // Setting the new cacheline as valid

        // Checking the dirty bits and writing back to New Memory if the bit is true
        if (dirty[nextLoc]){
            int[] oldLine = super.getLine(callingFrom, nextLoc);
            int out = Memory.WAIT;

            System.out.println("Trying to writeback line to memory at address " + (tag));
            while (out == Memory.WAIT) {
                out = nextMemory.writeLine(callingFrom, address, oldLine);
                System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
            }
        }

        writeLine(callingFrom, nextLoc, line);
        lineData.set(nextLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));
        lineData.get(nextLoc).v.set(1);

        //Setting the dbits on the cache line, this works because we will just write a whole line when writing a single word in the cache
        dirty[nextLoc] = isDirty;
        lineData.get(nextLoc).setDirty(isDirty ? 1 : 0);

        for (int i = 0; i < lru.length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
            lru[i] = (nextLoc == i) ? 0 : ((tags[i] == -1) ? -1 : lru[i] + 1);
            lineData.get(i).setLru(lru[i]);
        }

    }

    //Direct Write to cache
    public void directWrite(int tag, int[] line, int address, String callingFrom, boolean isDirty){
        int tagLoc = -1;

        // Check if tag is in cache
        for (int i = 0; i < tags.length; i++) {
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }
        }

        if(tagLoc >= 0) {
            lineData.set(tagLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));

            // Update LRU (0 for tagLoc, +1 for everything else smaller than tagLoc(original val))
            int prevLocVal = lru[tagLoc];
            for (int i = 0; i < lru.length; i++) {
                if(i == tagLoc){
                    lru[i] = 0;
                } else if(lru[i] < prevLocVal){
                    lru[i] += 1;
                }

                lineData.get(i).setLru(lru[i]);
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
