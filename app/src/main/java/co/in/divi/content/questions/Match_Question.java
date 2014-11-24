package co.in.divi.content.questions;

import java.util.ArrayList;

import android.text.Spanned;

public class Match_Question extends BaseQuestion {
	public Spanned			questionHTML;
	public ArrayList<Match>	matches;

	public class Match {
		public String	id;
		public Spanned	leftHTML;
		public Spanned	rightHTML;
	}

}
