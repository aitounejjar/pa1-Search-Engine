package cs276.assignments;

import java.util.ArrayList;
import java.util.List;

public class PostingList {

	private int termId;
	private String termStr;
	private List<Integer> postings; // a list of docIDs (i.e. postings)

	public PostingList(int termId, List<Integer> list) {
		this.termId = termId;
		this.postings = list;
	}

	public PostingList(int termId, String termStr) {
		this.termId = termId;
		this.termStr = termStr;
		this.postings = new ArrayList<Integer>();
	}

    public PostingList(int termId, String termStr, List<Integer> list) {
        this.termId = termId;
        this.termStr = termStr;
        this.postings = list;
    }

	public int getTermId() {
		return this.termId;
	}

	public List<Integer> getList() {
		return this.postings;
	}

	public String toString() {
	    return "(" + termId + ", \"" + termStr + "\") -->" + postings.toString();
    }
}
