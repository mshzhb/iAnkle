package edu.utoronto.cimsah.myankle.Helpers;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

import edu.utoronto.cimsah.myankle.BuildConfig;
import edu.utoronto.cimsah.myankle.R;

public class SoundPoolHelper {
	public static final String TAG = SoundPoolHelper.class.getSimpleName();
	private final int LAST_TO_LOAD;
	private Context m_context;
	private SoundPool m_sp;
	private boolean loaded = false;
	
	// Sounds
	private int longPingDing;
	private int shortDing;
	
	public SoundPoolHelper(Context context) {
		m_context = context;
		m_sp = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
	    m_sp.setOnLoadCompleteListener(new OnLoadCompleteListener() {
	    	@Override
	    	public void onLoadComplete(SoundPool sp, int sampleId,
	    			int status) {
	    		if(sampleId == LAST_TO_LOAD) {
	    			loaded = true;
	    		}
	    	}
	    });
	    
	    // Longer sound - signals end (and success) of entire calibration activity
	    longPingDing = m_sp.load(m_context, R.raw.pingding, 1);
	    
	    // Shorter sound - signals end of one axis of calibration
	    shortDing = m_sp.load(m_context, R.raw.lumding, 1);
	    
	    // LAST_TO_LOAD is the integer ID of the last sound to use m_sp.load(...)
	    LAST_TO_LOAD = shortDing;
	}
	
	public void pause() {
		m_sp.autoResume();
	}
	public void resume() {
		m_sp.autoResume();
	}
	public void release() {
		m_sp.release();
	}
	
	public void longPingDing() {
		ping(longPingDing);
	}
	public void shortDing() {
		ping(shortDing);
	}
	
	private void ping(int soundID) {
		if(PrefUtils.getPingCheckBox(m_context)) {
			// Getting the user sound settings
		    AudioManager audioManager = (AudioManager) m_context.getSystemService(Context.AUDIO_SERVICE);
		    float actualVolume = (float) audioManager
		        .getStreamVolume(AudioManager.STREAM_MUSIC);
		    float maxVolume = (float) audioManager
		        .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		    float volume = actualVolume / maxVolume;
		    // Is the sound loaded already?
		    if (loaded) {
		      m_sp.play(soundID, volume, volume, 1, 0, 1f);
		      if(BuildConfig.DEBUG) Log.d(TAG, "Played sound");
		    }
		} else {
			if(BuildConfig.DEBUG) Log.d(TAG, "Suppressed sound");
		}
	}

}
