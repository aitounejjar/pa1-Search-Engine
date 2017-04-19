package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GammaIndex extends AbstractIndex {

	@Override
	public void writePosting(FileChannel fc, PostingList p) {

	    /*
		 * TODO: Your code here
		 */

		/* encode the posting list */
        int[] arr = p.getListAsArray();
        gapEncode(arr);

        BitSet bitSet = new BitSet();

        for (int i=0; i<arr.length; ++i) {
            int gap = arr[i];
            gammaEncodeInteger(gap, bitSet, bitSet.length());
        }

        int gammaCodeLen = Math.max(1, bitSet.toByteArray().length);
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE*2 + gammaCodeLen);
        buffer.putInt(p.getTermId());
        buffer.putInt(gammaCodeLen);
        buffer.put( bitSet.length() != 0 ? bitSet.toByteArray() : new byte[]{0});

        // flip
        buffer.flip();

        // write
        try { fc.write(buffer); } catch (IOException e) { e.printStackTrace(); }

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
        int gammaCodeLength = buffer.getInt();

        buffer = ByteBuffer.allocate(gammaCodeLength);
        try {
            numOfBytesRead = fc.read(buffer);
            if (numOfBytesRead == -1) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.rewind();

        List<Integer> gaps = new ArrayList<>();
        byte[] allBytes = new byte[gammaCodeLength];
        buffer.get(allBytes, 0, gammaCodeLength);

        // gamma decode
        BitSet bitSet = BitSet.valueOf(allBytes);

        int index = 0;

        while (index != bitSet.length()) {
            boolean bit = bitSet.get(index);
            if (bit) {
                int gap = gammaDecodeInteger(bitSet, index, new int[2]);
                gaps.add(gap);
            }
        }


        int[] a = getIntArray(gaps);
        gapDecode(a);
        return new PostingList(termId, a);
    }

    /**
     * Gamma encodes number.  The encoded bits are placed in BitSet outputGammaCode starting at
     * (0-based) index position startIndex.  Returns the index position immediately following the
     * encoded bits.  If you try to gamma encode 0, then the return value should be startIndex (i.e.,
     * it does nothing).
     *
     * @param number          Number to be gamma encoded
     * @param outputGammaCode Gamma encoded bits are placed in this BitSet starting at startIndex
     * @param startIndex      Encoded bits start at this index position in outputGammaCode
     * @return Index position in outputGammaCode immediately following the encoded bits
     */
    public static int gammaEncodeInteger(int number, BitSet outputGammaCode, int startIndex) {

        //outputGammaCode.clear();

        if (number == 0 || number == 1){
            outputGammaCode.clear(0);
            startIndex++;
            return 1;
        }

        BitSet offset = convert(number);

        int length = offset.length()-1;
        offset.clear(length);

        unaryEncodeInteger(length, offset, length+1);

        //reverse bitset
        for (int i = 0; i<offset.length();i++){
            //if (offset.get(offset.length()-1-i)==check.get(0)){
            if (offset.get(offset.length()-1-i) ){
                outputGammaCode.set(i);
            }else{
                outputGammaCode.clear(i);
            }
        }

        return (2*length+1);
    }

    /**
     * Decodes the Gamma encoded number in BitSet inputGammaCode starting at (0-based) index startIndex.
     * The decoded number is returned in numberEndIndex[0] and the index position immediately following
     * the encoded value in inputGammaCode is returned in numberEndIndex[1].
     *
     * @param inputGammaCode BitSet containing the gamma code
     * @param startIndex     Gamma code starts at this index position
     * @param numberEndIndex Return values: index 0 holds the decoded number; index 1 holds the index
     *                       position in inputGammaCode immediately following the gamma code.
     */
    public static int gammaDecodeInteger(BitSet inputGammaCode, int startIndex, int[] numberEndIndex) {
        // TODO: Fill in your code here
        int zero = inputGammaCode.nextClearBit(0);
        BitSet bitset_length = inputGammaCode;
        bitset_length = inputGammaCode.get(0, zero+1);

        int[] number= new int[2];

        //Length of length bits
        unaryDecodeInteger(bitset_length,0, number);
        int length = number[0];

        //Length of all the bits
        int total_length = inputGammaCode.length();

        //Get all the Non-length bits
        BitSet bitset_value = inputGammaCode.get(number[0], number[0]+number[1]);

        bitset_value.set(0);

        int value = 0;
        for (int i = 0; i< number[1]; i++){
            value<<=1;
            if (bitset_value.get(i)) {
                value |= 1;
            }
        }

        // Convert result to hex
        String hex = Integer.toHexString(value);
        int parsedResult = (int) Long.parseLong(hex, 16);

        numberEndIndex[0] = parsedResult;
        numberEndIndex[1] = 0;

        return parsedResult;
    }

    /**
     * Encodes a number using unary code.  The unary code for the number is placed in the BitSet
     * outputUnaryCode starting at index startIndex.  The method returns the BitSet index that
     * immediately follows the end of the unary encoding.  Use startIndex = 0 to place the unary
     * encoding at the beginning of the outputUnaryCode.
     * <p>
     * Examples:
     * If number = 5, startIndex = 3, then unary code 111110 is placed in outputUnaryCode starting
     * at the 4th bit position and the return value 9.
     *
     * @param number          The number to be unary encoded
     * @param outputUnaryCode The unary code for number is placed into this BitSet
     * @param startIndex      The unary code for number starts at this index position in outputUnaryCode
     * @return The next index position in outputUnaryCode immediately following the unary code for number
     */
    public static int unaryEncodeInteger(int number, BitSet outputUnaryCode, int startIndex) {
        // TODO: Fill in your code here

        outputUnaryCode.set(startIndex, startIndex + number);
        //System.out.println("outputUnaryCode: " + createString(outputUnaryCode));
        return startIndex + number + 1;
    }

    /**
     * Decodes the unary coded number in BitSet inputUnaryCode starting at (0-based) index startIndex.
     * The decoded number is returned in numberEndIndex[0] and the index position immediately following
     * the encoded value in inputUnaryCode is returned in numberEndIndex[1].
     *
     * @param inputUnaryCode BitSet containing the unary code
     * @param startIndex     Unary code starts at this index position
     * @param numberEndIndex Return values: index 0 holds the decoded number; index 1 holds the index
     */
    public static int unaryDecodeInteger(BitSet inputUnaryCode, int startIndex, int[] numberEndIndex) {
        // TODO: Fill in your code here


        numberEndIndex[0] = inputUnaryCode.length();
        numberEndIndex[1] = inputUnaryCode.length() + 1;
        return startIndex;
    }

    public static BitSet convert(int value) {
        BitSet bits = new BitSet();
        int index = 0;
        while (value != 0) {
            if (value % 2 != 0) {
                bits.set(index);
            }
            ++index;
            value = value >>> 1;
        }
        return bits;
    }

    public static BitSet createBitSet(String bits) {
        BitSet outputBitSet = new BitSet();
        int bitIndex = 0;
        for (int i = 0; i < bits.length(); ++i) {
            if (bits.charAt(i) == '1') {
                outputBitSet.set(bitIndex++, true);
            } else if (bits.charAt(i) == '0') {
                outputBitSet.set(bitIndex++, false);
            }
        }
        return outputBitSet;
    }

}
