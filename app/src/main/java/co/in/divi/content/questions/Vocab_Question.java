package co.in.divi.content.questions;

import java.util.ArrayList;

import android.text.Spanned;

public class Vocab_Question {
	public String				id;
	public String				title;
	public Spanned				questionHTML;
	public ArrayList<Statement>	statements;

	public class Statement {
		public Spanned	statementHTML;
		public boolean	isTrue;
	}
}
