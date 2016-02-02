package edu.utoronto.cimsah.myankle.Game;

import android.app.Service;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.unity3d.player.UnityPlayer;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.AccelerometerManager;
import edu.utoronto.cimsah.myankle.R;

public class MainActivity extends UnityPlayerActivity implements Accelerometer.AccelerometerListener{
    private UnityPlayer mUnityPlayer;


        public Vibrator vibrator;
    private Accelerometer mAccelerometer;
    private TextView textViewX;
    private TextView textViewY;
    private TextView textViewZ;


    public static int device;

    public static float BN;
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
        vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);
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

         x = values[0];
         y = values[1];
         z = values[2];
        x = (float)(Math.round(x*100))/100;
        y = (float)(Math.round(y*100))/100;
        z = (float)(Math.round(z*100))/100;


        double tmp = x*x + y*y + z*z - 9.80*9.80;
        BN =  (float)Math.sqrt(tmp);
        if (!(BN >= 0))
        {
            BN = 0;
        }

        if(BN>3)
        {
            vibrator.vibrate(100);
        }

        textViewX.setText("" + x);
        textViewY.setText("" + y);
        textViewZ.setText("BN" + BN);

    }

    @Override
    public void onAccelerometerConnected(final boolean isBluetoothDevice, String MAC_Address) {

        // Register the device listener. The accelerometer device is guaranteed to exist and be valid
        mAccelerometer.start();
    }
    @Override
    public void onAccelerometerDisconnected() {
    }
//横向移动

    public static float getX() {
       if(device == AccelerometerManager.TYPE_METAWEAR) {

           if (Math.abs(z) > 0.1)
               return z/2;
           else
               return 0;
       }
        else
       {        x = ((float) (x * 1.3));
               return x;}
    }
    public static float getZ() {

        return z;
    }

    public static float getY() {

        return y;
    }

    public static float getBN() {



            return  BN;
    }



}
