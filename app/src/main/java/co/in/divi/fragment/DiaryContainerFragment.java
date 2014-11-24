package co.in.divi.fragment;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import co.in.divi.BaseActivity;
import co.in.divi.DiaryManager;
import co.in.divi.LectureSessionProvider;
import co.in.divi.R;
import co.in.divi.UserSessionProvider;
import co.in.divi.ui.UserDataPopup;
import co.in.divi.util.LogConfig;
import co.in.divi.util.Week;

public class DiaryContainerFragment extends Fragment {

	private static final String		TAG						= DiaryContainerFragment.class.getName();
	private static final String		DIARY_COMPOSE_FRAGMENT	= "DIARY_COMPOSE_FRAGMENT";

	private UserSessionProvider		userSessionProvider;
	private LectureSessionProvider	lectureSessionProvider;
	private DiaryManager			diaryManager;

	private ViewPager				viewPager;
	private Button					newEntryButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		lectureSessionProvider = LectureSessionProvider.getInstance(getActivity());
		userSessionProvider = UserSessionProvider.getInstance(getActivity());
		diaryManager = DiaryManager.getInstance(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_diary_container, container, false);
		rootView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((BaseActivity) getActivity()).closeDiary();
			}
		});
		viewPager = (ViewPager) rootView.findViewById(R.id.diaryPager);
		newEntryButton = (Button) rootView.findViewById(R.id.newEntryButton);
		return rootView;
	}

	@Override
	public void onStart() {
		super.onStart();
		if (diaryManager.getCurrentEntry() != null) {
			FragmentManager fm = getFragmentManager();
			DiaryEntryComposeFragment diaryComposeFragment = new DiaryEntryComposeFragment();
			diaryComposeFragment.show(fm, DIARY_COMPOSE_FRAGMENT);
			((BaseActivity) getActivity()).closeDiary();
		}
		((MarginLayoutParams) getView().getLayoutParams()).setMargins(0, (int) getResources().getDimensionPixelSize(R.dimen.header_height),
				0, 0);
		getView().requestLayout();
		DiaryPagerAdapter adapter = new DiaryPagerAdapter(getFragmentManager(), diaryManager.getAllWeeks());
		viewPager.setAdapter(adapter);
		viewPager.setCurrentItem(adapter.getCount() - 1);
		newEntryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				diaryManager.startNewEntry();
				FragmentManager fm = getFragmentManager();
				DiaryEntryComposeFragment diaryComposeFragment = new DiaryEntryComposeFragment();
				diaryComposeFragment.show(fm, DIARY_COMPOSE_FRAGMENT);
				((BaseActivity) getActivity()).closeDiary();
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();

	}

	class DiaryPagerAdapter extends FragmentStatePagerAdapter {
		ArrayList<Week>	weeks;

		public DiaryPagerAdapter(FragmentManager fm, ArrayList<Week> weeks) {
			super(fm);
			this.weeks = weeks;
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "got weeks:" + weeks.size());
		}

		@Override
		public Fragment getItem(int position) {
			if (LogConfig.DEBUG_ACTIVITIES)
				Log.d(TAG, "returning frag:" + position);
			ProgressFragment f = ProgressFragment.newInstance(weeks.get(position).weekBeginTimestamp);
			return f;
		}

		@Override
		public int getCount() {
			return weeks.size();
		}

		@Override
		public int getItemPosition(Object object) {
			// force refresh on notify dataset change.
			return POSITION_NONE;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			// Log.d(TAG, "setting primary item, position:	" + position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return weeks.get(position).getDisplayString();
		}
	}

}
