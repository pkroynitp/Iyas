package qa.qcri.qf.cQAdemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import qa.qf.qcri.cqa.CQAcomment;
import qa.qf.qcri.cqa.CQAinstance;

public class OutputVisualization {

  private final int MAX_TO_PRINT = 5;
	private List<CQAinstance> threadList; 
	
	public OutputVisualization(List<CQAinstance> threads) {
		this.threadList = threads;
	}
	
	public void print() {
		printOnCommandLine();
	}
	
	/**
	 * 
	 */
	public void printOnCommandLine() {
		List<CQAcomment> clist;
		int counter;
		for (CQAinstance thread : threadList) {
			clist = thread.getComments();
			Collections.sort(clist);
<<<<<<< HEAD
			System.out.println("\n****\nThread: " + thread.getQuestion().getWholeText());
			counter =0;
=======
			System.out.println("\n****\nThread: " 
					+ thread.getQuestion().getWholeText());
>>>>>>> b5ee9ac6b3a574cb444e5b48d2fd6bcd96302a1f
			for (CQAcomment c : clist) {
			  if (++counter > MAX_TO_PRINT) {
			    break;
			  }
				System.out.println(c.getScore() + ": " + c.getWholeText());
			}
			System.out.println("****\n");
		}
	}
	
}
