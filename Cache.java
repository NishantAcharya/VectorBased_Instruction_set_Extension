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
        super(numLines, 4, 0);
        this.nextMemory = nextMemory;

        tags = new int[numLines];
        lru = new int[numLines];
        dirty = new boolean[numLines];
        valid = new boolean[numLines];


        ArrayList<LineData> lineArrayList = new ArrayList<>();

        for (int i = 0; i < numLines; i++) {
            tags[i] = -1;
            lru[i] = -1;
            dirty[i] = false; //Initializing the dirty and the valid bit to false
            valid[i] = false;

            lineArrayList.add(new LineData(-1, -1, -1, -1, -1, -1));
        }


        lineData = FXCollections.observableList(lineArrayList);
    }
    @Override
    public int read(String callingFrom, int address) {
        int offset = address % 4;
        int tag = address - offset;
        int tagLoc = -1;

        // Check if tag is in cache
        for (int i = 0; i < tags.length; i++)
            if (tags[i] == tag) {
                tagLoc = i;
                break;
            }

        if(!valid[tagLoc]){
            return 404; //Return 404 if the reading an invalid bit
        }

        if (tagLoc >= 0) { // Cache hit
            // Read word from cache in location of tag
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
            //Add with dirty bit set to zero
            writeToCache(tag, line, 0);
            return line[offset];
        }
    }

    public void writeToCache(int tag, int[] line, int dBit) {
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
        //TODO: Check if the dirty bit is 1, if it is then write back to memory
        tags[nextLoc] = tag;
        valid[nextLoc] = true; // Setting the new cacheline as valid
        writeLine("", nextLoc, line);


        lineData.set(nextLoc, new LineData(0, tag, line[0], line[1], line[2], line[3]));

        for (int i = 0; i < lru.length; i++) { // Update LRU (0 for nextLoc, +1 for everything else)
            lru[i] = (nextLoc == i) ? 0 : ((tags[i] == -1) ? -1 : lru[i] + 1);
            lineData.get(i).setLru(lru[i]);
        }

    }
    

    // Holds cache line data to display in table
    public class LineData {
        public SimpleIntegerProperty lru, tag, word1, word2, word3, word4;

        public LineData(int lru, int tag, int word1, int word2, int word3, int word4) {
            this.lru = new SimpleIntegerProperty(lru);
            this.tag = new SimpleIntegerProperty(tag);
            this.word1 = new SimpleIntegerProperty(word1);
            this.word2 = new SimpleIntegerProperty(word2);
            this.word3 = new SimpleIntegerProperty(word3);
            this.word4 = new SimpleIntegerProperty(word4);
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
    }
}
