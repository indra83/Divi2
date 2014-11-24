package co.in.divi.progress;

public class WeeklyTimeReport {

	public long		weekBeginTimestamp;
	public int		totalTime;

	public BookTime	bookTimes[];
	public DayTime	dayTimes[];

	public static class BookTime {
		public String	bookId;
		public int		timeSpent;
	}

	public static class DayTime {
		public int	dayOfWeek;

		public int	learnTime;
		public int	assessmentTime;
		public int	videoTime;
	}
}
