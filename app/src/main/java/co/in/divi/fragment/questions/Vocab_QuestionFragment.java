package co.in.divi.fragment.questions;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.StackView;
import android.widget.TextView;
import co.in.divi.R;
import co.in.divi.content.questions.Vocab_Question;
import co.in.divi.content.questions.Vocab_Question.Statement;
import co.in.divi.content.questions.Vocab_QuestionXmlParser;
import co.in.divi.db.model.Attempt;

public class Vocab_QuestionFragment extends BaseQuestionFragment {

	TextView	title, questionTV;
	StackView	stack;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.q_fragment_vocab, container, false);
		title = (TextView) rootView.findViewById(R.id.title);
		questionTV = (TextView) rootView.findViewById(R.id.questionTV);
		stack = (StackView) rootView.findViewById(R.id.stack);
		return rootView;
	}

	@Override
	void loadAttempt(Attempt attempt) {
		// TODO Auto-generated method stub

	}

	@Override
	Object getQuestionData() {
		return new Vocab_QuestionXmlParser().getQuestionFromXml(questionXmlFile, ImageGetter);
	}

	@Override
	void fillUI(Object questionData) {
		Vocab_Question question = (Vocab_Question) questionData;
		title.setText(question.title);
		questionTV.setText(question.questionHTML);
		stack.setAdapter(new StatementAdapter(activity, question.statements));
	}

	public class StatementAdapter extends BaseAdapter {
		private Context					contxt;
		private ArrayList<Statement>	statements;

		public StatementAdapter(Context c, ArrayList<Statement> statements) {
			contxt = c;
			this.statements = statements;
		}

		public int getCount() {
			return statements.size();
		}

		public Object getItem(int position) {
			return statements.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				LayoutInflater vi = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.q_item_vocab, null, false);
			}
			TextView tv = (TextView) view.findViewById(R.id.question);

			Statement statement = (Statement) getItem(position);
			tv.setText(statement.statementHTML);
			return view;
		}
	}
}