package co.in.divi.fragment.questions;

import android.widget.Toast;
import co.in.divi.db.model.Attempt;

public class Error_QuestionFragment extends BaseQuestionFragment {

	@Override
	Object getQuestionData() {
		return null;
	}

	@Override
	void fillUI(Object questionData) {
		Toast.makeText(activity, "Error occured loading question...", Toast.LENGTH_LONG).show();
	}

	@Override
	void loadAttempt(Attempt attempt) {
		// TODO Auto-generated method stub
		
	}
}
