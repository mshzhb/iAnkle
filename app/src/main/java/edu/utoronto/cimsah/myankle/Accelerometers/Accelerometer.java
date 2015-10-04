package edu.utoronto.cimsah.myankle.Accelerometers;

@SuppressWarnings("unused")
public abstract class Accelerometer {

	/**
	 * Returns whether a connection is established with the device corresponding to this 
	 * Accelerometer object. If the device is already connected, returns true. 
	 * Else, returns false.
	 *
	 * @return boolean Whether or not the device is connected
	 */
	public abstract boolean isConnected();
	
	/**
	 * Registers any receivers and listeners associated with the accelerometer. Also attempts
	 * to connect to the device. This method should be invoked immediately after construction of 
	 * an Accelerometer instance. A successful connection is acknowledged by a callback 
	 * (onAccelerometerConnected). Any time-outs should be handled by the calling method.
	 * 
	 * @param listener The accelerometer event-listener to which call-backs should be propagated
	 * @return The Accelerometer instance on which the method was invoked
	 */
	public abstract Accelerometer registerListenerAndConnect(AccelerometerListener listener);

	/**
	 * Enables streaming of accelerometer data. Should be called after the onAccelerometerConnected 
	 * method, in onResume, or on starting data-collection. Accelerometer events trigger the
	 * onAccelerometerEvent callback.
	 */
	public abstract void start();
	
	/**
	 * Disables streaming of accelerometer data. Should be called in onPause, 
	 * or when data-collection ends.
	 */
	public abstract void stop();
	
	/**
	 * Unregisters any receivers and listeners associated with the accelerometer device. 
	 * Also disconnects the device and unbinds any persistent services (for instance, device-specific
	 * bluetooth services) the application is tied to. To continue using the same Accelerometer 
	 * instance after this method invocation, re-register the listener. Note: Does not provide a 
	 * callback.
	 * 
	 * @param listener The accelerometer event-listener for which call-backs should be unregistered
	 */
	public abstract void unregisterListenerAndDisconnect(AccelerometerListener listener);
	
	/** Interface to facilitate communication with measuring activities/fragments through call-backs */
	public interface AccelerometerListener {
		
		/** Called when the accelerometer device is connected */
		void onAccelerometerConnected(final boolean isBluetoothDevice, final String MAC_Address);
		
		/** Called when the accelerometer device is disconnected */
		void onAccelerometerDisconnected();
		
		/** Called when an event (abstraction of a sensor-event) is received from an accelerometer device
		 * @param values A floating-point array of accelerometer values (x, y, z) */
		void onAccelerometerEvent(final float[] values);
	}
}