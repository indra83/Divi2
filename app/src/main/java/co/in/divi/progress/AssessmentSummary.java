package co.in.divi.progress;

import java.util.HashMap;

import co.in.divi.content.Book;
import co.in.divi.content.Node;
import co.in.divi.db.model.Attempt;
import co.in.divi.db.model.Command;

public class AssessmentSummary implements Comparable<AssessmentSummary> {
	public Book						book;
	public Node						assessmentNode;
	public Command					unlockCommand;
	public HashMap<String, Attempt>	attempts	= new HashMap<String, Attempt>();

	public int						totalPoints;
	public int						maxPoints;
	public double					avgAccuracy;

	public int						diviScore;

	@Override
	public int compareTo(AssessmentSummary another) {
		return (int) (this.unlockCommand.appliedAt / 1000 - another.unlockCommand.appliedAt / 1000);
	}
}