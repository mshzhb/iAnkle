package edu.utoronto.cimsah.myankle.Accelerometers;

import android.content.Context;

import edu.utoronto.cimsah.myankle.Accelerometers.BuiltIn.BuiltInAccelerometer;
import edu.utoronto.cimsah.myankle.Accelerometers.MetaWear.MetawearAccelerometer;
import edu.utoronto.cimsah.myankle.Game.MainActivity;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;

@SuppressWarnings("unused")
/** Static class to manage available accelerometer devices */
public class AccelerometerManager {
	
	// Available accelerometer devices
	final public static int TYPE_NATIVE_PHONE = 1;
	final public static int TYPE_METAWEAR = 2;
	
	// Fully static class, user should not be able to instantiate
	private AccelerometerManager() {}

	/** 
	 * Returns an instance of the Accelerometer class (any of its children) based on 
	 * the parametrized device ID. Note: Connections or configuration of call-backs should
	 * not be done here. These should be explicitly handled by the calling method on the
	 * returned Accelerometer object.
	 * 
	 * @param context: The activity context
	 */
	public static Accelerometer get(Context context) {
		
		// Variable declaration
		int deviceID = TYPE_NATIVE_PHONE;
		MainActivity.device = TYPE_NATIVE_PHONE;
		String deviceAddress = PrefUtils.getStringPreference(context, PrefUtils.MAC_ADDRESS_KEY);
		
		/* If the MAC Address is valid, select the appropriate BLE accelerometer. Note: To identify
		 * the type of device (if required), use the Service UUID of the manufacturer (can be
		 * recorded in FragmentScannerDialog) */
		//TODO: Check UUID instead of null/nonnull
		if(deviceAddress != null) {
			deviceID = TYPE_METAWEAR;
			MainActivity.device = TYPE_METAWEAR;
		}
		
		// Select the relevant Accelerometer based on the parametrized device ID
		switch(deviceID)
		{
			/* Consider maintaining a reference to recently used Accelerometers. 
			 * May eliminate overheads due to starting/stopping various services */
 			case TYPE_NATIVE_PHONE: return new BuiltInAccelerometer(context);
			case TYPE_METAWEAR: return new MetawearAccelerometer(context, deviceAddress);
			
			// By default, return the built-in accelerometer
			default : return new BuiltInAccelerometer(context);
		}
	}

	//TODO: Function to convert UUID to device types
}
