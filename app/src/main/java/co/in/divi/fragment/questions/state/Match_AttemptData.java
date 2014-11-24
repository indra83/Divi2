package co.in.divi.fragment.questions.state;

public class Match_AttemptData {
	public Integer[]	solvedMatches;

	public Match[]		attemptedMatches;	// used in test mode

	public static class Match {
		public int	left;
		public int	right;
	}
}
