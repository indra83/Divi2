package co.in.divi.model;

public class ClassMembers {

	public ClassMember[]	members;

	public static class ClassMember {
		public static final String	ROLE_TEACHER	= "teacher";
		public static final String	ROLE_TESTER		= "tester";

		public String				uid;
		public String				name;
		public String				role;
		public String				profilePic;
	}
}
