package co.in.divi.content;

public class QuestionMetadata {

	public enum BLOOMS {
		NONE, KNOWLEDGE, UNDERSTANDING, APPLICATION, HOTS
	}

	public int		difficulty;
	public int		languageLevel;

	public BLOOMS	blooms;

	public String[]	tagIds;
}
