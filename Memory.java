import java.util.Arrays;
import java.util.Random;

public class Memory {

    public static int WAIT = -93902354;
    private int lineLength = 4;
    private int size;
    private int[][] data; // Data is a 2d array of lines, each with LINE_LENGTH words
    private int delay = 3;

    private String currentlyWaiting = "";
    private int currWait = 0;
// Just a reminder, Numlines for the demo need to be 16 and numline*linelength < 2^32(address) for RAM
    public Memory(int numLines, int lineLength) {
        this.size = numLines * lineLength;
        data = new int[numLines][lineLength];

        /*Memory needs to be set to 0 in each block and the chache needs to be clean*/
        //REMOVE LATER JUST FOR DEMO TO HAVE RANDOM DATA IN DATA
        Random rand = new Random();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                //data[i][j] = rand.nextInt(10);
                data[i][j] = 0;
            }
        }
    }

    public Memory(int numLines, int lineLength, int delay) {
        this.lineLength = lineLength;
        this.delay = delay;
        this.size = numLines * lineLength;
        data = new int[numLines][lineLength];
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
        if (needsToWait(callingFrom, address))
            return Memory.WAIT;

        address = address % size; // Wrap around if needed
        int offset = address % lineLength;
        int lineNum = (address - offset)  / lineLength;

        return data[lineNum][offset];
    }

    public int[] getLine(String callingFrom, int address) {
        if (needsToWait(callingFrom, address))
            return new int[] { Memory.WAIT };

        address = address % size; // Wrap around if needed
        int offset = address % lineLength;
        int lineNum = (address - offset)  / lineLength;

        return data[lineNum];
    }

    public int write(String callingFrom, int address, int value) {
        if (needsToWait(callingFrom, address))
            return Memory.WAIT;

        address = address % size; // Wrap around if needed
        int offset = address % 4;
        int lineNum = (address - offset)  / lineLength;

        data[lineNum][offset] = value;

        return 1;
    }

    public int writeLine(String callingFrom, int lineNum, int[] line) {
        if (needsToWait(callingFrom, lineNum))
            return Memory.WAIT;

        data[lineNum] = line;

        return 1;
    }

    //Method of direct access during write back since the cache line data is known
    public int readCache(String callingFrom, int tag , int offset) {


        return data[tag][offset];
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
}