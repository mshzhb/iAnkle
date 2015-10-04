package edu.utoronto.cimsah.myankle;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import edu.utoronto.cimsah.myankle.FragmentExerciseDialog.CustomDialogFinishListener;
import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.Threads.PipelineThread;

public class ActivityMeasure extends FragmentActivity implements CustomDialogFinishListener {

    private static final String TAG = ActivityMeasure.class.getSimpleName();
    public static PipelineThread mPipelineThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        // hide the status (notification) bar
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        // create and maintain a new instance of PipelineThread to be used later
        mPipelineThread = new PipelineThread();
        mPipelineThread.start();


        // create a new instance of FragmentExercises and add it to the placeholder
        FragmentExercisesListFragment newFragment = FragmentExercisesListFragment.newInstance();
        FragmentHelper.swapFragments(getSupportFragmentManager(),
                R.id.activity_measure_container, newFragment,
                true, false, null, FragmentExercisesListFragment.TAG);

        //TODO: Change call back to RecyclerView once loading occurs smooothly
        /**
         FragmentExercisesRecyclerView newFragment = FragmentExercisesRecyclerView.newInstance();
         FragmentHelper.swapFragments(getSupportFragmentManager(),
         R.id.activity_measure_container, newFragment,
         true, false, null, FragmentExercisesRecyclerView.TAG);
         **/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // release the PipelineThread object
        mPipelineThread.requestStop();
    }

    @Override
    public void onBackPressed() {

        // if the back button is pressed while on the 'Measure' fragment, do nothing
        if (getCurrentFragmentTag() == FragmentExerciseMeasureRecyclerView.TAG) {

            // create a debug message
            if (BuildConfig.DEBUG) Log.d(TAG, "Blocked back-press!");

            // if the back button is pressed while on the 'Result' fragment
        } else if (getCurrentFragmentTag() == FragmentExerciseResults.TAG) {

            // navigate up the backstack to the 'Exercises' fragment
            getSupportFragmentManager().popBackStackImmediate(null,
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);

            // else, use the default functionality
        } else {

            super.onBackPressed();
        }
    }

    // if a fragment is currently being rendered in the placeholder, return its tag
    private String getCurrentFragmentTag() {

        String currFragmentTag = null;

        // find the fragment currently hosted in the fragment placeholder
        Fragment currFragment = getSupportFragmentManager()
                .findFragmentById(R.id.activity_measure_container);

        // if the fragment isn't null, return it's associated tag
        if (currFragment != null) {

            currFragmentTag = currFragment.getTag();

        } else {

            // create a debug message indicating no fragment is currently hosted
            if (BuildConfig.DEBUG) Log.d(TAG, "Current fragment is NULL");
        }

        return currFragmentTag;
    }

    // creating and manipulating menu elements
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.measure_menu, menu);

        return true;
    }

    // functionality for menu button-press
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // when the 'Up' button is pressed, use the default back-button press functionality
            case android.R.id.home:

                onBackPressed();

                break;

            // when the 'Settings' menu-button is selected
            case R.id.measure_menu_settings:

                Intent settingsIntent = new Intent(this, ActivitySettings.class);
                startActivity(settingsIntent);

                break;

            // when the 'Show User ID' menu-button is pressed
            case R.id.measure_menu_server_id:

                int serverId = PrefUtils.getIntPreference(this, PrefUtils.SERVER_ID);
                Toast toast = Toast.makeText(this, "Server ID = " + serverId, Toast.LENGTH_SHORT);
                toast.show();
                break;

            // shouldn't reach here
            default:
                if (BuildConfig.DEBUG) Log.d(TAG, "Illegal menu navigation!");
                return false;
        }

        return super.onOptionsItemSelected(item);
    }

    // the callback initiated when a dialog fragment is closed
    @Override
    public void onFinish(int id, Bundle bundle) {

        switch (id) {

            // the call-back was initiated by FragmentExerciseDialog
            case FragmentExerciseDialog.ID:

                // retrieve the bundled argument strings
                String ankleSide = bundle.getString(FragmentExerciseDialog.RETURN_ANKLE_SIDE);
                String eyeState = bundle.getString(FragmentExerciseDialog.RETURN_EYE_STATE);

                // get a reference to the currently displayed FragmentExercises instance
                Fragment exerciseFragment = getSupportFragmentManager().
                        findFragmentById(R.id.activity_measure_container);

                // sanity check
                if (exerciseFragment != null && exerciseFragment
                        instanceof FragmentExercisesRecyclerView) {


                    // initiate the call-back method in the exercises fragment
                    ((FragmentExercisesRecyclerView) exerciseFragment).
                            updateSelection(ankleSide, eyeState);

                }

                else if (exerciseFragment != null && exerciseFragment instanceof FragmentExercisesListFragment) {
                    // initiate the call-back method in the exercises fragment
                    ((FragmentExercisesListFragment) exerciseFragment).
                            updateSelection(ankleSide, eyeState);
                } else {

                    // display a toast indicating failure to update selection
                    Toast.makeText(this, "Selection not updated!", Toast.LENGTH_SHORT).show();

                    // display an error-level message
                    if (BuildConfig.DEBUG) Log.d(TAG, "FragmentExercisesRecyclerView is null");
                }

                break;
        }
    }
}
