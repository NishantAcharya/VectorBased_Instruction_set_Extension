public class Memory {

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

    // Just for cache
    public int writeLine(String callingFrom, int lineNum, int[] line) {
        if (needsToWait(callingFrom, lineNum))
            return Memory.WAIT;

        data[lineNum] = line;

        return 1;
    }
}