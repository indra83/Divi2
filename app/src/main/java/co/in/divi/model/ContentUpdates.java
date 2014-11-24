package co.in.divi.model;

public class ContentUpdates {

	public String[]	cdn;

	public Update[]	updates;

	public static class Update {
		public String	courseId;
		public String	bookId;
		public int		bookVersion;
		public int		bookFromVersion;
		public String	strategy;
		public String	status;
		public String	fileName;
		public String	webUrl;

		// Computed; additional check to see the version is higher
		public boolean	isApplicable;

		// for dropbox import (reviewers)
		public boolean	isDropboxImport	= false;
	}
}
