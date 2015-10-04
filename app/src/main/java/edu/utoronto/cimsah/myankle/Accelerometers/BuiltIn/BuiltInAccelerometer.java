package edu.utoronto.cimsah.myankle.Accelerometers.BuiltIn;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;

@SuppressWarnings("unused")
public class BuiltInAccelerometer extends Accelerometer implements SensorEventListener {
	
	private SensorManager mSensorManager = null;
	private AccelerometerListener mAccelerometerListener = null;
	
	// Instantiate BuiltInAccelerometer
	public BuiltInAccelerometer(Context context) {		
		
		// Initialize the SensorManager object
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	public boolean isConnected() {
		return true;
 	}
	
	@Override
	public Accelerometer registerListenerAndConnect(AccelerometerListener listener) {
		
		// Set the accelerometer-listener to the parametrized value
		mAccelerometerListener = listener;
		return this;
	}
	
	@Override 
	public void start() {
		
		// Configure the listener to receive call-backs from the default (in-built) accelerometer
		Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
	}
	
	@Override
	public void stop() {
		
		// Unregister the listener
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	public void unregisterListenerAndDisconnect(AccelerometerListener listener) {
		
		// Invalidate the accelerometer-listener
		if(mAccelerometerListener == listener) {
			mAccelerometerListener = null;
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		// Check whether the callback was triggered by an accelerometer event
		if(mAccelerometerListener != null && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			
			// Propagate the callback to the class implementing it, through the interface method
			mAccelerometerListener.onAccelerometerEvent(event.values);
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
