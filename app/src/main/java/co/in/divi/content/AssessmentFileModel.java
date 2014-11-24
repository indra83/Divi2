package co.in.divi.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import co.in.divi.db.model.Attempt;

/*
 * Data model class for assessment.json
 * Doubles as the model for Assessment Node too!
 */
public class AssessmentFileModel {

	// public static final String TYPE_EXERCISE = "exercise";
	public static final String	TYPE_ASSIGNMENT	= "assignment";
	public static final String	TYPE_QUIZ		= "quiz";
	public static final String	TYPE_TEST		= "test";

	public String				assessmentId;					// has to be populated!!!
	public long					randomizerSeed	= 0;
	// public String id;

	public String				name;
	public String				type;
	public String				time;
	public String				difficulty;
	public String[]				recommendedReadings;

	public Question[]			questions;

	// computed for easy insertion to db
	public String				content;

	public static class Question {
		public String			id;
		public String			name;
		public String			type;
		public int				points	= 1;

		public String			text;

		public QuestionMetadata	metadata;
	}

	// Scoring related helpers
	public AssessmentScorecard getScorecard(HashMap<String, Attempt> attempts) {
		AssessmentScorecard scorecard = new AssessmentScorecard();
		scorecard.userPoints = 0;
		scorecard.maxPoints = 0;
		ArrayList<Double> accuracies = new ArrayList<Double>();
		for (int i = 0; i < questions.length; i++) {
			Attempt a = attempts.get(questions[i].id);
			scorecard.maxPoints += questions[i].points;
			if (a != null) {
				scorecard.userPoints += a.getCurrentPoints();
				if (a.isAttempted())
					accuracies.add(a.getCurrentAccuracy());
			}
		}
		double totalAccuracy = 0;

		if (accuracies.size() > 0) {
			for (double a : accuracies)
				totalAccuracy += a;
			scorecard.avgAccuracy = totalAccuracy / accuracies.size();// only attempted questions
		} else {
			scorecard.avgAccuracy = 0;
		}

		if (type.equalsIgnoreCase(AssessmentFileModel.TYPE_TEST))
			scorecard.diviScore = (int) ((100.0 * scorecard.userPoints) / scorecard.maxPoints);
		else
			scorecard.diviScore = (int) totalAccuracy / questions.length;

		return scorecard;
	}

	private boolean	shuffled	= false;

	public void shuffleQuestions() {
		// make sure this happens only once!!
		if (!shuffled) {
			shuffled = true;
			Random rnd = new Random(randomizerSeed);
			for (int i = questions.length - 1; i > 0; i--) {
				int index = rnd.nextInt(i + 1);
				// Simple swap
				Question a = questions[index];
				questions[index] = questions[i];
				questions[i] = a;
			}
		}
	}

	public static class AssessmentScorecard {
		public int		numQuestions;
		public int		maxPoints;
		public int		userPoints;
		public double	avgAccuracy;	// of attempted questions only
		public int		diviScore;
	}
}
