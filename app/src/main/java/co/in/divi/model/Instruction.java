package co.in.divi.model;

/**
 * JSON data in instruction! (different from LectureInstruction.Instruction)
 * 
 * @author indraneel
 * 
 */
public class Instruction {
	public static final int	INSTRUCTION_TYPE_NAVIGATE			= 0;
	public static final int	INSTRUCTION_TYPE_BLACKOUT			= 1;
	public static final int	INSTRUCTION_TYPE_END_LECTURE		= 2;

	public static final int	INSTRUCTION_TYPE_FOLLOW_ME			= 11;

	public static final int	INSTRUCTION_TYPE_NAVIGATE_EXTERNAL	= 21;

	public int				type;
	public String			location;
	public String[]			breadcrumb;
	public boolean			syncCommand;
	public boolean			followMe;

	// not a clean way.. we need to know if its VM (for dashboard)
	public boolean			isVM;
}
