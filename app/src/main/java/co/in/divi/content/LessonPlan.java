package co.in.divi.content;

public class LessonPlan {

	public String[]	goals;
	public Module[]	modules;

	public static final class Module {
		public String			topics;
		public int				time;
		public Quiz[]			quizzes;
		public Instruction[]	instructions;

		public static final class Quiz {
			public String	text;
			public String	src;
		}

		public static final class Instruction {
			public String		text;
			public Resource[]	resources;

			public static final class Resource {
				public String	text;
				public String	src;
			}
		}
	}

}
