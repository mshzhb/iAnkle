package edu.utoronto.cimsah.myankle;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Timer;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.AccelerometerManager;
import edu.utoronto.cimsah.myankle.Game.MainActivity;
import edu.utoronto.cimsah.myankle.Helpers.DatabaseHelper;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.Helpers.Samples;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentGameEntrance.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentGameEntrance#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentGameEntrance extends Fragment implements Accelerometer.AccelerometerListener {
    private static final int STATE_MEASURE = 3;
    private static final int STATE_IDLE = 4;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static final String TAG = FragmentGameEntrance.class.getSimpleName();
    // TODO: Rename and change types of parameters
    private TextView TextViewRealTimeBN;
    private TextView textViewX;
    private TextView textViewY;
    private TextView textViewZ;
    private Button ButtonStart;
    private Button ButtonGame;
    private OnFragmentInteractionListener mListener;

    //ECE496
    private DatabaseHelper mDatabaseHelper = null;
    private Timer mTimer;
    long lastUpdate = 0;
    long lastUpdateBN = 0;
    private Samples mSamples;
    // Starting measurement state
    private volatile int mState = STATE_MEASURE;

    // Hardware
    private Accelerometer mAccelerometer;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *

     * @return A new instance of fragment FragmentGameEntrance.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentGameEntrance newInstance() {
        FragmentGameEntrance fragment = new FragmentGameEntrance();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    public FragmentGameEntrance() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabaseHelper = new DatabaseHelper(getActivity());

        // Init hardware
        mAccelerometer = AccelerometerManager.get(getActivity());
        mAccelerometer.registerListenerAndConnect(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fragment_game_entrance, container, false);
        ButtonStart = (Button) view.findViewById(R.id.buttonStart);
        ButtonGame = (Button) view.findViewById(R.id.buttongame);
        ButtonStart.setText("Stop");
        ButtonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onStartButtonClick(v);
            }
        });
        ButtonGame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), MainActivity.class);
                startActivity(i);
            }
        });
                TextViewRealTimeBN = (TextView)view.findViewById(R.id.realtimebn);
        textViewX = (TextView)view.findViewById(R.id.textViewx);
        textViewY = (TextView)view.findViewById(R.id.textViewy);
        textViewZ = (TextView)view.findViewById(R.id.textViewz);
        mSamples = new Samples(readCalibrationValues());
        mSamples.clear();
        mAccelerometer.start();
        return view;

    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDatabaseHelper.close();
        mAccelerometer.unregisterListenerAndDisconnect(this);
    }
    private void onStartButtonClick(View v){
        if(mState == STATE_IDLE){
            mAccelerometer.start();
            mState = STATE_MEASURE;
            ButtonStart.setText("Stop");
        }
        else {
            mAccelerometer.stop();
            mState = STATE_IDLE;
            ButtonStart.setText("Start");
        }

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }




    /*ECE496 */
    @Override
    public void onAccelerometerEvent(final float[] values) {



            // Collect data
            long curTime = System.currentTimeMillis();


                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float elapsedTime = diffTime;
                float x = values[0];
                float y = values[1];
                float z = values[2];
                mSamples.add(elapsedTime, x, y, z);
            if (BuildConfig.DEBUG) {
                // do something for a debug build
                textViewX.setText(""+x);
                textViewY.setText(""+y);
                textViewZ.setText(""+z);
            }

            if ((curTime - lastUpdateBN) > 400) {
                float realTimeBN = mSamples.get_mean_r();
                TextViewRealTimeBN.setText(""+realTimeBN);
                mSamples.clear();
                lastUpdateBN = curTime;
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
