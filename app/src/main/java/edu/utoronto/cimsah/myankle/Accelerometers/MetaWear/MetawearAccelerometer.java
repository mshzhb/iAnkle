package edu.utoronto.cimsah.myankle.Accelerometers.MetaWear;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.mbientlab.metawear.api.MetaWearBleService;
import com.mbientlab.metawear.api.MetaWearController;
import com.mbientlab.metawear.api.Module;
import com.mbientlab.metawear.api.util.BytesInterpreter;

import edu.utoronto.cimsah.myankle.Accelerometers.Accelerometer;
import edu.utoronto.cimsah.myankle.BuildConfig;

@SuppressWarnings("unused")
public class MetawearAccelerometer extends Accelerometer implements ServiceConnection {
	
	public static final String TAG = MetawearAccelerometer.class.getSimpleName();
	
	private Context mContext = null;
	private MetaWearBleService mMetaWearBleService = null;
	private MetaWearController mMetaWearController = null;
	private AccelerometerListener mAccelerometerListener = null;
	
	// MetaWear-specific accelerometer configuration
	byte[] config = new byte[] {0, 0, 0x20, 0, 0};
	private String MAC_Address = null;
	
	// Instantiate MetawearAccelerometer
	public MetawearAccelerometer(Context context, String deviceAddress) {
		
		// Save a reference to the parametrized context
		mContext = context;
		MAC_Address = deviceAddress;
	}
	
	// Instantiate the MetaWear class used to handle accelerometer-related call-backs
	private com.mbientlab.metawear.api.controller.Accelerometer.Callbacks mAccelerometerCallbacks =
			new com.mbientlab.metawear.api.controller.Accelerometer.Callbacks() {
				
				@Override
				public void receivedDataValue(short x, short y, short z) {
					
					// Set the values to propagate through the call-back
					float[] values = new float[3];
					
					values[0] = (float) (BytesInterpreter.bytesToGs(config, x) * 9.8);
					values[1] = (float) (BytesInterpreter.bytesToGs(config, y) * 9.8);
					values[2] = (float) (BytesInterpreter.bytesToGs(config, z) * 9.8);
					
					// Propagate the callback if the listener is valid and the device is connected
					if(mAccelerometerListener != null && isConnected()) {
						mAccelerometerListener.onAccelerometerEvent(values);
					}
				}
			};
	
	// Instantiate the MetaWear class used to handle device-related call-backs
	private MetaWearController.DeviceCallbacks mDeviceCallbacks = 
			new MetaWearController.DeviceCallbacks() {
				
				@Override
				public void connected() {

					// Initiate the appropriate callback
					if(mAccelerometerListener != null) {
						mAccelerometerListener.onAccelerometerConnected(true, MAC_Address);
					}
					
					// Debug message: Print an appropriate message
					if(BuildConfig.DEBUG) Log.d(TAG, "MetaWear device connected!");
				}
				
				@Override
				public void disconnected() {
					
					// If the device is disconnected, initiate the corresponding callback
					if(mAccelerometerListener != null) {
						mAccelerometerListener.onAccelerometerDisconnected();
					}
					
					// Debug message: Print an appropriate error message
					if(BuildConfig.DEBUG) Log.e(TAG, "MetaWear device disconnected unexpectedly!");
				}
			};
	
	@Override
	public boolean isConnected() {
		
		// If the MetaWear service and controller are both valid
		if(mMetaWearBleService != null && mMetaWearController != null) {

			// Return whether the MetaWear device is connected
			return mMetaWearController.isConnected();
		}
		
		return false;
	}
	
	@Override
	public Accelerometer registerListenerAndConnect(AccelerometerListener listener) {

		// Set the accelerometer listener to the parametrized value
		mAccelerometerListener = listener;
		
		/* Bind the MetaWear service to the application. Successful completion is 
		 * acknowledged by the onServiceConnected callback method (implemented below) */
		mContext.getApplicationContext().bindService(new Intent(mContext,
				MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);
		
		return this;
	}
	
	@Override
	public void start() {
		
		/* Sanity check: Ensure that the device is connected before continuing. Note: 
		 * If this method is called in an activity/fragment lifecycle-method, it is 
		 * possible (and acceptable) that the device is not connected by then */
		if(!isConnected()) {
			
			Log.d(TAG, "In start(), accelerometer may have been expected to be connected");
			return;
		}
		
		// Retrieve a reference to the device's accelerometer controller
		com.mbientlab.metawear.api.controller.Accelerometer accelerometerController =
				(com.mbientlab.metawear.api.controller.Accelerometer) mMetaWearController.
						getModuleController(Module.ACCELEROMETER);
		
		// Configure the accelerometer sampling rate and type
		accelerometerController.enableXYZSampling()

				.withFullScaleRange(com.mbientlab.metawear.api.controller.Accelerometer.
						SamplingConfig.FullScaleRange.FSR_8G)

				.withOutputDataRate(com.mbientlab.metawear.api.controller.Accelerometer.
						SamplingConfig.OutputDataRate.ODR_100_HZ);

		// Start the accelerometer
		accelerometerController.startComponents();
	}
	
	@Override
	public void stop() {
		
		// Sanity check: Ensure that the device is connected
		if(!isConnected()) {
			return;
		}
		
		// Retrieve a reference to the device's accelerometer controller
		com.mbientlab.metawear.api.controller.Accelerometer accelerometerController =
				(com.mbientlab.metawear.api.controller.Accelerometer) mMetaWearController.
						getModuleController(Module.ACCELEROMETER);

		// Disable sampling and stop the accelerometer
		accelerometerController.disableAllDetection(true);
		accelerometerController.stopComponents();
	}
	
	@Override 
	public void unregisterListenerAndDisconnect(AccelerometerListener listener) {
		
		// Implicitly stop the accelerometer device
		this.stop();
		
		// If the accelerometer-listener is already invalid, return to the caller
		if(mAccelerometerListener == null) {
			return;
			
		// Else, invalidate the listener
		} else if (mAccelerometerListener == listener) {
			mAccelerometerListener = null;
		}
		
		// Unregister all persistent callbacks and disconnect the device (without triggering a callback)
		mMetaWearController.removeModuleCallback(mAccelerometerCallbacks);
		mMetaWearController.removeDeviceCallback(mDeviceCallbacks);
		mMetaWearController.close(false);
		
		try {
			
			// Attempt to unregister the broadcast-receiver
			mMetaWearBleService.unregisterReceiver(MetaWearBleService.getMetaWearBroadcastReceiver());
		
		} catch(Exception e) {
			// Ignore, this is a common occurrence
		}

		// Invalidate the controller and listener to catch incorrect use of the instance
		mMetaWearController = null;
		mAccelerometerListener = null;
		
		// Unbind the MetaWear BLE Service from the application
		mContext.getApplicationContext().unbindService(this);
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

		// Debug message
		Log.d(TAG, "In onServiceConnected, service connected!");
		
		// Fetch the MetaWear service from the binder and register the broadcast-receiver
		mMetaWearBleService = ((MetaWearBleService.LocalBinder) service).getService();
		mMetaWearBleService.registerReceiver(MetaWearBleService.getMetaWearBroadcastReceiver(),
				MetaWearBleService.getMetaWearIntentFilter());
		
		// Get the BluetoothDevice object corresponding to the MetaWear device
		BluetoothManager manager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothDevice device = manager.getAdapter().getRemoteDevice(MAC_Address);
		
		// Retrieve a reference to the MetaWear controller and register the callbacks
		mMetaWearController = mMetaWearBleService.getMetaWearController(device);
		mMetaWearController.addDeviceCallback(mDeviceCallbacks);
		mMetaWearController.addModuleCallback(mAccelerometerCallbacks);
		mMetaWearController.setRetainState(false);
		
		// Attempt to connect to the MetaWear device
		mMetaWearController.connect();
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {}
}