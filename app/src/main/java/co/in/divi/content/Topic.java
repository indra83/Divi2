package co.in.divi.content;

public class Topic {

	public static final String	TAG	= "divi.Topic";

	public String				title;
	public String				pagePath;			// each topic corresponds to one HTML page
	public Section[]			sections;
	public Image[]				images;
	public Video[]				videos;
	public Audio[]				audios;
	public ImageSet[]			imageSets;
	public VM[]					vms;

	public static class Section {
		public String	id;
		public String	title;
	}

	public static class Image {
		public String	id;
		public String	src;
		public String	title;
		public String	desc;
		public boolean	allowFullscreen;
		public boolean	hideInToc;
	}

	public static class Video {
		public String	id;
		public String	src;
		public String	thumb;
		public String	title;
		public String	desc;
		public String	youtubeId;
		public boolean	hideInToc;
	}

	public static class Audio {
		public String	id;
		public String	src;
		public String	title;
		public String	desc;
		public boolean	hideInToc;
	}

	public static class VM {
		public String	id;
		public String	src;
		public String	appPackage;
		public int		appVersionCode;
		public String	appActivityName;
		public String	thumb;
		public String	title;
		public String	desc;
		public boolean	hideInToc;
		public boolean	showInApps;
	}

	public static class ImageSet {
		public String		id;
		public String		title;
		public ImageItem[]	imagesItems;

		public static class ImageItem {
			public String	src;
			public String	desc;
		}
	}
}
