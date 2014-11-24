package co.in.divi.content.questions;

import java.util.ArrayList;

import android.text.Spanned;

public class MCQ_Question extends BaseQuestion {
	public Spanned				questionHTML;
	public ArrayList<Option>	options;

	public class Option {
		public String	id;
		public Spanned	optionHTML;
		public boolean	isAnswer;
	}
}
