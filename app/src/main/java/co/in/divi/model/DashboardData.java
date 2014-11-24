package co.in.divi.model;

public class DashboardData {

	public StudentScore[]	scoresByStudent;
	public QuestionScore[]	scoresByQuestion;

	public static class StudentScore {
		public String	id;
		public int		points;
		public int		accuracy;
		public String	userId;
	}

	public static class QuestionScore {
		public String	id;
		public int		points;
		public int		accuracy;
		public String	questionId;
	}

}
