package co.in.divi.model;


public class LectureInstruction {

	public String			lectureStatus;

	public Instruction[]	instructions;

	public static class Instruction {
		public String	id;
		public String	data;
		public long		timestamp;
	}
}
