package edu.utoronto.cimsah.myankle.Game;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.unity3d.player.UnityPlayer;
import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.AccelerometerManager;

import edu.utoronto.cimsah.myankle.R;

public class MainActivity extends UnityPlayerActivity implements Accelerometer.AccelerometerListener{
    private UnityPlayer mUnityPlayer;



    private Accelerometer mAccelerometer;
    private TextView textViewX;
    private TextView textViewY;
    private TextView textViewZ;

    public static float x;
    public static float y;
    public static float z;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);
        textViewX = (TextView)findViewById(R.id.xacc);
        textViewY = (TextView)findViewById(R.id.yacc);
        textViewZ = (TextView)findViewById(R.id.zacc);
        mUnityPlayer = new UnityPlayer(this);
        int glesMode = mUnityPlayer.getSettings().getInt("gles_mode", 1);
        boolean trueColor8888 = false;
        mUnityPlayer.init(glesMode, trueColor8888);

        // Add the Unity view
        FrameLayout layout = (FrameLayout) findViewById(R.id.u3dlayout);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(RadioGroup.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT);
        layout.addView(mUnityPlayer.getView(), 0, lp);

        // Init hardware
        mAccelerometer = AccelerometerManager.get(this);
        mAccelerometer.registerListenerAndConnect(this);
        mAccelerometer.start();
    }

    /*ECE496 */
    @Override
    public void onAccelerometerEvent(final float[] values) {

         x = -values[0]/2;
         y = values[1]/2;
         z = values[2]/2;


        textViewX.setText(""+x);
        textViewY.setText(""+y);
        textViewZ.setText(""+z);

    }

    @Override
    public void onAccelerometerConnected(final boolean isBluetoothDevice, String MAC_Address) {

        // Register the device listener. The accelerometer device is guaranteed to exist and be valid
        mAccelerometer.start();
    }
    @Override
    public void onAccelerometerDisconnected() {
    }

    public static float getX() {

        return x;
    }
    public static float getZ() {

        return z;
    }

}
