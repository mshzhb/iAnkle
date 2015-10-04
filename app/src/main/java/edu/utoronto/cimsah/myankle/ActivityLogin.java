package edu.utoronto.cimsah.myankle;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import edu.utoronto.cimsah.myankle.Helpers.FragmentHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;

public class ActivityLogin extends FragmentActivity {

	public static final String TAG = ActivityLogin.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		// set default values in Preferences and SharedPreferences
	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    PrefUtils.setDefaultPreferences(this);
		
		// create a new instance of FragmentLogin and add it to the placeholder
		Fragment newFragment = FragmentLogin.newInstance();
		FragmentHelper.swapFragments(getSupportFragmentManager(), 
				R.id.activity_login_container, newFragment, 
				true, false, null, FragmentLogin.TAG);
	}
}
