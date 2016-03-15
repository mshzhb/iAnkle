package edu.utoronto.cimsah.myankle.Game;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

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
    public int TI_sensor = 0;

    public static int device;
    private Button button;
    public static float BN;
    public static float x;
    public static float y;
    public static float z;
    Context myContext;
    Socket s;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);

        textViewX = (TextView)findViewById(R.id.xacc);
        textViewY = (TextView)findViewById(R.id.yacc);
        textViewZ = (TextView)findViewById(R.id.zacc);
        button = (Button)findViewById(R.id.ti);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                if(TI_sensor == 0)
                {
                    TI_sensor = 1;
                    button.setText("TI sensor");
                }
                else
                {
                    TI_sensor = 0;
                    button.setText("Phone");
                }
            }
        });


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
/*
      try {
          s = new Socket(InetAddress.getLocalHost(), 5432);
           new client(s).start();
       }
      catch (IOException e){
            Log.e("Mshzhb","TCP failed"+e.getStackTrace());
      }
*/
// get an instance of the receiver in your service
       // IntentFilter filter = new IntentFilter();
       // filter.addAction("action");
       // filter.addAction("anotherAction");
       // mReceiver = new MyReceiver();
       // registerReceiver(mReceiver, filter);



    }

    /*ECE496 */
    @Override
    public void onAccelerometerEvent(final float[] values) {


        if(TI_sensor == 0) {
            x = values[0];
            y = values[1];
            z = values[2];
            x = (float) (Math.round(x * 100)) / 100;
            y = (float) (Math.round(y * 100)) / 100;
            z = (float) (Math.round(z * 100)) / 100;

        }
        else if(TI_sensor == 1){
            x = dataRead("acc_x");
            y = dataRead("acc_y");
            z = dataRead("acc_z");

            //x = (float) (Math.round(x * 100)) / 100;
            //y = (float) (Math.round(y * 100)) / 100;
            //z = (float) (Math.round(z * 100)) / 100;
        }
           double tmp = x * x + y * y + z * z - 9.75 * 9.75;
            BN = (float) Math.sqrt(tmp);
            if (!(BN >= 0)) {
                BN = 0;
            }

           // if (BN > 5) {
            //    vibrator.vibrate(100);
           // }
            if(TI_sensor == 1)
            {
                BN = (float)Math.sqrt(BN);
            }
           textViewX.setText("x: " + x);
           textViewY.setText("y: " + y);
            textViewZ.setText("z: " + z);

        }

    public float dataRead(String key){
        try{
            myContext = createPackageContext("com.example.ti.ble.sensortag", 0);

            SharedPreferences preferencesReader = myContext.getSharedPreferences("Myankle_IT_sensor", Context.MODE_MULTI_PROCESS);
            float savedValueInWriterProcess = preferencesReader.getFloat(key,0f);
            Log.d("Mshzhb","Received :"+key+" : "+savedValueInWriterProcess);
            if(savedValueInWriterProcess!=0)
            return savedValueInWriterProcess;
            else
            {
                if (key.equals("acc_x")) return x;

                else if (key.equals("acc_y")) return y;

                else return z;
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Not data shared", e.toString());
        }
        if (key.equals("acc_x")) return x;

        else if (key.equals("acc_y")) return y;

        else return z;
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

       // if (BN < 3)
        //    return (BN / 6f * 0.7f);
        //else
            return  BN;


    }

    protected void onDestroy()
    {
        super.onDestroy();
    }



}
