package edu.utoronto.cimsah.myankle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer.AccelerometerListener;
import edu.utoronto.cimsah.myankle.Accelerometers.AccelerometerManager;
import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.DialogStyleHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.Helpers.SoundPoolHelper;

/** A class to automate calibration data collection and access internal storage.
 * @author Vivian
 */
public class FragmentCalibration extends Fragment implements AccelerometerListener {
	
	public static final String TAG = FragmentCalibration.class.getSimpleName();
	
	private static final int X = 0, Y = 1, Z = 2, X_NEG = 3, Y_NEG = 4, Z_NEG = 5, DONE = 6;
	
	// Acceleration parameters
	private final float LOWEST_ALLOWED_ACCELERATION = (float) 9.000000;
	private final float HIGHEST_ALLOWED_ACCELERATION = (float) 10.600000;
	private final long DCTIMER_DURATION_IN_MILLIS = 5000;
	private final int MAX_SAMPLE_SIZE = 200;
	
	// Accelerometer instance
	private Accelerometer mAccelerometer = null;
	
	// Layout objects
	private TextView mTextInstructions;
	private TextView mTextStatus;
	private ImageView mImageMain;
	
	private boolean mBeginCalibration;
	private int mCurrAxis;
	private ArrayList<Float> mRawAccel;
	private ArrayList<Float> mMeanAccel;
	private float mSum;

	private MyDataCollectionTimer mDataCollectionTimer;
	
	// SoundPool
	private SoundPoolHelper mSoundPoolHelper;
	
	public static FragmentCalibration newInstance() {
		FragmentCalibration myFragment = new FragmentCalibration();
		
		Bundle args = new Bundle();		
		myFragment.setArguments(args);
		
	    return myFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		
		// pause calibration while user reads calibration alert
		mBeginCalibration = false;
		
		// alert the user that calibration is needed	
		createCalibrationInstructionAlert();
		
		// Initialize accelerometer
		mAccelerometer = AccelerometerManager.get(getActivity());
		mAccelerometer.registerListenerAndConnect(this);
	
		// NOTE: Calibration is first performed on the z-axis because there is minimal user error here. 
		// The app asks the user to put the phone on a flat surface.
		mCurrAxis = Z;
				
		// Initialize variables and arrays
		mSum = 0;
		mRawAccel = new ArrayList<Float>();
		mRawAccel.clear();
		
		// Initialize meanAccel ArrayList to store the 6 means
		// - load them with 0s
		// - set them when values are found
		// - NOTE: Do NOT use meanAccel.add(...) anywhere else! Use meanAccel.set(X or Y or Z or X_NEG or Y_NEG or Z_NEG) instead!
		mMeanAccel = new ArrayList<Float>();
		mMeanAccel.add((float) 0);
		mMeanAccel.add((float) 0);
		mMeanAccel.add((float) 0);
		mMeanAccel.add((float) 0);
		mMeanAccel.add((float) 0);
		mMeanAccel.add((float) 0);
		
		// Initialize SoundPool objects
		getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC); // Allow volume to be adjusted using hardware buttons
		mSoundPoolHelper = new SoundPoolHelper(getActivity());
        
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_calibration, container, false);
		// Initialize text and image views
		mTextInstructions = (TextView)view.findViewById(R.id.fragment_calibration_textview_instructions);
		mImageMain = (ImageView)view.findViewById(R.id.fragment_calibration_imageview_main);
		mTextStatus = (TextView)view.findViewById(R.id.fragment_calibration_textview_status);
		mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_1));
		mImageMain.setImageResource(R.drawable.z);
		return (view);
	}

	@Override
	public void onAccelerometerEvent(float[] values) {		
		
		// If calibration has started
		if (mBeginCalibration) {
			
			if(mCurrAxis == Z) {
				float z = values[2];
					
				// Verify point z, store it. 
				checkAndStorePoint(z);
				
				// Done collecting z-accelerations
				if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
					mCurrAxis = X;
					// Calculate and store the z-mean
					mMeanAccel.set(Z, (float)mSum/mRawAccel.size());
					reset();
					mSoundPoolHelper.shortDing();
					// Update the view for next axis
					mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_2));
					mImageMain.setImageResource(R.drawable.x);
				}
				
			} else if(mCurrAxis == X) {
				float x = values[0];
				
				// Verify point x, store it.
				checkAndStorePoint(x);
				
				// Done collecting x-accelerations
				if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
					mCurrAxis = Y;
					// Calculate and store the x-mean
					mMeanAccel.set(X, (float)mSum/mRawAccel.size());
					// Compare the recently calculated x-mean with z-mean
					compareToZMean(mMeanAccel.get(X));
					reset();
					mSoundPoolHelper.shortDing();
					mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_3));
					mImageMain.setImageResource(R.drawable.y);
				}
				
			} else if(mCurrAxis == Y) {
				float y = values[1];
				
				// Verify point y, store it.
				checkAndStorePoint(y);
				
				// Done collecting y-accelerations
				if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
					mCurrAxis = X_NEG;
					// Calculate and store the y-Mean
					mMeanAccel.set(Y, (float)mSum/mRawAccel.size());
					// Compare the recently calculated y-mean and compare with z-mean
					compareToZMean(mMeanAccel.get(Y));
					reset();
					mSoundPoolHelper.shortDing();
					mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_4));
					mImageMain.setImageResource(R.drawable.xneg);
				}
				
			} else if(mCurrAxis == X_NEG) {
				float xneg = values[0];
				
				// Verify point xneg, store it.
				checkAndStorePoint(-1*xneg);
				
				// Done collecting xnegs
				if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
					mCurrAxis = Y_NEG;
					// Calculate and store the xneg-mean
					mMeanAccel.set(X_NEG, (float)mSum/mRawAccel.size());
					// Compare the recently calculated y-mean and compare with z-mean
					compareToZMean(mMeanAccel.get(X_NEG));
					reset();
					mSoundPoolHelper.shortDing();
					mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_5));
					mImageMain.setImageResource(R.drawable.yneg);
				}
				
			} else if(mCurrAxis == Y_NEG) {
					float yneg = values[1];
					
					// Verify point yneg, store it.
					checkAndStorePoint(-1*yneg);
					
					// Done collecting ynegs
					if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
						mCurrAxis = Z_NEG;
						// Calculate and store the yneg-mean
						mMeanAccel.set(Y_NEG, (float)mSum/mRawAccel.size());
						// Compare the recently calculated yneg-mean and compare with z-mean
						compareToZMean(mMeanAccel.get(Y_NEG));
						reset();
						mSoundPoolHelper.shortDing();
						mTextInstructions.setText(getResources().getString(R.string.calibration_instruction_6));
						mImageMain.setImageResource(R.drawable.zneg);
					}
					
			} else if(mCurrAxis == Z_NEG) {
				float zneg = values[2];
				
				// Verify point zneg, store it.
				checkAndStorePoint(-1*zneg);
				
				// Done collecting znegs
				if(mDataCollectionTimer.isFinished || mRawAccel.size() > MAX_SAMPLE_SIZE) {
					mCurrAxis = DONE;
					// Kill the timer
					mDataCollectionTimer.cancel();
					// Calculate and store the zneg-mean
					mMeanAccel.set(Z_NEG, (float)mSum/mRawAccel.size());
					// Compare the recently calculated zneg-mean and compare with z-mean
					compareToZMean(mMeanAccel.get(Z_NEG));
					mSoundPoolHelper.longPingDing();
					mTextInstructions.setText("Done");
				}
				
			// Done collecting all six means
			} else if(mCurrAxis == DONE){
				// Increment m_curAxis so onSensorChanged becomes noop (due to all the if-statements)
				mCurrAxis++;
				// Change button and text indicators
				mTextInstructions.setText("Finishing");
				
				// Write the values collected in meanAccel to internal storage
				writeCalibrationValues(getActivity(), 
						mMeanAccel.get(X), 
						mMeanAccel.get(Y),
						mMeanAccel.get(Z), 
						mMeanAccel.get(X_NEG),
						mMeanAccel.get(Y_NEG),
						mMeanAccel.get(Z_NEG));
		      
				// alert the user that calibration is complete
				createCalibrationFinishedAlert();
			}
		}
	}
	
	private void checkAndStorePoint(float point) {
		// Store point if within thresholds
		if(point > LOWEST_ALLOWED_ACCELERATION && point < HIGHEST_ALLOWED_ACCELERATION) {
			// set the status text view
			mTextStatus.setText(getResources().getString(R.string.calibration_status_good));
			mTextStatus.setTextColor(getResources().getColor(R.color.blue));
			
			// add current point to the collected points
			mRawAccel.add(point);
			mSum += point;
			
		// point has taken a strange value, clear raw accelerations and reset sum
		} else {
			
			// set the status textview
			mTextStatus.setText(getResources().getString(R.string.calibration_status_not_good));
			mTextStatus.setTextColor(getResources().getColor(R.color.skin));
			
			// clear previously collected points and reset the timer
			mDataCollectionTimer.restart();
			mRawAccel.clear();
			mSum = 0;
		}
	}
	// Compares mean to z-mean, throws a dialog to finish() if there's a large difference between them
	private void compareToZMean(float mean) {
		if(Math.abs(mean - mMeanAccel.get(Z)) > 0.1*LOWEST_ALLOWED_ACCELERATION) {					
			createCalibrationErrorAlert();		
		}		
	}
	
	private void reset() {
		// Restart timer
		mDataCollectionTimer.restart();
		// Clear sum and rawAccel ArrayList to be reused
		mSum = 0;
		mRawAccel.clear();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// disable the action bar home button (so user can't accidentally go back)
		getActivity().getActionBar().setTitle("Calibrating");
		
		mDataCollectionTimer = new MyDataCollectionTimer(DCTIMER_DURATION_IN_MILLIS, 1000);
		mDataCollectionTimer.start();
		
		mSoundPoolHelper.resume();
		mAccelerometer.start();
	}

	@Override
	public void onPause() {
		super.onPause();
		
		mAccelerometer.stop();
		if(!getActivity().isFinishing()) {
			mSoundPoolHelper.pause();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mAccelerometer.unregisterListenerAndDisconnect(this);
		mSoundPoolHelper.release();
	}
	
	// called in ActivityCalibration when the back button is pressed
	// create an alert dialog asking whether calibration should be ended
	public void onBackPressed() {
		onPause();
		
		createCalibrationExitEarlyAlert();

	}

	@Override
	public void onAccelerometerConnected(boolean isBluetoothDevice, String MAC_Address) {

		// Enable the accelerometer device to collect data
		mAccelerometer.start();
	}

	@Override
	public void onAccelerometerDisconnected() {
	}

	public class MyDataCollectionTimer extends CountDownTimer {
		public boolean isFinished;
		public MyDataCollectionTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			isFinished = false;
		}
		
		@Override
		public void onFinish() {
			isFinished = true;
		}

		@Override
		public void onTick(long millisUntilFinished) {		
		}
		
		public void restart() {
			isFinished = false;
			cancel();
			start();
		}
	}

	/**
	 * Returns whether or not the selected accelerometer device is calibrated.
	 * 
	 * @param context The activity context
	 * @return Whether the device has been calibrated
	 */
	public static boolean isCalibrated(Context context) {
		
		// Variable declaration
		boolean isCalibrated = false;
		String deviceAddress = PrefUtils.getStringPreference(context, PrefUtils.MAC_ADDRESS_KEY);
		
		/* If the device address is null (indicates the inbuilt accelerometer), 
		 * set the string to the appropriate value ("Inbuilt") */
		if(deviceAddress == null) { 
			deviceAddress = "Inbuilt"; 
		}
		
		// Query the database to check whether an entry corresponding to the device exists
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
				"SELECT * FROM calibration WHERE device = '" + deviceAddress +
						"'", null);
		
		// Check if the query returned a valid result
		if(cursor != null && cursor.moveToFirst()) {
			
			// Set the return value and close the cursor
			isCalibrated = true;
		}
		
		// Release the database objects
		if(cursor != null) cursor.close();
		databaseHelper.close();
		
		return isCalibrated;
	}

	/**
	 * Given the results of calibration (acceleration values along all axes), inserts them into
	 * the database corresponding to the currently connected device. If the device entry exists,
	 * updates the values. Else, creates a new row corresponding to the device.
	 */
	public void writeCalibrationValues(Context context, float x, float y, float z, float xneg, float yneg, float zneg) {

		// Sanity check: Ensure that the values are valid
		if(x == 0) {
			
			// Output an appropriate error message
			if(BuildConfig.DEBUG) Log.e(TAG, "Error: Collected calibration values are invalid");
			return;
		}
		
		// Variable declaration
		DatabaseHelper databaseHelper = new DatabaseHelper(context);
		String deviceAddress = PrefUtils.getStringPreference(context, PrefUtils.MAC_ADDRESS_KEY);
		
		// If the device address is null, reset the string to the appropriate value ("Inbuilt")
		if(deviceAddress == null) {
			deviceAddress = "Inbuilt";
		}
		
		// Create and populate a new ContentValues instance with the accelerometer values
		ContentValues args = new ContentValues();
		args.put("device", deviceAddress);
		args.put("x", x);
		args.put("y", y);
		args.put("z", z);
		args.put("xneg", xneg);
		args.put("yneg", yneg);
		args.put("zneg", zneg);
		
		// If the entry does not exist in the database, create a new one
		if(!isCalibrated(context)) {
			databaseHelper.getWritableDatabase().insert("calibration", null, args);
			
		// Else, update the existing entry	
		} else {
			databaseHelper.getWritableDatabase().update("calibration", args, 
					"device = '" + deviceAddress + "'", null);
		}
		
		// Release the database objects
		databaseHelper.close();
	}
	
	
	private void createCalibrationExitEarlyAlert(){
		
		AlertDialog.Builder dlgAlert= new AlertDialog.Builder(getActivity())
			.setTitle(getResources().getString(R.string.calibration_back_alert_title))
			.setCancelable(false)
			.setNegativeButton(getResources().getString(R.string.calibration_back_alert_negitive_button), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					onResume();
				}
			})
			.setPositiveButton(getResources().getString(R.string.calibration_back_alert_positive_button), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					// calibration didn't complete successfully. return error-level message
					// to the caller and finish the current activity
					Intent returnIntent = new Intent();
					getActivity().setResult(Activity.RESULT_CANCELED, returnIntent);
					getActivity().finish();
				}
			});
		
		// create a new instance of DialogHelper, set parameters and display the dialog
		DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
		box.showDialog();
	}
	
	
	private void createCalibrationErrorAlert(){
		AlertDialog.Builder dlgAlert= new AlertDialog.Builder(getActivity())
	    	.setTitle(getResources().getString(R.string.calibration_error_alert_title))
	    	.setCancelable(false)
	        .setPositiveButton(getResources().getString(R.string.calibration_error_alert_positive_button),
	        	new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {

	                	// calibration didn't complete successfully. return error-level message
						// to the caller and finish the current activity
	                	Intent returnIntent = new Intent();
						getActivity().setResult(Activity.RESULT_CANCELED, returnIntent);
						getActivity().finish();
	                }
	        });
		
		// create a new instance of DialogHelper, set parameters and display the dialog
		DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
		box.setDialogButtonParams(null, -1, getActivity().getResources().getColor(R.color.blue));
		box.showDialog();
	}
	
	private void createCalibrationInstructionAlert(){
		// Fix Portrait orientation
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Build a one button dialog and show it
		AlertDialog.Builder dlgAlert= new AlertDialog.Builder(getActivity())
			.setTitle(getResources().getString(R.string.calibration_initial_alert_title))
		    .setMessage(getResources().getString(R.string.calibration_initial_alert_message))
		    .setCancelable(false)
		    .setPositiveButton(getResources().getString(R.string.calibration_initial_alert_positive_button), new DialogInterface.OnClickListener() {
		    	public void onClick(DialogInterface dialog, int whichButton) {
		    		mBeginCalibration = true;
		    	}
		    });
		
		// create a new instance of DialogHelper, set parameters and display the dialog
		DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
		box.setDialogButtonParams(null, -1, getActivity().getResources().getColor(R.color.blue));
		box.showDialog();
	}
	
	private void createCalibrationFinishedAlert(){
		
	    AlertDialog.Builder dlgAlert= new AlertDialog.Builder(getActivity())
	    	.setTitle(getResources().getString(R.string.calibration_done_alert_title))
	    	.setCancelable(false)
	    	.setPositiveButton(getResources().getString(R.string.calibration_done_alert_positive_button), new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    			
	    			// calibration completed successfully. return a success message to the 
	    			// caller and finish the current activity
      		  		Intent returnIntent = new Intent();
      		  		getActivity().setResult(Activity.RESULT_OK, returnIntent);
      		  		getActivity().finish();
	    		}
	    	});

	    // create a new instance of DialogHelper, set parameters and display the dialog
	    DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
	    box.showDialog();
	}
}
