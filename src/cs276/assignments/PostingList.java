package cs276.assignments;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PostingList {

	private int termId;
	private List<Integer> postings; // a list of docIDs (i.e. postings)

    //--------------------------------------------------------------------------------------------------------------
    // public constructor(s)
    //--------------------------------------------------------------------------------------------------------------

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

    public PostingList(int termId, int[] list) {
        this.termId = termId;
        this.postings = IntStream.of(list).boxed().collect(Collectors.toList());
    }

	public PostingList(int termId) {
		this.termId = termId;
		this.postings = new ArrayList<>();
	}

    //--------------------------------------------------------------------------------------------------------------
    // getter(s) and setter(s)
    //--------------------------------------------------------------------------------------------------------------

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}

    public int[] getListAsArray() {
	    return this.postings.stream().mapToInt(i->i).toArray();
    }

}
