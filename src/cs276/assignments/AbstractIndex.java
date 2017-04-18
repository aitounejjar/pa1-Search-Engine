package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public abstract class AbstractIndex implements BaseIndex {

    // number of bytes in an int
    public static final int INT_SIZE = 4;

    /**
     * Gap encodes a postings list.  The DocIds in the postings list are provided
     * in the array inputDocIdsOutputGaps.  The output gaps are placed right back
     * into this array, replacing each docId with the corresponding gap.
     *
     * Example:
     * If inputDocIdsOutputGaps is initially {5, 1000, 1005, 1100}
     * then at the end inputDocIdsOutputGaps is set to {5, 995, 5, 95}
     *
     * @param inputDocIdsOutputGaps The array of input docIds.
     *                              The output gaps are placed back into this array!
     */

     void gapEncode(int[] inputDocIdsOutputGaps) {

        // TODO: Fill in your code here

        if (inputDocIdsOutputGaps.length == 0) {
            return;
        }

        int tracker = inputDocIdsOutputGaps[0];
        for (int i=1; i<inputDocIdsOutputGaps.length; ++i) {
            inputDocIdsOutputGaps[i] = inputDocIdsOutputGaps[i] - tracker;
            tracker += inputDocIdsOutputGaps[i];
        }

    }

    /**
     * Decodes a gap encoded postings list into the corresponding docIds.  The input
     * gaps are provided in inputGapsOutputDocIds.  The output docIds are placed
     * right back into this array, replacing each gap with the corresponding docId.
     *
     * Example:
     * If inputGapsOutputDocIds is initially {5, 905, 5, 95}
     * then at the end inputGapsOutputDocIds is set to {5, 1000, 1005, 1100}
     *
     * @param inputGapsOutputDocIds The array of input gaps.
     *                              The output docIds are placed back into this array.
     */
     int[] gapDecode(int[] inputGapsOutputDocIds) {
        // TODO: Fill in your code here
        for (int i=1; i<inputGapsOutputDocIds.length; ++i) {
            inputGapsOutputDocIds[i] = inputGapsOutputDocIds[i] + inputGapsOutputDocIds[i-1];
        }
        return inputGapsOutputDocIds;
    }

    public void writeHelper(FileChannel fc, ByteBuffer buffer) {
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int readHelper(FileChannel fc, ByteBuffer buffer) {
        int numOfBytesRead = -1;
        try {
            numOfBytesRead = fc.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfBytesRead;
    }

    public int[] getIntArray(List<Integer> list) {
        return list.stream().mapToInt(i->i).toArray();
    }

}
