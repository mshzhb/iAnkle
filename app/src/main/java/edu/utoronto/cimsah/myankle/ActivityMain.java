package edu.utoronto.cimsah.myankle;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.mbientlab.metawear.api.GATT;

import java.util.UUID;

import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.Threads.PipelineThread;
import edu.utoronto.cimsah.myankle.FragmentScannerDialog.CustomDialogFinishListener;

public class ActivityMain extends FragmentActivity implements CustomDialogFinishListener {
	
	private static final String TAG = ActivityMain.class.getSimpleName();
	
	// the request codes with which to launch child activities
	public static final int CALIBRATION_REQUEST_CODE = 1;
	public static final int PROFILE_REQUEST_CODE = 2;
	public static final int TUTORIAL_REQUEST_CODE = 3;
	
	// custom return codes (which the children return to the caller)
	public static final int RESULT_PROFILE_DELETED = RESULT_FIRST_USER + 1;
	
	public static PipelineThread mPipelineThread;
	
	private DatabaseHelper mDatabaseHelper = null;
	private int mUserId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mDatabaseHelper = new DatabaseHelper(this);
		mUserId = PrefUtils.getIntPreference(this, PrefUtils.LOCAL_USER_ID);
		
		// Create and launch a thread which consume issued jobs sequentially off of the main UI thread
        mPipelineThread = new PipelineThread();
        mPipelineThread.start();
		
		// Start FragmentTitle
	    FragmentHelper.swapFragments(getSupportFragmentManager(),
				R.id.activity_main_container, FragmentTitle.newInstance(),
				true, false, null, FragmentTitle.TAG);
	    
	    
	    // if the user hasn't consented yet make them do the full tutorial
		Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
				"SELECT consent FROM users WHERE _id = " + mUserId, null);
		cur.moveToFirst();
	   if(cur.getInt(0) == -1){
		   // launch an instance of the tutorial in "full" mode
		   Intent intent = new Intent(this, ActivityTutorial.class);
		   intent.putExtra(ActivityTutorial.ARG_TUTORIAL_TYPE,
				ActivityTutorial.Tutorial_Type.FULL);
		   
		   startActivityForResult(intent, TUTORIAL_REQUEST_CODE);
	   }
	}
	
	@Override
	  public void onStart() {
	    super.onStart();

	    // enable google analytics
	    EasyTracker.getInstance(this).activityStart(this);  // Add this method.
	  }
	
	@Override 
	public boolean onCreateOptionsMenu(Menu menu) {		
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override
	public void onStop() {
		super.onStop();

		// disable google analytics
	    EasyTracker.getInstance(this).activityStop(this);  // Add this method.
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPipelineThread.requestStop();
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		// link the layout objects to the corresponding menu-items
		MenuItem menuItemResearch = menu.findItem(R.id.menu_research);
		
		// if the application is launched in debug mode
		if(BuildConfig.DEBUG) {
			
			// enable the 'Mass Upload' menu-item (not visible by default)
			menuItemResearch.setVisible(true);
			menuItemResearch.setEnabled(true);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if(BuildConfig.DEBUG) Log.i(TAG,(String) item.getTitle());
		switch(item.getItemId())
		{
				
			case R.id.menu_logout:
				
				// create an intent to launch a new instance of ActivityLogin
				Intent loginIntent = new Intent(this, ActivityLogin.class);
				startActivity(loginIntent);
				
				// close the current activity
				this.finish();
				break;
				
			case R.id.menu_profile:
				
				// create an intent to launch a new instance of ActivityProfile
				// in 'Update Profile' mode
				Intent profileIntent = new Intent(this, ActivityProfile.class);
				profileIntent.putExtra(ActivityProfile.ARG_PROFILE_MODE,
						ActivityProfile.UPDATE_MODE);
				
				// listen for a return call, in case the user deletes the profile
				startActivityForResult(profileIntent, PROFILE_REQUEST_CODE);
				break;
				
			case R.id.menu_calibration:
				
				// create an intent to launch a new instance of ActivityCalibration
				Intent calibrationIntent = new Intent(this, ActivityCalibration.class);
				startActivityForResult(calibrationIntent, CALIBRATION_REQUEST_CODE);
				break;
				
			case R.id.menu_settings:
				Intent i = new Intent(this, ActivitySettings.class);
				startActivity(i);
				break;
				
			case R.id.menu_info:
				Intent j = new Intent(this, ActivityInformation.class);
				startActivity(j);
				break;
				
			case R.id.menu_server_id:
				int serverId = PrefUtils.getIntPreference(this, PrefUtils.SERVER_ID);
				Toast toast = Toast.makeText(this, "server ID = " + serverId , Toast.LENGTH_LONG);
				toast.show();
				break;
				
			case R.id.menu_research:
				Intent l = new Intent(this, ActivityResearch.class);
				this.startActivity(l);				
				break;
				
			case R.id.menu_connect:
				
				// Fetch a reference to the Fragment Manager
				FragmentManager manager = getSupportFragmentManager();
				FragmentTransaction transaction = manager.beginTransaction();
				//TODO: Add TI UUID
				// Create a new instance of FragmentScannerDialog
				DialogFragment dialogFragment = FragmentScannerDialog.newInstance(
						new UUID[] {GATT.GATTService.METAWEAR.uuid()}, true);
				
				// Remove the previous instance of the fragment if it exists
				Fragment previousFragment = manager.findFragmentByTag(FragmentScannerDialog.TAG);
				if(previousFragment != null) transaction.remove(previousFragment);
				
				// Set the dialog parameters and display the fragment dialog
				dialogFragment.setCancelable(false);
				dialogFragment.show(transaction, FragmentScannerDialog.TAG);
				break;
			
			default: // shouldn't reach this
				Toast.makeText(this, "Illegal navigation", Toast.LENGTH_LONG).show();
				return false;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// called when the child activity finishes with a response to 
	// the parent's intent call
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
		super.onActivityResult(requestCode, resultCode, returnedIntent);
		
		if(requestCode == CALIBRATION_REQUEST_CODE) {
			
			// calibration was successful
			if(resultCode == RESULT_OK) {
				
				// display a toast indicating calibration was successful
				Toast.makeText(this, R.string.calibration_successful_toast, 
						Toast.LENGTH_SHORT).show();
				
				// Clear the ankle-side and eye-state preferences from previous sessions
    			PrefUtils.setStringPreference(this, PrefUtils.ANKLE_SIDE_KEY, null);
    			PrefUtils.setStringPreference(this, PrefUtils.EYE_STATE_KEY, null);
				
				// launch ActivityMeasure
				Intent measureIntent = new Intent(this, ActivityMeasure.class);
				startActivity(measureIntent);
				
			// calibration failed
			} else if(resultCode == RESULT_CANCELED) {
				
				// display a toast indicating calibration failure
				Toast.makeText(this, R.string.calibration_failed_toast, 
						Toast.LENGTH_SHORT).show();
			}
			
		} else if(requestCode == PROFILE_REQUEST_CODE) {
			
			// the profile was deleted
			if(resultCode == RESULT_PROFILE_DELETED) {
				
				// debug message
				if(BuildConfig.DEBUG) Log.d(TAG, "User profile deleted");
				
				// launch a new instance of ActivityLogin
				Intent loginIntent = new Intent(this, ActivityLogin.class);
				startActivity(loginIntent);
				
				// finish the current instance of ActivityMain
				this.finish();
			}
		} else if (requestCode == TUTORIAL_REQUEST_CODE){

			// the full tutorial has been complete. mark this user as having consented
			if(resultCode == RESULT_OK){

				int userId = PrefUtils.getIntPreference(this, PrefUtils.LOCAL_USER_ID);
				
				ContentValues args = new ContentValues();
				args.put("consent", 1);
				
				// update the user consent value
				mDatabaseHelper.getWritableDatabase()
						.update("users", args, "_id = " + userId, null);

			} else if (resultCode == RESULT_CANCELED){
				// don't let the user use the app
				this.finish();	
			}
		}
	}
	
	@Override
	public void onFinish(int id, Bundle bundle) {
		
		switch (id) {
			
			// The call-back was initiated by FragmentScannerDialog
			case FragmentScannerDialog.ID:

				// Display a toast indicating the selected device
				String deviceName = bundle.getString(FragmentScannerDialog.RETURN_DEVICE_NAME);
				Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
						
				break;
		}
	}
}