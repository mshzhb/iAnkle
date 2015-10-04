package edu.utoronto.cimsah.myankle;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Date;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer.AccelerometerListener;
import edu.utoronto.cimsah.myankle.Accelerometers.AccelerometerManager;
import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.DialogStyleHelper;
import edu.utoronto.cimsah.myankle.Helpers.FileIOHelper;
import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.Helpers.Samples;
import edu.utoronto.cimsah.myankle.Helpers.SoundPoolHelper;
import edu.utoronto.cimsah.myankle.Threads.RunnableSaveRawDataFile;
import edu.utoronto.cimsah.myankle.Threads.RunnableUploadFile;

@SuppressWarnings("unused")
public class FragmentExerciseMeasureRecyclerView extends Fragment implements AccelerometerListener, OnSeekBarChangeListener {
	
	public static final String TAG = FragmentExerciseMeasureRecyclerView.class.getSimpleName();
	
	public final static String RAW_FOLDER_PATH = "Raw/";
	private static final int STATE_INIT = 1;
	private static final int STATE_COUNTDOWN = 2;
	private static final int STATE_MEASURE = 3;
	private static final int STATE_FINISH = 4;
	
	// Countdown Timer
	private static final int TICK_INTERVAL_IN_MILLIS = 100;
	private static final int SHORT_VIBRATE_DURATION_IN_MILLIS = 50;
	private static final int LONG_VIBRATE_DURATION_IN_MILLIS = 200;
	private MyCounter mTimer;
	
	// SoundPool
	private SoundPoolHelper mSoundPoolHelper;

	// Starting measurement state 
	private volatile int mState = STATE_INIT;

	// Hardware
	private Accelerometer mAccelerometer;
	private Vibrator mVibrator;
	
	// Miscellaneous objects
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private DatabaseHelper mDatabaseHelper = null;
	private Thread mMeasureThread = null;
	private Samples mSamples = null;
	
	// Views
	private TextView mTextTitle, mTextTime, mTextSubtitle;
	
	// User specific data
	private int mUserId = -1;
	private int mServerId = -1;
	private int mExerciseId = -1;
	private String mAnkleSide = null;
	private float mMeanR;
	private int mExerciseDuration;
	private int mCountdownDuration;

	// SeekBar
	private SeekBar mStopSeekBar;
	private int mSeekBarProgress;
	
	/** Instantiate FragmentExerciseMeasureRecyclerView */
	public static FragmentExerciseMeasureRecyclerView newInstance(int exercise_id) {

		// Create and populate a bundle to instantiate the fragment with
		FragmentExerciseMeasureRecyclerView myFragment = new FragmentExerciseMeasureRecyclerView();
		
		Bundle args = new Bundle();	
		args.putInt("exercise_id", exercise_id);
		myFragment.setArguments(args);
	
		return myFragment;
	} 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mDatabaseHelper = new DatabaseHelper(getActivity());
		
		/* Indicates that the fragment should receive all menu-related
		 * callbacks that are not explicitly consumed in the host activity */
		setHasOptionsMenu(true);
		
		// Init hardware
		mAccelerometer = AccelerometerManager.get(getActivity());
		mAccelerometer.registerListenerAndConnect(this);

		mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
		
		// Init SoundPool
		getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mSoundPoolHelper = new SoundPoolHelper(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// Inflate the fragment's layout file
		View view = inflater.inflate(R.layout.fragment_exercise_measure, container, false);
		mStopSeekBar = (SeekBar) view.findViewById(R.id.fragment_exercise_measure_seekbar);
		mStopSeekBar.setOnSeekBarChangeListener(this);
		
		view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		
		view.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
		    @Override
		    public void onSystemUiVisibilityChange(int visibility) {
		    }
		});
		return (view);
	}
	  
	@Override
	public void onActivityCreated(Bundle bundle) {
		super.onActivityCreated(bundle);

		/* Retrieve user id, server id, exercise id, and ankleSide 
		 * information from the arguments and shared preferences */
		mUserId = PrefUtils.getIntPreference(getActivity(), PrefUtils.LOCAL_USER_ID);
		
		// Print debug message
		if(BuildConfig.DEBUG) Log.d(TAG, "User ID is " + mUserId); 
		
		mServerId = PrefUtils.getIntPreference(getActivity(), PrefUtils.SERVER_ID);
		mExerciseId = getArguments().getInt("exercise_id", -1);
		
		mAnkleSide = PrefUtils.getStringPreference(getActivity(), PrefUtils.ANKLE_SIDE_KEY);
		Assert.assertTrue(mExerciseId >= 0);
		Assert.assertTrue(mAnkleSide != null);
		
		// Retrieve duration information from SharedPreferences
		mCountdownDuration = Integer.parseInt(PrefUtils.getStringPreference(getActivity(), "prefCountdownDuration")) * 1000; 
		mExerciseDuration = Integer.parseInt(PrefUtils.getStringPreference(getActivity(), "prefExerciseDuration")) * 1000; 
		
		// Init views
		mTextTitle = (TextView) getView().findViewById(R.id.fragment_exercise_measure_textview_title);
		mTextTime = (TextView) getView().findViewById(R.id.fragment_exercise_measure_textview_time);
		mTextSubtitle = (TextView) getView().findViewById(R.id.fragment_exercise_measure_textview_subtitle);
		
		// Init and clear samples
		mSamples = new Samples(readCalibrationValues());
		mSamples.clear();
	}


	@Override
	public void onResume() {
		super.onResume();
				
		// Set the action bar parameters
		getActivity().getActionBar().setTitle("Measure");
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		
		// Keep screen on
		getActivity().getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Require sensors
		mSoundPoolHelper.resume();
		mAccelerometer.start();
		
		// Start the measurement routine
		startMeasuring();
	}

	@Override
	public void onPause() {
		super.onPause();

		// Allow screen to turn off 		
		getActivity().getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Cancel the sensor and countdown activity
		mAccelerometer.stop();
		mMeasureThread.interrupt();
		if(mTimer != null) mTimer.cancel();
		
		// If it's measuring and paused, go back to instruction fragment
		if(mState != STATE_FINISH) {
			
			mSoundPoolHelper.pause();
			mSamples.clear();
			backToInstructionFragment();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mDatabaseHelper.close();
		mSoundPoolHelper.release();
		mAccelerometer.unregisterListenerAndDisconnect(this);
	}

	/** 
	 * Subroutine to initiate countdown and measurement functionality in a worker 
	 * thread. Emulates a simple State Machine to handle recurring, sequential code. 
	 */
	public void startMeasuring() {
		
		// Set the initial state
		mState = STATE_INIT;
		
		// Create a worker thread and delegate the runnable to it
		mMeasureThread = new Thread(new RunnableExerciseMeasure());
		mMeasureThread.start();
	}
	
	/** Runnable to manage collection of accelerometer data. Handles the countdown- and
	 * measurement-timers. Also updates layout elements from the UI thread as appropriate */
	private class RunnableExerciseMeasure implements Runnable {
		
		@Override
		public void run() {
			
			// While the 'Finished' state hasn't been reached
			while(mState != STATE_FINISH) {
				
				// If the thread has been interrupted, stop executing
				if(Thread.interrupted()) {
					return;
				}
				
				// Currently in the 'Initialization' state
				if(mState == STATE_INIT) {
					
					// Invalidate the timer and update the state
					mTimer = null;
					mState = STATE_COUNTDOWN;
					
					// Enqueue layout-element updates to the main (UI) thread
					mHandler.post(mRunnableInitUI);
					
				// Currently in the	'Countdown' state and the countdown timer has completed
				} else if(mState == STATE_COUNTDOWN && (mTimer != null && mTimer.isTimerDone())) {
					
					/* Sanity check: Ensure that the accelerometer device has been connected by
					 * the end of the countdown. If so, proceed to the measurement state. If not, 
					 * skip to the final state */
					if(mAccelerometer.isConnected()) {
						
						// Invalidate the timer and update the state
						mTimer = null;
						mState = STATE_MEASURE;
						
						// Enqueue layout-element updates to the main (UI) thread
						mHandler.post(mRunnableCountdownUI);
						
					// Else, the accelerometer has not been connected yet	
					} else {
						
						// Skip to the exit code
						mState = STATE_FINISH;
					}
					
				// Currently in the 'Measuring' state and the measurement timer has completed
 				} else if(mState == STATE_MEASURE && (mTimer != null && mTimer.isTimerDone())) {
					
					// Update the state
					mState = STATE_FINISH;
				}
			}
			
			// Currently in the 'Finished' state and the accelerometer is still connected
			if(mState == STATE_FINISH && mAccelerometer.isConnected()) {
				
				// Enqueue UI tasks to the main thread
				mHandler.post(mRunnableFinishSuccessUI);
				
			// Else, the measurement failed (accelerometer was disconnected)
			} else {
				
				// Perform the error-handling in the main thread
				mHandler.post(mRunnableFinishFailedUI);
			}
		}

		/** Runnable to enqueue UI operations in the 'Initialization' state. */
		private Runnable mRunnableInitUI = new Runnable() {
			
			@Override
			public void run() {
				
				// Update the timer-related TextViews
				mTextTitle.setText(R.string.exercise_countdown_title);
				mTextSubtitle.setText(R.string.exercise_countdown_subtitle);
				
				/* Start the countdown timer. Note: Android requires that 
				 * CountDownTimer processes be run on the main thread. */
				mTimer = new MyCounter(mCountdownDuration, TICK_INTERVAL_IN_MILLIS, true);
				mTimer.start();
			}
		};

		/** Runnable to enqueue UI operations in the 'Countdown' state. */
		private Runnable mRunnableCountdownUI = new Runnable() {
			
			@Override
			public void run() {

				// Update the timer-related TextViews
				mTextTitle.setText(R.string.exercise_messure_title);
				mTextSubtitle.setText(R.string.exercise_messure_subtitle);

				// Start the measurement timer
				mTimer = new MyCounter(mExerciseDuration, TICK_INTERVAL_IN_MILLIS, true);
				mTimer.start();
			}
		};
		
		/** Runnable to enqueue UI operations in the 'Finished' state, 
		 * where the measurement completed successfully. */
		private Runnable mRunnableFinishSuccessUI = new Runnable() {
			
			@Override
			public void run() {
				
				// Vibrate to indicate completion of the measurement
				vibrate(LONG_VIBRATE_DURATION_IN_MILLIS);

				// Create a dialog box asking user to Save or Delete 
				AlertDialog.Builder dlgAlert = new AlertDialog.Builder(getActivity())
						.setTitle("Done measuring!")
						.setCancelable(false)
						.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								mSamples.clear();
								backToInstructionFragment();
							}
						})
						.setPositiveButton("Save", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								int session_id = saveResults();
								goToResultFragment(session_id);
							}
						});

				// Create a new instance of DialogHelper and display the dialog
				DialogStyleHelper box = new DialogStyleHelper(getActivity(), dlgAlert.create());
				box.showDialog();
			}
		};

		/** Runnable to enqueue UI operations in the 'Finished' state, 
		 * where the measurement operation was unsuccessful. */
		private Runnable mRunnableFinishFailedUI = new Runnable() {
			
			@Override
			public void run() {
				
				// Create a toast with the appropriate error message
				Toast.makeText(getActivity(), "Measurement failed. " +
						"Accelerometer device disconnected!", Toast.LENGTH_SHORT)
						.show();

				// Invalidate the collected samples and release the sensors
				mSamples.clear();
				mAccelerometer.stop();
				mSoundPoolHelper.pause();
				if(mTimer != null) mTimer.cancel();

				// Return to the instruction fragment
				backToInstructionFragment();
			}
		};
 	}
	
	@Override
	public void onAccelerometerEvent(final float[] values) {

		if (mState == STATE_MEASURE && mTimer != null) {

			// Collect data
			float elapsedTime = mTimer.getElapsedSec();
			float x = values[0];
			float y = values[1];
			float z = values[2];

			if (BuildConfig.DEBUG) {
				// do something for a debug build
			//	Toast bread = Toast.makeText(getActivity(), "x,y,z:("+x+","+y+","+z+")", Toast.LENGTH_LONG);
			//	bread.show();
			}
			mSamples.add(elapsedTime, x, y, z);
		}
	}
	
	@Override
	public void onAccelerometerConnected(final boolean isBluetoothDevice, String MAC_Address) {
		
		// Register the device listener. The accelerometer device is guaranteed to exist and be valid
		mAccelerometer.start();
	}
	
	@Override
	public void onAccelerometerDisconnected() {
	}
	
	/**
	 * This method manages all the saving of results from this session. 
	 * - calls databaseSaveSummary() to save summary of session into database (ie. userId, exerciseId, meanR, date...)
	 * - calls postSaveThread(...) to save the raw data to a .csv file
	 * - calls postSendThread(...) to send the raw data off to the server
	 */
	private int saveResults() {
		
		mAccelerometer.stop();
		
		// Save all samples to file
		int session_id = databaseSaveSummary();
		
		// Setup parameters for SaveThread and SendThread
		String filename = makeFileName(session_id);
		FileIOHelper fHelper = new FileIOHelper();
		FileIOHelper.ExternalStorageHelper esHelper = fHelper.new ExternalStorageHelper(
				getActivity(), 
				RAW_FOLDER_PATH, 
				filename
		);
		
		// Save raw data to a .csv file
		postSaveThread(filename, session_id, esHelper);
		
		// If the user did not opt out of data collection
		if ( PrefUtils.getIntPreference(getActivity(), PrefUtils.OPT_OUT_KEY) != 1){
			
			// Send the .csv file to the server
			postSendThread(filename, session_id, esHelper.getFullPath());
		}
		
		if(BuildConfig.DEBUG) Log.d("FragmentExerciseMeasureRecyclerView", String.valueOf(session_id));
		return session_id;
	} 

	/**
	 * Saves a summary of the session to the database, in the "sessions" table
	 * The summary includes (userId, exerciseId, meanR, numSamples, ankleSide, date)
	 * @return the session_id integer auto-incremented in the table
	 */
	private int databaseSaveSummary() {
		
		int session_id = -1;
		
		try {
			mMeanR = mSamples.get_mean_r();
			String date = DateFormat.format("yyyy-MM-dd", new Date()).toString();
			mDatabaseHelper.getWritableDatabase().execSQL(
					"INSERT INTO sessions (userId, exerciseId, meanR, ankleSide, date) VALUES ("
							+ mUserId + ", " + mExerciseId + ", " + mMeanR + ", '" + mAnkleSide + "', '" + date + "');");
			
			Cursor cursor = mDatabaseHelper.getReadableDatabase().rawQuery("SELECT MAX(_id) FROM sessions", null);
			cursor.moveToFirst();
			session_id = cursor.getInt(0);
			
			cursor.close();
			mDatabaseHelper.close();
			
		} catch (Throwable t) {
			if(BuildConfig.DEBUG) Log.e(TAG, t.toString());
		}
		return session_id;
	}
	
	/**
	 * Enqueues a new thread to threadManager to save the raw data in a .csv file.
	 * - creates a progress dialog and a UI handler
	 * - passes an ExternalStorageHelper, the Samples object, 
	 * 		the progress dialog and UI handler to the SaveFileRunnable
	 * 		
	 * @param filename The name of the CSV file
	 * @param session_id The exercise-session ID
	 * @param esHelper Instance of the Storage Helper class
	 */
	private void postSaveThread(String filename, int session_id, FileIOHelper.ExternalStorageHelper esHelper) {
		if(session_id > 0) {
			
			ProgressDialog pD = makePD();
			Handler m_pDHandler = new Handler();

			String date = DateFormat.format("yyyy-MM-dd", new Date()).toString();			
			
			// Fetch the user's age and gender from the database
			Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
					"SELECT age, gender FROM users WHERE _id = " + mUserId, null);
			
			cur.moveToFirst();
			int age = cur.getInt(0);
			String gender = cur.getString(1);
			
			// Get the latest left-ankle injury date
			cur = mDatabaseHelper.getReadableDatabase().rawQuery(
					"SELECT MAX(injuryDate) FROM injuries WHERE ankleSide='Left' " +
					" AND userId = " + mUserId + ";", null);
			
			cur.moveToFirst();
			String injuryDateLeft = cur.getString(0);
			
			// Get the latest right-ankle injury date
			cur = mDatabaseHelper.getReadableDatabase().rawQuery(
					"SELECT MAX(injuryDate) FROM injuries WHERE ankleSide='Right' " +
					" AND userId = " + mUserId + ";", null);
			
			cur.moveToFirst();
			String injuryDateRight = cur.getString(0);
			
			// Create a runnable to write the collected data to a file
			ActivityMeasure.mPipelineThread.enqueueNewTask(
					new RunnableSaveRawDataFile(esHelper, 
										PrefUtils.getIntPreference(getActivity(), PrefUtils.SERVER_ID),
										gender,age,injuryDateLeft,injuryDateRight,mExerciseId, date,
										mSamples, pD, m_pDHandler));
			
			// Release the cursor
			cur.close();
		}
	}
	
	/**
	 * Enqueues a new thread to threadManager to send the raw data in a .csv file to the server
	 * - creates a List of BasicNameValuePairs containing userId, meanR, and ankleSide info
	 * - passes the context, filename, file's path, the list, 
	 * 		and an httpInteractionResponseListener to the SendFileRunnable
	 * 		
	 * @param filename The name of the CSV file
	 * @param session_id The exercise-session ID
	 * @param path The path to the storage directory
	 */
	private void postSendThread(String filename, int session_id, String path) {
		if(session_id > 0) {
			ActivityMeasure.mPipelineThread.enqueueNewTask(
				new RunnableUploadFile( getActivity(), mServerId, filename, path)
			);
		}
	}

	/**
	 * Creates a filename string of the format
	 * gender_age_ankleSide_e#_s#_yyyyMMdd_h_mmaa.csv
	 * ie. male_32_left_e2_s15_20130814_12_08pm.csv
	 * 
	 * @param session_Id The exercise-session ID
	 * @return The compiled string for filename
	 */
	private String makeFileName(int session_Id) {
		
		String exerciseStr = "e" + mExerciseId;
		String sessionStr = "s" + session_Id;
		String date = DateFormat.format("yyyyMMdd", new Date()).toString();
		String curTime = DateFormat.format("h_mmaa", new Date()).toString();
		
		// Get user's age and gender from the database
		Cursor cur = mDatabaseHelper.getReadableDatabase().rawQuery(
				"SELECT age, gender FROM users WHERE _id = " + mUserId, null);

		cur.moveToFirst();
		int age = cur.getInt(0);
		String gender = cur.getString(1);
		
		// Get the latest left-ankle injury date
		cur = mDatabaseHelper.getReadableDatabase().rawQuery(
				"SELECT MAX(injuryDate) FROM injuries WHERE ankleSide='Left' " +
				" AND userId = " + mUserId + ";", null);
		
		cur.moveToFirst();
		String injuryDateLeft = cur.getString(0);
					
		// Get the latest right-ankle injury date
		cur = mDatabaseHelper.getReadableDatabase().rawQuery(
				"SELECT MAX(injuryDate) FROM injuries WHERE ankleSide='Right' " +
				" AND userId = " + mUserId + ";", null);
		cur.moveToFirst();
		String injuryDateRight = cur.getString(0);
		
		// Release the cursor
		cur.close();
		
		// Generate the file-name based on the user parameters
		String filename = "";
		filename += gender + "_";
		filename += Integer.toString(age) + "_";
		filename += "injLeft-" + injuryDateLeft + "_";
		filename += "injRight-" + injuryDateRight + "_";
		filename += mAnkleSide + "_";
		filename += exerciseStr + "_";
		filename += sessionStr + "_";
		filename += date + "_";
		filename += curTime;
		filename += ".csv";
		return filename;
	}
	
	/**
	 * Initializes a progress dialog for saving raw data to .csv file
	 * @return the progress dialog
	 */
	private ProgressDialog makePD() {
		
		ProgressDialog pD = new ProgressDialog(getActivity());
		pD.setCancelable(false);
		pD.setMessage("Saving raw data...");
		pD.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pD.setProgress(0);
		pD.setMax(mSamples.getNumSamples());
		pD.show();
		return pD;
	}
	
	/**
	 * Causes the vibrator to vibrate for millis amount of milliseconds.
	 * - checks Preferences first to see if vibrate is enabled
	 */
	private void vibrate(int millis) {
		if(PrefUtils.getVibrateCheckBox(getActivity())) {
			if(mVibrator.hasVibrator()) {
				mVibrator.vibrate(millis);
			}
		}
	}

	/*
	 * SeekBar methods
	 * (non-Javadoc)
	 * @see android.widget.SeekBar.OnSeekBarChangeListener#onProgressChanged(android.widget.SeekBar, int, boolean)
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		mSeekBarProgress = progress;		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

		// User has dragged seekBar to the end. Cancel the timer
		if(mSeekBarProgress == seekBar.getMax()) {
			if(mTimer != null) {
				
				// Stop the timer
				mTimer.cancel();
				
				// Set off the vibrator
				vibrate(SHORT_VIBRATE_DURATION_IN_MILLIS);
			
				backToInstructionFragment();
			}
		} else {
			
			// Reset the seek bar to the beginning
			seekBar.setProgress(0);
		}	
	}
	
	/**
	 * Navigating out of this fragment. Launch FragmentExerciseInstruction.
	 */
	private void backToInstructionFragment() {
		
		// Display the most recent fragment on the back-stack
		String backStackName = FragmentExercisesRecyclerView.TAG;
		getActivity().getSupportFragmentManager().popBackStackImmediate(backStackName, 0);
	}
	
	/**
	 * Launch FragmentExerciseResult.
	 * Puts session_id into a bundle and passes it along
	 */
	private void goToResultFragment(int session_id) {
		
		FragmentExerciseResults newFragment = FragmentExerciseResults.newInstance(session_id);
		String nextFragmentTag = FragmentExerciseResults.TAG;
		FragmentHelper.swapFragments(getActivity().getSupportFragmentManager(), 
				R.id.activity_measure_container, newFragment, 
				false, true, TAG, nextFragmentTag);
	}
	
	/**
	 * This class is a CountDownTimer that counts with descending times.
	 * It has additional functionality:
	 * - can play sounds by starting a MyBeepCounter object in the last 5 seconds
	 * 		(enabled by beepEnabled in constructor)
	 * - updates the m_tv_time textView with the remaining time rounded to one decimal place
	 * - updates the isTimerDone boolean flag when the timer finishes
	 * - computes elapsed time (increasing) and returns it using getElapsedSec()
	 * @author Vivian
	 *
	 */
	public class MyCounter extends CountDownTimer {
		
		private final long millisInFuture;
		private long elapsedMilliSec;	// elapsed<Time> means counting up, from 0-max
		private double sec;
		private double roundedSec;
		private boolean beepEnabled;
		private boolean beepCounterStarted;
		private boolean isTimerDone;
		private boolean isRunning;
		
		public MyCounter(long millisInFuture, long countDownInterval, boolean beepEnabled) {
			super(millisInFuture, countDownInterval);
			elapsedMilliSec = 0;
			this.millisInFuture = millisInFuture;
			isTimerDone = false;
			beepCounterStarted = false;
			this.beepEnabled = beepEnabled;
			isTimerDone = false;
			isRunning = false;
		}
		
		@Override
		public void onFinish() {
			isTimerDone = true;
			isRunning = false;
		}
		
		@Override
		public void onTick(long millisUntilFinished) {
			isRunning = true;
			elapsedMilliSec = millisInFuture - millisUntilFinished; 
			sec = (millisUntilFinished/1000.000);
			roundedSec = (double)Math.round(sec * 10) / 10;
			mTextTime.setText((roundedSec) + " seconds");
			
			if(millisUntilFinished < 5100 && beepEnabled && !beepCounterStarted) {
				new MyBeepCounter(5100, 1000).start();
				beepCounterStarted = true;
			}
		}
		
		public float getElapsedSec() {
			return (float) (elapsedMilliSec/1000.000);
		}
		
		public boolean isTimerDone() {
			return isTimerDone;
		}
		
		public boolean isTimerRunning(){
			return isRunning;
		}
	}
	
	/**
	 * This class is a CountDownTimer that plays ping(shortDing)
	 * at every tick, and plays ping(longPingDing) when it finishes.
	 * @author Vivian
	 *
	 */
	public final class MyBeepCounter extends CountDownTimer {

		public MyBeepCounter(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}
		
		@Override
		public void onTick(long millisUntilFinished) {
			mSoundPoolHelper.shortDing();
		}

		@Override
		public void onFinish() {
			mSoundPoolHelper.longPingDing();
		}
	}
	
	private ArrayList<Float> readCalibrationValues() {
		
		// Variable declaration
		Context context = getActivity();
		ArrayList<Float> calibrationResults = new ArrayList<>();
		String deviceAddress = PrefUtils.getStringPreference(context, PrefUtils.MAC_ADDRESS_KEY);
		
		// If the device address is null, reset the string to the appropriate value ("Inbuilt")
		if(deviceAddress == null) deviceAddress = "Inbuilt";
		
		/* Check if there exists an entry in the calibration table corresponding to the
		 * currently selected device. If not, start ActivityCalibration, else just read it */
		if(!FragmentCalibration.isCalibrated(context)) {
			
			// Create an intent to launch a new instance of ActivityCalibration
			Intent calibrationIntent = new Intent(getActivity(), ActivityCalibration.class);
			getActivity().startActivityForResult(calibrationIntent,
					ActivityMain.CALIBRATION_REQUEST_CODE);
			
		} else {
			
			// Read from the database
			Cursor cursor = mDatabaseHelper.getReadableDatabase().rawQuery(
					"SELECT x, y, z, xneg, yneg, zneg FROM calibration WHERE " +
							"device = '" + deviceAddress + "'", null);
			
			// Sanity check: Ensure that the cursor exists and is valid
			if(cursor != null && cursor.moveToFirst()) {
				
				// Retrieve the calibration data from the cursor
				for(int i = 0; i < cursor.getColumnCount(); i++) {
					calibrationResults.add(cursor.getFloat(i));
				}
				
				// Debug statements
				if(BuildConfig.DEBUG) {
					
					Log.d("Calibration: X", String.valueOf(calibrationResults.get(0)));
					Log.d("Calibration: Y", String.valueOf(calibrationResults.get(1)));
					Log.d("Calibration: Z", String.valueOf(calibrationResults.get(2)));
					Log.d("Calibration: XNEG", String.valueOf(calibrationResults.get(3)));
					Log.d("Calibration: YNEG", String.valueOf(calibrationResults.get(4)));
					Log.d("Calibration: ZNEG", String.valueOf(calibrationResults.get(5)));
				}
				
				// Close the cursor
				cursor.close();
			}
		}
		
		return calibrationResults;
	}
}