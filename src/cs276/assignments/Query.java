package cs276.assignments;

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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Query {

	// Term id -> position in index file
	private static Map<Integer, Long> posDict = new TreeMap<Integer, Long>();

	// Term id -> document frequency
	private static Map<Integer, Integer> freqDict = new TreeMap<Integer, Integer>();

	// Doc id -> doc name dictionary
	private static Map<Integer, String> docDict = new TreeMap<Integer, String>();
	private static Map<String, Integer> docDict_reversed = new TreeMap<>();

	// Term -> term id dictionary
	private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
	private static Map<Integer, String> termDict_reversed = new TreeMap<>();

	// Index
	private static BaseIndex index = null;

	private static final String NO_RESULTS_FOUND = "no results found";
	
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
            result.setTermStr(termDict_reversed.get(termId));
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
			String termStr = tokens[0];
			int termId = Integer.parseInt(tokens[1]);
			termDict.put(termStr, termId);
			termDict_reversed.put(termId, termStr);
		}
		termReader.close();

		/* Doc dictionary */
		BufferedReader docReader = new BufferedReader(new FileReader(new File(input, "doc.dict")));
		while ((line = docReader.readLine()) != null) {
			String[] tokens = line.split("\t");
			int docId = Integer.parseInt(tokens[1]);
			String docName = tokens[0];
			docDict.put(docId, docName);
			docDict_reversed.put(docName, docId);
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



			doQuery(line, indexFile);


		}
		br.close();
		indexFile.close();
	}

	private static void doQuery(String line, RandomAccessFile indexFile) {
        LinkedList<List<Integer>> queue = new LinkedList<>();

        String[] query_terms = line.split("\\s+");

        List<PostingList> postings = new ArrayList<>();

        Set<String> set = new HashSet<>();

        for (String q : query_terms) {

            if (set.contains(q)) {
                continue;
            }

            set.add(q);

            Integer termId = termDict.get(q);
            if (termId == null) {
                System.out.println(NO_RESULTS_FOUND);
                return;

            }

            PostingList p = null;
            try {
                p = readPosting(indexFile.getChannel(), termId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            postings.add(p);
        }

        Collections.sort(postings, new Comparator<PostingList>() {
            @Override
            public int compare(PostingList p1, PostingList p2) {
                Integer freq1 = p1.getList().size();
                Integer freq2 = p2.getList().size();
                return freq1.compareTo(freq2);
            }
        });


        for (PostingList p : postings) {
            queue.add(p.getList());
        }

        boolean search_aborted = false;
        while (true) {
            if (queue.size() <= 1) {
                break;
            }

            List<Integer> combined = intersect(queue.removeFirst(), queue.removeFirst());
            if (combined.isEmpty()) {
                // posting lists of two terms didn't have any intersection
                // no need to proceed further
                search_aborted = true;
                break;
            }
            queue.addFirst(combined);

        }

        if (!search_aborted) {
            List<Integer> docIds = queue.getFirst();
            // sorting the document ids in lexicographical order
            Collections.sort(docIds, new Comparator<Integer>() {
                @Override
                public int compare(Integer docId1, Integer docId2) {
                    String docName1 = docDict.get(docId1);
                    String docName2 = docDict.get(docId2);
                    return docName1.compareTo(docName2);
                }
            });
            for (int docId : docIds) {
                System.out.println(docDict.get(docId));
            }
            System.out.println("----> total: " + docIds.size());
        } else {
            System.out.println(NO_RESULTS_FOUND);
        }

    }

	private static List<Integer> intersect(List<Integer> list1, List<Integer> list2) {

	    int size1 = list1.size();
	    int size2 = list2.size();

        List<Integer> result = new ArrayList<>();

        int i=0, j=0;

        while ( i<size1 && j<size2 ) {
            int docId1 = list1.get(i);
            int docId2 = list2.get(j);
            if (docId1 == docId2) {
                result.add(docId1);
                i++;
                j++;
            } else if (docId1 < docId2) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }

}
