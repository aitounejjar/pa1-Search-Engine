package cs276.util;

import cs276.assignments.PostingList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class that holds various methods used by the indexing algorithms
 */

public class IndexUtils {

    public static List<Integer> mergePostingLists(List<Integer> list1, List<Integer> list2) {
        // TODO: implement the algorithm that merges two posting lists into a single one

        // Assumption(s)
        // - both posting lists come from two different inverted indexes, hence docIds are unique
        // - both lists are sorted in increasing order

        int element1 = list1.get(0);
        int element2 = list2.get(0);

        List<Integer> result = new ArrayList<>();

        if (element1 < element2) {
            result.addAll(list1);
            result.addAll(list2);
        } else {
            result.addAll(list2);
            result.addAll(list1);
        }

        return result;
    }

    /** Returns the next item from the Iterator, or null if it is exhausted.
     *  (This is a more C-like method than idiomatic Java, but we use it so as
     *  to be more parallel to the pseudo-code in the textbook.)
     */

    public static List<PostingList> mergeInvertedIndexes(List<PostingList> index1, List<PostingList> index2) {
        // resulting inverted index
        List<PostingList> result = new ArrayList<>();

        int i=0, j=0;

        int I_MAX = index1.size() - 1;
        int J_MAX = index2.size() - 1;

        int t1 = index1.get(i).getTermId();
        int t2 = index2.get(j).getTermId();

        while (i <= I_MAX || j <= J_MAX) {
            if (i<=I_MAX) {
                t1 = index1.get(i).getTermId();
            }
            if (j<=J_MAX) {
                t2 = index2.get(j).getTermId();
            }
            // Index1 complete, result filled by index2
            if (i > I_MAX){
                result.add(index2.get(j));
                j++;
            }else if ( j > J_MAX){
                result.add(index1.get(i));
                i++;
            }else if (t1 == t2) {
                List<Integer> merged = mergePostingLists(index1.get(i).getList(), index2.get(j).getList());
                PostingList list = new PostingList(t1, merged);
                result.add(list);
                i++;
                j++;
            }else if (t1 < t2) {
                result.add(index1.get(i));
                i++;
            } else if (t1 > t2) {
                result.add(index2.get(j));
                j++;
            }else {
                // Nothing should be here
            }

        }

        return result;
    }

    private void move_this_method_to_a_jUnit() {

        List<PostingList> i1 = new ArrayList<>();
        List<PostingList> i2 = new ArrayList<>();

        // index 1
        PostingList p2 = new PostingList(2, Arrays.asList(new Integer[]{1,2,7,8}));
        PostingList p31 = new PostingList(3, Arrays.asList(new Integer[]{5,7,9}));
        PostingList p1 = new PostingList(1, Arrays.asList(new Integer[]{1,6,13,15}));
        PostingList p9 = new PostingList(9, Arrays.asList(new Integer[]{1,6,13,25}));

        // index 2
        PostingList p32 = new PostingList(3, Arrays.asList(new Integer[]{17,19,21,33}));
        PostingList p5 = new PostingList(5, Arrays.asList(new Integer[]{24,28,29,100}));
        PostingList p7 = new PostingList(7, Arrays.asList(new Integer[]{16,17,18,19}));


        i1.add(p2);
        i1.add(p31);
        i1.add(p1);
        i1.add(p9);

        i2.add(p32);
        i2.add(p5);
        i2.add(p7);

        List<PostingList> result = IndexUtils.mergeInvertedIndexes(i1, i2);
        // TBD: add some verification on the result
        // TBD: move method to a jUnit
    }

}
