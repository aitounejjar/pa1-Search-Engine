package cs276.assignments;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class VBIndex implements BaseIndex {

    private static final int INT_SIZE = 4;
    private static final int ONE_TWENTY_EIGHT = 128;

	@Override
	public PostingList readPosting(FileChannel fc) {
		/*
		 * TODO: Your code here
		 */

		// read a sequence of bytes that have continuation bits of 0, except for the last byte which has CB set to 1
        // concatenate the 7 low bits of each byte to construct the gap, which then needs to be decoded to get the document id




		return null;
	}

	@Override
	public void writePosting(FileChannel fc, PostingList p) {
		/*
		 * TODO: Your code here
		 * an entry in the VB compressed index looks like: [termId] [gap1] [gap2] .... [gapN]
		 */

		/*
		 * The allocated space is for termID + freq + docIds in p
		 */
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE);
        buffer.putInt(p.getTermId()); // put termId

		/* Flip the buffer so that the position is set to zero.
		 * This is the counterpart of buffer.rewind()
		 */
        buffer.flip();

		/*
		 * fc.write writes a sequence of bytes into fc from buffer.
		 * File position is updated with the number of bytes actually
		 * written
		 */
        try {
            fc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Integer> gaps = create_gaps(p.getList());
        ByteArrayOutputStream stream = vb_encode(gaps);
        try {
            fc.write(ByteBuffer.wrap(stream.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

	private ByteArrayOutputStream vb_encode(List<Integer> pGaps) {
        ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
        for (int gap : pGaps) {
            byte[] bytes = vb_encode(gap);
            try {
                bytestream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	    return bytestream;
    }

    private byte[] vb_encode(int pGap) {


	    int size = 0;
	    byte[] bytes = new byte[size];
	    while (true) {
	        // do prepend bytes
            int remainder = pGap % ONE_TWENTY_EIGHT;
            bytes = prepend(bytes, remainder);
            if (pGap < ONE_TWENTY_EIGHT) {
                break;
            }
            pGap = pGap/ONE_TWENTY_EIGHT;
            bytes[bytes.length] += ONE_TWENTY_EIGHT;
        }


	    return bytes;
    }

    private byte[] prepend(byte[] pBytes, int pDecimal) {

	    byte[] a = new byte[pBytes.length+1];
        int i=0;
	    for (; i<pBytes.length; i++) {
	        a[i] = pBytes[i];
        }
        a[i] = (byte) pDecimal;




	    // TODO: append pDecimal to pBytes and return the result
        BitSet bitSet = new BitSet();
        // TODO: do some bit ops
	    return a;
    }


    private List<Integer> vb_decode(ByteArrayOutputStream pStream) {
        List<Integer> docIds = new ArrayList<>();

        return docIds;
    }

    private List<Integer> create_gaps(List<Integer> pDocIds) {
        List<Integer> gaps = new ArrayList<>(pDocIds.size());
        gaps.add(pDocIds.get(0));
        for (int i=1; i<pDocIds.size(); i++) {
            gaps.add( pDocIds.get(i) - pDocIds.get(i-1) );
        }
        return gaps;
    }

}
