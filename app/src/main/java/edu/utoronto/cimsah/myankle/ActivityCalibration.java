package edu.utoronto.cimsah.myankle;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;

public class ActivityCalibration extends FragmentActivity {

	@SuppressWarnings("unused")
	private static final String TAG = ActivityCalibration.class.getSimpleName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);
		
		// create a new instance of FragmentCalibration and add it to the placeholder
		FragmentCalibration newFragment = FragmentCalibration.newInstance();
		FragmentHelper.swapFragments(getSupportFragmentManager(),
				R.id.activity_calibration_container, newFragment,
				true, false, null, FragmentCalibration.TAG);
	}

	// on pressing the back button, create a dialog asking whether
	// calibration should be ended
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		// get a reference to the instance of FragmentCalibration in the placeholder
		FragmentCalibration fragment = (FragmentCalibration) getSupportFragmentManager()
				.findFragmentById(R.id.activity_calibration_container);
		
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			
			// if fragment in the placeholder is not null (redundant check), 
			// let the fragment handle the key-press
			if(fragment != null) fragment.onBackPressed();
		}
		
		return false;
	}
}
