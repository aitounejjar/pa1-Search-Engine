package cs276.assignments;

import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();

	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();

	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();

	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();

	// Index
	private static BaseIndex index = null;

	
	/* 
	 * Write a posting list with a given termID from the file 
	 * You should seek to the file position of this specific
	 * posting list and read it back.
	 * */
	private static PostingList readPosting(FileChannel fc, int termId) throws IOException {
		/*
		 * TODO: Your code here
		 */

		long position = posDict.get(termId);
        fc.position(position);
        PostingList result = null;
        try {
            result = index.readPosting(fc);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return result;
	}

	public static void main(String[] args) throws IOException {
		/* Parse command line */
		if (args.length != 2) {
			System.err.println("Usage: java Query [Basic|VB|Gamma] index_dir");
			return;
		}

		/* Get index */
		String className = "cs276.assignments." + args[0] + "Index";
		try {
			Class<?> indexClass = Class.forName(className);
			index = (BaseIndex) indexClass.newInstance();
		} catch (Exception e) {
			System.err
					.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
			throw new RuntimeException(e);
		}

		/* Get index directory */
		String input = args[1];
		File inputdir = new File(input);
		if (!inputdir.exists() || !inputdir.isDirectory()) {
			System.err.println("Invalid index directory: " + input);
			return;
		}

		/* Index file */
		RandomAccessFile indexFile = new RandomAccessFile(new File(input, "corpus.index"), "r");

		String line = null;
		/* Term dictionary */
		BufferedReader termReader = new BufferedReader(new FileReader(new File(input, "term.dict")));
		while ((line = termReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			termDict.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			docDict.put(Integer.parseInt(tokens[1]), tokens[0]);
		}
		docReader.close();

		/* Posting dictionary */
		BufferedReader postReader = new BufferedReader(new FileReader(new File(input, "posting.dict")));
		while ((line = postReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			posDict.put(Integer.parseInt(tokens[0]), Long.parseLong(tokens[1]));
			freqDict.put(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[2]));
		}
		postReader.close();

		/* Processing queries */
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		/* For each query */
		while ((line = br.readLine()) != null) {
			/*
			 * TODO: Your code here
			 *       Perform query processing with the inverted index.
			 *       Make sure to print to stdout the list of documents
			 *       containing the query terms, one document file on each
			 *       line, sorted in lexicographical order.
			 */

            LinkedList<List<Integer>> queue = new LinkedList<>();

			String[] query_terms = line.split("\\s+");

			// (termId, termFrequency) pairs
            List<Pair<Integer, Integer>> pairs = new ArrayList<>(query_terms.length);
			for (String q : query_terms) {
                int termId = termDict.get(q);
                int termFreq = freqDict.get(termId);

                PostingList p = readPosting(indexFile.getChannel(), termId);

                pairs.add( new Pair(termId, termFreq) );
            }

            Collections.sort(pairs, new Comparator<Pair<Integer, Integer>>() {
                @Override
                public int compare(Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2) {
                    return pair1.getSecond().compareTo(pair2.getSecond());
                }
            });


			// TBD ...

		}
		br.close();
		indexFile.close();
	}

	private static List<Integer> interset(RandomAccessFile indexFile, int t1, int t2) {
        PostingList p1 = null;
        PostingList p2 = null;
        try {
            p1 = readPosting(indexFile.getChannel(), t1);
            p2 = readPosting(indexFile.getChannel(), t2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Integer> result  = new ArrayList<>();
        List<Integer> l1 = p1.getList();
        List<Integer> l2 = p2.getList();

        // do the intersect here ....

	    return result;
    }

}
