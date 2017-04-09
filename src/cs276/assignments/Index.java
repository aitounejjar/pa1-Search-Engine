package cs276.assignments;

//import cs276.util.TermIdDocIdPairComparator;

import cs276.util.IndexUtils;
import cs276.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Index {

    private static boolean DEBUG_FLAG = true;

    // Term id -> (position in index file, doc frequency) dictionary
    private static Map<Integer, Pair<Long, Integer>> postingDict = new TreeMap<Integer, Pair<Long, Integer>>();

    // Doc name -> doc id dictionary
    private static Map<String, Integer> docDict = new TreeMap<String, Integer>();
    private static Map<Integer, String> docDict_reversed = new TreeMap<>();

    // Term -> term id dictionary. This makes index construction more efficient. How ?
    private static Map<String, Integer> termDict = new TreeMap<String, Integer>();
    private static Map<Integer, String> termDict_reversed = new TreeMap<>();

    // Block queue
    private static LinkedList<File> blockQueue = new LinkedList<File>();

    // Total file counter
    private static int totalFileCount = 0;

    // Document counter
    private static int docIdCounter = 0;

    // Term counter
    private static int wordIdCounter = 0;

    // Index
    private static BaseIndex index = null;


    /*
     * Write a posting list to the given file
     * You should record the file position of this posting list
     * so that you can read it back during retrieval
     *
     * */
    private static void writePosting(FileChannel fc, PostingList posting) throws IOException {
		/*
		 * TODO: Your code here
		 *
		 */

        postingDict.put(posting.getTermId(), new Pair<Long, Integer>(fc.position(), posting.getList().size()));

        try {
            index.writePosting(fc, posting);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }


    }

    public static void main(String[] args) throws IOException {

		/* Parse command line */
        if (args.length != 3) {
            System.err.println("Usage: java Index [Basic|VB|Gamma] data_dir output_dir");
            return;
        }

		/* Get index */
        String className = "cs276.assignments." + args[0] + "Index";
        try {
            Class<?> indexClass = Class.forName(className);
            index = (BaseIndex) indexClass.newInstance();
        } catch (Exception e) {
            System.err.println("Index method must be \"Basic\", \"VB\", or \"Gamma\"");
            throw new RuntimeException(e);
        }

		/* Get root directory */
        String root = args[1];
        File rootdir = new File(root);
        if (!rootdir.exists() || !rootdir.isDirectory()) {
            System.err.println("Invalid data directory: " + root);
            return;
        }

		/* Get output directory */
        String output = args[2];

        File outdir = new File(output);
        deleteDirectory(outdir);

        if (outdir.exists() && !outdir.isDirectory()) {
            System.err.println("Invalid output directory: " + output);
            return;
        }

        if (!outdir.exists()) {
            if (!outdir.mkdirs()) {
                System.err.println("Create output directory failure");
                return;
            }
        }

		/* A filter to get rid of all files starting with .*/
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                return !name.startsWith(".");
            }
        };

		/* BSBI indexing algorithm */

        // get the list of files under that the rootdir (i.e. the data_dir)
        File[] dirlist = rootdir.listFiles(filter);

        //--------------------------------------------------------------------------------------------------------------
        // create, and write to disk, an inverted index for each of the blocks
        //--------------------------------------------------------------------------------------------------------------

		/* For each block - each subdirectory is treated as a block in our cases */
        for (File block : dirlist) {

            // keys are termIds, values are posting lists
            Map<Integer, PostingList> inv_index = new TreeMap<>();

            File blockFile = new File(output, block.getName());
            blockQueue.add(blockFile);

            File blockDir = new File(root, block.getName());
            File[] filelist = blockDir.listFiles(filter);

			/* For each file */
            for (File file : filelist) {
                ++totalFileCount;
                String fileName = block.getName() + "/" + file.getName();
                docDict.put(fileName, ++docIdCounter);
                docDict_reversed.put(docIdCounter, fileName);

                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.trim().split("\\s+");
                    for (String token : tokens) {

					    /*
						 * TODO: Your code here
						 *       For each term, build up a list of
						 *       documents in which the term occurs
						 */

                        int termId;
                        if (!termDict.containsKey(token)) {
                            // add the new term to dictionary
                            termDict.put(token, ++wordIdCounter);
                            termDict_reversed.put(wordIdCounter, token);
                            // assign it an empty postings list
                            inv_index.put(wordIdCounter, new PostingList(wordIdCounter, token));
                            termId = wordIdCounter;
                        } else {
                            termId = termDict.get(token);
                            if (inv_index.get(termId) == null) {
                                // this term already exists in the dictionary, but this is the first time it was discovered in this block
                                inv_index.put(termId, new PostingList(termId, token));
                            }
                        }

                        List<Integer> docs = inv_index.get(termId).getList();
                        if (!docs.contains(docIdCounter)) {
                            docs.add(docIdCounter);
                        }

                        inv_index.put(termId, new PostingList(termId, token, docs));

                    }
                }
                reader.close();

            } // end - for each file

			/* Sort and output */
            if (!blockFile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bfc = new RandomAccessFile(blockFile, "rw");

			/*
			 * TODO: Your code here
			 *       Write all posting lists for all terms to file (bfc)
			 */

            for (int tId : inv_index.keySet()) {
                writePosting(bfc.getChannel(), inv_index.get(tId));
            }

            // print the resulting inv. index for the current block
            print_helper("Printing the index for the block " + block.getName() + ":" );
            for ( Integer i : inv_index.keySet() ) {
                print_helper(inv_index.get(i).toString());
            }

            bfc.close();
        } /* end - for each block

		// by now, for each each block an inverted index has been constructed and written to disk
		// next step, and final one, is to merge all of these inverted indexes into a single one

		//--------------------------------------------------------------------------------------------------------------
        // merge the many inverted indexes into a single one
        //--------------------------------------------------------------------------------------------------------------

		/* Required: output total number of files. */
        System.out.println(totalFileCount);

		/* Merge blocks */
        while (true) {
            if (blockQueue.size() <= 1)
                break;

            File b1 = blockQueue.removeFirst();
            File b2 = blockQueue.removeFirst();

            File combfile = new File(output, b1.getName() + "+" + b2.getName());
            if (!combfile.createNewFile()) {
                System.err.println("Create new block failure.");
                return;
            }

            RandomAccessFile bf1 = new RandomAccessFile(b1, "r");
            RandomAccessFile bf2 = new RandomAccessFile(b2, "r");
            RandomAccessFile mf = new RandomAccessFile(combfile, "rw");

            FileChannel fc1 = bf1.getChannel();
            FileChannel fc2 = bf2.getChannel();
            FileChannel mc = mf.getChannel();

			/*
			 * TODO: Your code here
			 *       Combine blocks bf1 and bf2 into our combined file, mf
			 *       You will want to consider in what order to merge
			 *       the two blocks (based on term ID, perhaps?).
			 *
			 */

            PostingList p1 = null;
            PostingList p2 = null;

            try {
                p1 = index.readPosting(fc1);
                p2 = index.readPosting(fc2);

                print_helper("Printing the merged index of blocks '" + b1.getName() + "' and '" +  b2.getName() + "'");

                while (p1!= null || p2!=null) {

                    if (p1==null && p2==null) {
                        // we have read all the postings in both inverted indexes
                        break;
                    }

                    int term1 = (p1==null) ? -1 : p1.getTermId();
                    int term2 = (p2==null) ? -1 : p2.getTermId();

                    if (p1 != null) {
                        p1.setTermStr(termDict_reversed.get(term1));
                    }

                    if (p2 != null) {
                        p2.setTermStr(termDict_reversed.get(term2));
                    }

                    // TBD - do the actual merge ...
                    if (term1 == term2) {
                        List<Integer> merged = IndexUtils.mergePostingLists(p1.getList(), p2.getList());
                        index.writePosting(mf.getChannel(), new PostingList(term1, merged));

                        print_helper( new PostingList(term1, termDict_reversed.get(term1), merged).toString() );

                        // read next posting lists
                        p1 = index.readPosting(fc1);
                        p2 = index.readPosting(fc2);

                    } else {
                        if (term1 == -1) {
                            index.writePosting(mc, p2);
                            print_helper(p2!=null ? p2.toString() : "");
                            p2 = index.readPosting(fc2);
                        } else if (term2 == -1) {
                            index.writePosting(mc, p1);
                            print_helper(p1!=null ? p1.toString() : "");
                            p1 = index.readPosting(fc1);
                        } else if (term1 < term2) {
                            index.writePosting(mc, p1);
                            print_helper(p1!=null ? p1.toString() : "");
                            p1 = index.readPosting(fc1);
                        } else {
                            index.writePosting(mc, p2);
                            print_helper(p2!=null ? p2.toString() : "");
                            p2 = index.readPosting(fc2);
                        }
                    }

                }

            } catch (Throwable ex) {
                ex.printStackTrace();
            }

            bf1.close();
            bf2.close();
            mf.close();
            b1.delete();
            b2.delete();

            blockQueue.add(combfile);
        }

		/* Dump constructed index back into file system */
        File indexFile = blockQueue.removeFirst();
        indexFile.renameTo(new File(output, "corpus.index"));

        BufferedWriter termWriter = new BufferedWriter(new FileWriter(new File(output, "term.dict")));
        for (String term : termDict.keySet()) {
            termWriter.write(term + "\t" + termDict.get(term) + "\n");
        }
        termWriter.close();

        BufferedWriter docWriter = new BufferedWriter(new FileWriter(new File(output, "doc.dict")));
        for (String doc : docDict.keySet()) {
            docWriter.write(doc + "\t" + docDict.get(doc) + "\n");
        }
        docWriter.close();

        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File(output, "posting.dict")));
        for (Integer termId : postingDict.keySet()) {
            postWriter.write(termId + "\t" + postingDict.get(termId).getFirst() + "\t" + postingDict.get(termId).getSecond() + "\n");
        }
        postWriter.close();
    }

    public static String getTermStr(int termId) {
        return termDict_reversed.get(termId);
    }

    //--------------------------------------------------------------------------------------------------------------
    // private method(s)
    //--------------------------------------------------------------------------------------------------------------

    private static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    private static void print_helper(String ... strings) {
        if (DEBUG_FLAG) {
            for (String s : strings) {
                System.out.println(s);
            }
        }
    }

}
