package co.in.divi.content.questions;

import java.util.ArrayList;

import android.text.Spanned;

public class Label_Question extends BaseQuestion {
	public String			title;
	public String			imageFile;
	public ArrayList<Label>	labels;

	public class Label {
		public String	id;
		public Spanned	labelText;
		public double	x;
		public double	y;
	}
}
