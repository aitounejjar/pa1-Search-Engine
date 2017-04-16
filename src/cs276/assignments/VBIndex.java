package cs276.assignments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class VBIndex implements BaseIndex {

    // number of bytes in an int
    private static final int INT_SIZE = 4;

    // max number of bytes in a vb code
    private static final int MAX_VB_BYTES = Integer.SIZE / 7 + 1;

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		/*
		 * TODO: Your code here
		 * entries in the vb compressed index are in the form: [termId] [vbCodeLength] [gap1] [gap2] .... [gapN]
		 */

		/* encode the posting list */
        int[] arr = p.getListAsArray();
        gapEncode(arr);
        ByteArrayOutputStream stream = vb_encode(arr);
        int vbCodeLength = stream.size();

        if (vbCodeLength < arr.length) {
            throw new RuntimeException("Number of bytes in the vb code must be at least equal to number of encoded gaps.");
        }

        if (arr.length > 10000) {
            int thisIsCrazy = 0;
        }

        // allocate
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE*2 + vbCodeLength);

        // put
        buffer.putInt(p.getTermId());
        buffer.putInt(vbCodeLength);
        buffer.put(stream.toByteArray());

        // flip
        buffer.flip();

        // write
        try { fc.write(buffer); } catch (IOException e) { e.printStackTrace(); }

        /*
        / * writing the termId * /
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE);
        buffer.putInt(p.getTermId());
        buffer.flip();
        //writeHelper(fc, buffer);
        try { fc.write(buffer); } catch (IOException e) { e.printStackTrace(); }

        / * writing the vb code length * /
        buffer = ByteBuffer.allocate(INT_SIZE);
        buffer.putInt(vbCodeLength);
        buffer.flip();
        //writeHelper(fc, buffer);
        try { fc.write(buffer); } catch (IOException e) { e.printStackTrace(); }

        / * writing the encoded bytes * /
        buffer = ByteBuffer.allocate(stream.size());
        buffer.put(stream.toByteArray());
        buffer.flip();
        //writeHelper(fc, buffer);
        try { fc.write(buffer); } catch (IOException e) { e.printStackTrace(); }
        */

    }

    @Override
    public PostingList readPosting(FileChannel fc) {
		/*
		 * TODO: Your code here
		 */

        /* read the term id and vb code length */
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE*2);
        //readHelper(fc, buffer);
        int numOfBytesRead;
        try {
            numOfBytesRead = fc.read(buffer);
            if (numOfBytesRead == -1) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.rewind();
        int termId = buffer.getInt();
        int vbCodeLength = buffer.getInt();


        buffer = ByteBuffer.allocate(vbCodeLength);
        try {
            numOfBytesRead = fc.read(buffer);
            if (numOfBytesRead == -1) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.rewind();

        byte[] allBytes = new byte[vbCodeLength];
        buffer.get(allBytes, 0, vbCodeLength);

        List<Integer> gaps = new ArrayList<>();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i=0; i<allBytes.length; ++i) {
            byte b = allBytes[i];
            if (isMsbSet(b)) {
                baos.write(b);
                int gap = VBDecodeInteger(baos.toByteArray(), 0, new int[2]);
                gaps.add(gap);
                baos = new ByteArrayOutputStream();
            } else {
                baos.write(b);
            }
        }

        int[] arr = getIntArray(gaps);
        int[] docIds = gapDecode(arr);

        return new PostingList(termId, docIds);

    }

    private ByteArrayOutputStream vb_encode(int[] gaps) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    for (int i=0; i<gaps.length; ++i) {

	        byte[] b = new byte[MAX_VB_BYTES];
	        int numBytes = VBEncodeInteger(gaps[i], b);

            stream.write(b, 0, numBytes);

        }
        return stream;
    }

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
    private void gapEncode(int[] inputDocIdsOutputGaps) {

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
    private int[] gapDecode(int[] inputGapsOutputDocIds) {
        // TODO: Fill in your code here
        for (int i=1; i<inputGapsOutputDocIds.length; ++i) {
            inputGapsOutputDocIds[i] = inputGapsOutputDocIds[i] + inputGapsOutputDocIds[i-1];
            if (inputGapsOutputDocIds[i] > 10000) {
                int thisIsCrazy = 0;
            }
        }
        return inputGapsOutputDocIds;
    }

    /**
     * Encodes gap using a VB code.  The encoded bytes are placed in outputVBCode.
     * Returns the number bytes placed in outputVBCode.
     *
     * @param gap          gap to be encoded.  Assumed to be greater than or equal to 0.
     * @param outputVBCode VB encoded bytes are placed here.  This byte array is assumed to be large
     *                     enough to hold the VB code for gap (e.g., Integer.SIZE/7 + 1).
     * @return Number of bytes placed in outputVBCode.
     */
    private int VBEncodeInteger(int gap, byte[] outputVBCode) {
        int numBytes = 0;

        // TODO: Fill in your code here

        while (true) {
            if (gap < 128) {
                byte b = (byte) (gap); // set continuation bit
                if (numBytes==0) {
                    b = (byte) (gap | 0b10000000);
                }
                prependToArray(outputVBCode, b);
                numBytes++;
                break;
            }

            byte low7bits = (byte) (gap & 0b01111111); // get low 7 bits
            if (numBytes==0) {
                low7bits = (byte) (low7bits | 0b10000000); // set continuation bit
            }

            prependToArray(outputVBCode, low7bits);
            numBytes++;
            gap = gap >> 7;
        }

        validateVBCode(outputVBCode);

        return numBytes;

    }

    /**
     * Decodes the first integer encoded in inputVBCode starting at index startIndex.  The decoded
     * number is placed in the element zero of the numberEndIndex array and the index position
     * immediately after the encoded value is placed in element one of the numberEndIndex array.
     *
     * @param inputVBCode    Byte array containing the VB encoded number starting at index startIndex.
     * @param startIndex     Index in inputVBCode where the VB encoded number starts
     * @param numberEndIndex Outputs are placed in this array.  The first element is set to the
     *                       decoded number and the second element is set to the index of inputVBCode
     *                       immediately after the end of the VB encoded number.
     * @throws IllegalArgumentException If not a valid variable byte code
     */
    private int VBDecodeInteger(byte[] inputVBCode, int startIndex, int[] numberEndIndex) {
        // TODO: Fill in your code here

        //validateVBCode(inputVBCode);

        int result = 0;
        int i = inputVBCode.length - 1;
        int counter = 0;
        for (; i>=0; i--) {
            // TODO: do some validations ... msb must be one for the first byte read, and 0 for the remaining bytes ...
            byte temp = inputVBCode[i];
            if ( ( counter==0 && (((temp>>7)&1) != 1)) || ( counter !=0 && (((temp>>7)&1) == 1)) ) {
                // a vb code is invalid if the continuation bit isn't set for the first byte
                // or if it was set for one of subsequent bytes
                throw new IllegalArgumentException("Oups ... an invalid was detected !");
            }
            temp = (byte) (temp & 0b01111111); // unset the high bit
            result = (temp << (7 * counter)) | result;
            counter++;

        }

        numberEndIndex[0] = result;
        numberEndIndex[1] = counter;

        return result;

    }

    private void prependToArray(byte[] a, byte b) {
        // TODO: handle edge cases
        for (int i=a.length-2; i>=0; --i) {
            a[i+1] = a[i];
        }
        a[0] = b;
        validateVBCode(a);
    }

    // returns true if the msb was set on the passed byte, otherwise returns false
    private boolean isMsbSet(byte b) {
        return ((b>>7)&1) == 1;
    }

    private void xxxwriteHelper(FileChannel fc, ByteBuffer buffer) {
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void xxxreadHelper(FileChannel fc, ByteBuffer buffer) {
        int numOfBytesRead;
        try {
            numOfBytesRead = fc.read(buffer);
            if (numOfBytesRead == -1) {
                throw new RuntimeException("Could not read from the buffer.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[] getIntArray(List<Integer> list) {
        return list.stream().mapToInt(i->i).toArray();
    }

    private void validateVBCode(byte[] bytes) {

        if (bytes.length > 5) {
            throw new IllegalArgumentException("VB code length for a single integer should never exceed 5. Actual size is: " + bytes.length);
        }

        // E.g.: [2] [0] [3] [0] [0] <- i

        boolean nonZeroEncountered = false;
        int indexOfRightMostNonZero = bytes.length-1;

        for (int i=bytes.length-1; i>=0; i--) {
            if (!nonZeroEncountered && bytes[i] == 0) {
                continue;
            }

            if (!nonZeroEncountered) {
                indexOfRightMostNonZero = i;
                nonZeroEncountered = true;
            }

            if (indexOfRightMostNonZero == i) {
                if (! isMsbSet(bytes[i])) {
                    throw new IllegalArgumentException("Wrong vb code: expecting 1 in msb, but found 0.");
                }
            } else {
                if (isMsbSet(bytes[i])) {
                    throw new IllegalArgumentException("Wrong vb code: expecting 0 in msb, but found 1.");
                }
            }
        }

        int counter = 0;
        for (int i=0; i<bytes.length; i++) {
            if (isMsbSet(bytes[i])) {
                counter++;
            }
        }
        if (counter != 1) {
            throw new IllegalArgumentException("A vb code must have exactly one byte with the msb set. Found: " + counter);
        }
    }

}
