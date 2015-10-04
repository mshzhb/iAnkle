package edu.utoronto.cimsah.myankle;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;

public class FragmentExerciseInstruction extends Fragment implements OnClickListener {
	
	public static final String TAG = FragmentExerciseInstruction.class.getSimpleName();
	public static final String ARG_EXERCISE_ID = "exercise_id"; 
	
	private TextView mTextName;
	private TextView mTextEquipment;
	private TextView mTextDifficulty;
	private TextView mTextExplanation;
	private ImageView mImageIcon;
	private Button mButtonStart;
	
	private DatabaseHelper mDatabaseHelper = null;
	private int mExerciseId = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDatabaseHelper = new DatabaseHelper(getActivity());
	}
	
	// instantiate FragmentExerciseInstruction
	public static FragmentExerciseInstruction newInstance(int exercise_id){
		
		// create fragment
		FragmentExerciseInstruction myFragment = new FragmentExerciseInstruction();
		
		// bundle arguments
		Bundle args = new Bundle();		
		args.putInt(ARG_EXERCISE_ID, exercise_id);		
		
		// set arguments		
		myFragment.setArguments(args);
		
		// return fragment
	    return myFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_exercise_instructions, container, false);
		mButtonStart = (Button) view.findViewById(R.id.fragment_exercise_instructions_button_start);
        mButtonStart.setOnClickListener(this);
 		return view;
	}
	  
	@Override
	public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);
		
		// Use exercise_id to find the corresponding exercise details
		mExerciseId = getArguments().getInt(ARG_EXERCISE_ID, -1);
		
		Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
			"SELECT name, instruction, picture, equipment, eyeState, difficulty FROM exercises WHERE _id = " + (mExerciseId), null);
		cur.moveToFirst();

		// Load views
		mTextName = (TextView) getView().findViewById(R.id.fragment_exercise_instructions_textview_name);
		mTextName.setText(cur.getString(0));
		
		mTextEquipment = (TextView) getView().findViewById(R.id.fragment_exercise_instructions_textview_equipment);
		mTextEquipment.setText(cur.getString(3) + " - Eyes " + cur.getString(4));
		
		mTextDifficulty = (TextView) getView().findViewById(R.id.fragment_exercise_instructions_textview_difficulty);
		mTextDifficulty.setText("Difficulty : " + cur.getString(5));

		mTextExplanation = (TextView) getView().findViewById(R.id.fragment_exercise_instructions_textview_explination);
		mTextExplanation.setText(cur.getString(1));

		mImageIcon = (ImageView) getView().findViewById(R.id.fragment_exercise_instructions_imageview_icon);
		int id = getStringIdentifier(getActivity(), cur.getString(2));
		mImageIcon.setImageResource(id);
		
		cur.close();
	}

	private int getStringIdentifier(Context context, String name) {
		return context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// set the action bar parameters
		getActivity().getActionBar().setTitle("Instructions");
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		
		// debug messages
		FragmentManager fm = getActivity().getSupportFragmentManager();
		if(BuildConfig.DEBUG) {
			
			Log.i(TAG, "The exercise ID is " + mExerciseId + " & number of"
				+ " backstack entries are " + fm.getBackStackEntryCount());
		
			for(int i=0; i < fm.getBackStackEntryCount(); i++) {
				Log.d(TAG, fm.getBackStackEntryAt(i).getName());
			}
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mDatabaseHelper.close();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
        	
		case R.id.fragment_exercise_instructions_button_start:
			if (BuildConfig.DEBUG) {
				// do something for a debug build
				Toast bread = Toast.makeText(getActivity(), "measure button click", Toast.LENGTH_SHORT);
				bread.show();
			}
			// create a new instance of FragmentExerciseMeasureRecyclerView replace the fragment in the placeholder
			Fragment newFragment = FragmentExerciseMeasureRecyclerView.newInstance(mExerciseId);
			String nextFragmentTag = FragmentExerciseMeasureRecyclerView.TAG;
			FragmentHelper.swapFragments(getActivity().getSupportFragmentManager(), 
					R.id.activity_measure_container, newFragment,
					false, true, TAG, nextFragmentTag);
			
        	break;
        	
		default: break;
		}
	}
}