package edu.utoronto.cimsah.myankle.Threads;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.http.message.BasicNameValuePair;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import edu.utoronto.cimsah.myankle.Http.HttpUpload;
import edu.utoronto.cimsah.myankle.Http.HttpUpload.httpUploadListener;
import edu.utoronto.cimsah.myankle.BuildConfig;

public class RunnableUploadFile implements Runnable {

	private static final String TAG = RunnableUploadFile.class.getSimpleName();
	
	private Context mContext;
	private int mUserId;
	private String mFileName;
	private String mFullPath;
	
	private List<BasicNameValuePair> mParams;
	private httpUploadListener mListener;
	
	// used for background send file
	public RunnableUploadFile(Context context, int user_id, String fileName, String fullPath, httpUploadListener listener) {
		mContext = context;
		mUserId = user_id;
		mFileName = fileName;
		mFullPath = fullPath;
		mListener = listener;
		
		mParams = new ArrayList<BasicNameValuePair>();
		mParams.add(new BasicNameValuePair("userId", String.valueOf(mUserId)));
		
	}
	
	public RunnableUploadFile(Context context, int user_id, String fileName, String fullPath) {
		this(context, user_id,fileName,fullPath, null);
	}
	
	@Override
	public void run() {
	
		// Check if user_id is valid
		if(mUserId > 0) {
			// Check if Internet is connected using these
			
			ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = connManager.getActiveNetworkInfo();
			if(netInfo != null && netInfo.isConnected()) {
				
				// get a file input stream to the file that you want to send
				FileInputStream fstrm = null;
				try {
				    // Set your file path here
				    fstrm = new FileInputStream(mFullPath);	
				} catch (FileNotFoundException e) {
					  if(BuildConfig.DEBUG) Log.e(TAG, "File not found expection: " + e.getMessage());
				}	
				
		    	// create the server interaction object
				HttpUpload httpUpload = new HttpUpload(mListener);
				httpUpload.execute("http://www.myankle.ca:8080", mFileName, mParams, fstrm);
				
			} else {
				if(BuildConfig.DEBUG) Log.e(TAG, "No wifi! I gave up.");
			}			
		}	
	}	
	
}
