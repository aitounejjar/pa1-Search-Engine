package cs276.util;

import java.util.Comparator;

/**
 * Custom comparator that sorts pairs of (termId, docId).
 * Pairs are first sorted by termId, then by docId
 */

public class TermIdDocIdPairComparator implements Comparator<Pair<Integer, Integer>> {

    @Override
    public int compare(Pair<Integer, Integer> pair1, Pair<Integer, Integer> pair2) {

        // term ids
        int term1 = pair1.getFirst();
        int term2 = pair2.getFirst();

        // document ids
        int doc1 = pair1.getSecond();
        int doc2 = pair2.getSecond();

        int result = 0;

        if (term1 > term2) {
            result = +1;
        } else if (term1 < term2) {
            result = -1;
        } else {
            // term ids are equals, so we compare based on document ids
            if (doc1 > doc2) {
                result = 1;
            } else if (doc1 < doc2) {
                result = -1;
            } else {
                result = 0;
            }
        }

        return result;
    }
}
