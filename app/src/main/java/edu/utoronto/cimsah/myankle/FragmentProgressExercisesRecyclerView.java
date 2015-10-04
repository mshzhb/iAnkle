package edu.utoronto.cimsah.myankle;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;

public class FragmentProgressExercisesRecyclerView extends Fragment implements ListAdapterExercisesRecyclerView.ViewHolder.OnRowClickedListener {
    public static final String TAG = FragmentProgressExercisesRecyclerView.class.getSimpleName();

    private DatabaseHelper mDatabaseHelper = null;
    private ListAdapterExercisesRecyclerView mAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<ListAdapterExercisesRecyclerView.Exercise> mExercisesList;

    // current state of the eye-state image-button
    private String mCurrentEyeState = null;

    private Toast mToast = null;

    public static FragmentProgressExercisesRecyclerView newInstance() {
        FragmentProgressExercisesRecyclerView myFragment = new FragmentProgressExercisesRecyclerView();

        Bundle args = new Bundle();
        myFragment.setArguments(args);

        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabaseHelper = new DatabaseHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the fragment's layout file
        View view = inflater.inflate(R.layout.fragment_exercises_recycler_view, container, false);

		/* get the most recent eye-state from the preferences. note: in case the 
		 * fragment is being created for the first time, the string is guaranteed
		 * to be null */
        mCurrentEyeState = PrefUtils.getStringPreference(getActivity(),
                PrefUtils.EYE_STATE_KEY);

        // get a reference to the existing instance of FragmentExerciseModeSelection
        Fragment previousFragment = getChildFragmentManager().findFragmentById(
                R.id.fragment_exercises_mode_selection_container);

        boolean replaceExistingFragment = false;

        // if it exists, replace the existing fragment instead of adding a new one
        if (previousFragment != null) {
            replaceExistingFragment = true;
        }

        // create a new instance of FragmentExerciseModeSelection and add it to the placeholder
        FragmentExerciseModeSelection selectionFragment = FragmentExerciseModeSelection.
                newInstance(false, true, null, mCurrentEyeState);
		
		/* note: ChildFragmentManager is used to ensure that the life-cycle of
		 * the nested fragment (FragmentExerciseModeSelection) is synchronized
		 * with that of the parent (this) fragment, without additional checks */
        FragmentHelper.swapFragments(getChildFragmentManager(),
                R.id.fragment_exercises_mode_selection_container, selectionFragment,
                !replaceExistingFragment, false, null, FragmentExerciseModeSelection.TAG);

        // if the eye-state is null
        if (mCurrentEyeState == null) {

            // initialize the cursor for the first time
            mExercisesList = convertCursorToAl(getNewListCursor("Open"));

            // create the dialog fragment
            selectionFragment.createSelectionAlert(getActivity(), false, true);

        } else {

            // retrieve new Exercises list
            mExercisesList = convertCursorToAl(getNewListCursor(mCurrentEyeState));
        }

        //Instantiate  adapter from Exercise list, set click listener
        mAdapter = new ListAdapterExercisesRecyclerView(mExercisesList, R.layout.layout_exercise_list_row_recycler_view, getActivity());
        mAdapter.setOnRowClickedListener(this);

        //Instantiate RecyclerView, link to adapter
        mRecyclerView = (RecyclerView) (view.findViewById(R.id.rView));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);


        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

    }


    @Override
    public void onResume() {
        super.onResume();
        // Set action bar title and spinner visibility
        getActivity().getActionBar().setTitle("Progress");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDatabaseHelper.close();
    }

    /* the result of the callback initiated in FragmentExerciseDialog
     * and propagated through the fragment's host activity */
    public void updateSelection(String eyeState) {

        // get a reference to the fragment in the placeholder
        Fragment selectionFragment = getChildFragmentManager().
                findFragmentById(R.id.fragment_exercises_mode_selection_container);

        if (selectionFragment != null && selectionFragment instanceof
                FragmentExerciseModeSelection) {

            // if the parameterized eye-state is not null
            if (eyeState != null) {

                // update the list-view only if the eye-state has recently changed
                if (mCurrentEyeState == null || (mCurrentEyeState != null &&
                        !mCurrentEyeState.equals(eyeState))) {

                    // update the cursor and re-create the displayed exercises list
                    mExercisesList = convertCursorToAl(getNewListCursor(eyeState));

                    mAdapter.changeExercises(mExercisesList);
                    mAdapter.notifyDataSetChanged();

                }

                // update the currently selected eye-state
                mCurrentEyeState = eyeState;

                // update the eye-state in the shared preferences
                PrefUtils.setStringPreference(getActivity(), PrefUtils.EYE_STATE_KEY,
                        eyeState);
            }

            // update the button state of the image-button fragment
            ((FragmentExerciseModeSelection) selectionFragment).updateSelection(
                    null, mCurrentEyeState);

        } else if (BuildConfig.DEBUG) {

            // log a debug-level message indicating failure
            Log.d(TAG, FragmentExerciseModeSelection.TAG + " is null");
        }

        // cancel (override) any existing toast messages
        if (mToast != null) {
            mToast.cancel();
        }

        // create and display a toast message with the current selection
        mToast = Toast.makeText(getActivity(), "Eyes " + mCurrentEyeState,
                Toast.LENGTH_SHORT);

        mToast.show();
    }

    /**
     * Converts a cursor from Exercise database into an ArrayList of exercise objects
     * Use changeExercises in Adapter class to update adapter
     * Cursor will be closed when done
     *
     * @param cursor Cursor to be parsed (use getNewListCursor)
     * @return ArrayList with Exercise objects
     */
    private ArrayList<ListAdapterExercisesRecyclerView.Exercise> convertCursorToAl(Cursor cursor) {
        ArrayList<ListAdapterExercisesRecyclerView.Exercise> exercises = new ArrayList<>();

        if (cursor != null) {
            //Iterates through  rows available to cursor
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String photoId = cursor.getString(1);
                String name = cursor.getString(2);
                String equipment = cursor.getString(3);
                String eyeState = cursor.getString(4);
                String difficulty = cursor.getString(5);

                //Creates new Exercise and adds onto list of Exercises
                exercises.add(new ListAdapterExercisesRecyclerView.Exercise(name, id, equipment, eyeState, difficulty, photoId));
            }
            cursor.close();
        }

        return exercises;
    }

    /* gets exercise-list cursor with the exercises corresponding to the
     * parameterized eye-state */
    private Cursor getNewListCursor(String eyeState) {
        Cursor cursor = null;
        if (eyeState != null) {

            // update the SQLite cursor associated with the list-adapter
            cursor = mDatabaseHelper.getReadableDatabase().rawQuery(
                    "SELECT _id, picture, name, equipment, eyeState, difficulty " +
                            " FROM exercises WHERE eyeState = '" + eyeState + "' " +
                            " ORDER BY position", null);
        }
        return cursor;
    }

    @Override
    public void onSelect(View itemView) {
        // Get exercise_id
        int cardPosition = mRecyclerView.getChildLayoutPosition(itemView);
        int exercise_id = mExercisesList.get(cardPosition).getId();

        // instantiate FragmentProgressTable with the selected exercise_id, adding
        // the current fragment to the back-stack
        Fragment newFragment = FragmentProgressGraphPager.newInstance(exercise_id);
        String nextFragmentTag = FragmentProgressGraphPager.TAG;

        FragmentHelper.swapFragments(getActivity().getSupportFragmentManager(),
                R.id.activity_progress_container, newFragment,
                false, true, TAG, nextFragmentTag);
    }
}