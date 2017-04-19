package cs276.assignments;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class BasicIndex extends AbstractIndex {

    @Override
    public void writePosting(FileChannel fc, PostingList p) throws Throwable {

        /*
		 * The allocated space is for termID + freq + docIds in p
		 */
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * (p.getList().size() + 2));
        buffer.putInt(p.getTermId()); // put termId
        buffer.putInt(p.getList().size()); // put freq
        for (int id : p.getList()) { // put docIds
            buffer.putInt(id);
        }

		/* Flip the buffer so that the position is set to zero.
		 * This is the counterpart of buffer.rewind()
		 */
        buffer.flip();

        /*
		 * fc.write writes a sequence of bytes into fc from buffer.
		 * File position is updated with the number of bytes actually
		 * written
		 */
        writeHelper(fc, buffer);
    }

    @Override
    public PostingList readPosting(FileChannel fc) throws Throwable {

        /*
		 * Allocate two integers, preparing for reading in termId and freq
		 */
        ByteBuffer buffer = ByteBuffer.allocate(INT_SIZE * 2);

        /*
		 * fc.read reads a sequence of bytes from the fc channel into
		 * buffer. Bytes are read starting at this channel's current
		 * file position, and then the file position is updated
		 * with the number of bytes actually read.
		 */
        int numOfBytesRead = readHelper(fc, buffer);

        /**
         * if no bytes were read ...
         */
        if (numOfBytesRead == -1) { return null; }

        /*
		 * Rewinds the buffer. Position is set to zero.
		 * We are ready to get our termId and frequency.
		 */
        buffer.rewind();

		/*
		 * Reads the next four bytes at buffer's current position, 
		 * composing them into an int value according to the 
		 * current byte order, and then increments the position 
		 * by four.
		 */
        int termId = buffer.getInt();
        int freq = buffer.getInt();
		
		/* TODO:
		 * You should create a PostingList and use buffer 
		 * to fill it with docIds, then return the PostingList 
		 * you created.
		 * Hint: This differs from reading in termId/freq only 
		 * in the number of ints to be read in.
		 */

        buffer = ByteBuffer.allocate(INT_SIZE * freq);
        numOfBytesRead = readHelper(fc, buffer);
        if (numOfBytesRead == -1) { return null; }
        buffer.rewind();

        List<Integer> docIds = new ArrayList<>(freq);
        for (int i = 0; i < freq; ++i) {
            int docId = buffer.getInt();
            docIds.add(docId);
        }

        return new PostingList(termId, docIds);
    }
}
