package edu.utoronto.cimsah.myankle;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.utoronto.cimsah.myankle.Bluetooth.ListAdapterDevice;
import edu.utoronto.cimsah.myankle.Bluetooth.ExtendedBluetoothDevice;
import edu.utoronto.cimsah.myankle.Bluetooth.ExtendedBluetoothDevice.AddressComparator;
import edu.utoronto.cimsah.myankle.Bluetooth.ScannerServiceParser;
import edu.utoronto.cimsah.myankle.Helpers.PrefUtils;
import edu.utoronto.cimsah.myankle.R.color;

@SuppressWarnings("all")
public class FragmentScannerDialog extends DialogFragment implements OnClickListener, 
        OnItemClickListener {
    
    public static final String TAG = FragmentScannerDialog.class.getSimpleName();
    
    // The call-back ID for this dialog. Note: Increment in other dialog fragments
    public static final int ID = 1;
    
    // Request codes with which to launch child activities
    public static final int BLUETOOTH_REQUEST_CODE = 1;
    
    // Bundle argument-strings
    private static final String ARG_PARAM_UUID = "param_uuid";
    private static final String ARG_IS_CUSTOM_UUID = "is_custom_uuid";
    
    // Bundle argument-strings for the callback (return values)
    public static final String RETURN_DEVICE_NAME = "return_device_name";
    
    // Layout objects
    private Button mButtonCancel;
    private Button mButtonScan;
    private ListView mListExternal;
    private LinearLayout mLayoutInternal;
    
    // Bluetooth-related primitives and objects
    private UUID[] mUuid = null;
    private boolean mIsCustomUuid = false;
    private static final long SCAN_DURATION = 5000;
    private BluetoothAdapter mBluetoothAdapter = null;
    
    // Miscellaneous objects
    private boolean mIsScanning = false;
    private Handler mHandler = new Handler();
    
    // List-related objects
    private ListAdapterDevice mExternalAdapter = null;
    private ArrayList<ExtendedBluetoothDevice> mExternalDeviceList = new ArrayList<>();
    private AddressComparator mComparator = new AddressComparator();
    
    // Create an instance of the interface
    private CustomDialogFinishListener mDialogFinishListener = null;
    
    // Instantiate FragmentScannerDialog
    public static FragmentScannerDialog newInstance(UUID[] uuids, boolean isCustomUUID) {
        
        FragmentScannerDialog myFragment = new FragmentScannerDialog();
        final ParcelUuid[] pUuids = new ParcelUuid[uuids.length];
        
        // Copy the parametrized vector into a parcelable array
        for(int i = 0; i < uuids.length; i++) {
            pUuids[i]= new ParcelUuid(uuids[i]);
        }
        
        // Create and populate a new bundle to instantiate the fragment with
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_CUSTOM_UUID, isCustomUUID);
        args.putParcelableArray(ARG_PARAM_UUID, pUuids);
        myFragment.setArguments(args);
        
        return myFragment;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        /* Ensure that the activity exists and has implemented the
         * fragment's callback interface. Else, throw an exception */
        if(activity instanceof CustomDialogFinishListener) {
            mDialogFinishListener = (CustomDialogFinishListener) activity;
            
        } else if(BuildConfig.DEBUG) {
            throw new ClassCastException();
        }
    }
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fetch the bundled arguments
        final Bundle args = getArguments();
        mIsCustomUuid = args.getBoolean(ARG_IS_CUSTOM_UUID, false);
        
        // If a list of custom UUIDs are provided, fetch them from the argument bundle
        if (mIsCustomUuid) {
            
            final ParcelUuid[] pu = (ParcelUuid[]) args.getParcelableArray(ARG_PARAM_UUID);
            mUuid = new UUID[pu.length];

            for(int i= 0; i < pu.length; i++) {
                mUuid[i]= pu[i].getUuid();
            }
        }
        
        // Retrieve a reference to the Bluetooth manager and its adapter
        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Fetch a reference to the created dialog
        Dialog thisDialog = super.onCreateDialog(savedInstanceState);

        // Set the dialog parameters (windows preferences, animations)
        thisDialog.getWindow().requestFeature(STYLE_NORMAL);
        thisDialog.getWindow().setTitle("Available Devices");
        
        return thisDialog;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        // Inflate the fragment's layout file
        View view = inflater.inflate(R.layout.fragment_scanner_dialog, container, false);
        
        // Link the layout elements to the corresponding UI elements
        mButtonScan = (Button) view.findViewById(R.id.fragment_scanner_dialog_button_scan);
        mButtonCancel = (Button) view.findViewById(R.id.fragment_scanner_dialog_button_cancel);
        mListExternal = (ListView) view.findViewById(R.id.fragment_scanner_dialog_list_view_external);
        mLayoutInternal = (LinearLayout) view.findViewById(R.id.fragment_scanner_dialog_layout_list_internal);
        
        /* Inflate the child view (the scanner-list row layout) in the placeholder. This
         * is used to simulate the "Inbuilt" device row without creating a new adapter */
        View childRow = inflater.inflate(R.layout.layout_scanner_list_row, null);
        mLayoutInternal.addView(childRow);
        
        // Configure the views inside the child-view
        ((TextView) childRow.findViewById(R.id.layout_scanner_list_row_text_name)).setText("Inbuilt");
        ((TextView) childRow.findViewById(R.id.layout_scanner_list_row_text_address)).setText("Internal Accelerometer");
        
        // Initialize the list-adapters for the external device-list
        mExternalAdapter = new ListAdapterDevice(getActivity(), 
                R.layout.layout_scanner_list_row, 
                mExternalDeviceList
        );
        
        // Link the list-adapters to the corresponding list-views
        mListExternal.setAdapter(mExternalAdapter);
        
        // Register the event-listeners
        mButtonScan.setOnClickListener(this);
        mButtonCancel.setOnClickListener(this);
        mLayoutInternal.setOnClickListener(this);
        mListExternal.setOnItemClickListener(this);
        
        // Simulate a 'Scan' button-click. This starts scanning without requiring user input
        mButtonScan.performClick();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        
        // Stop any previously initiated scans
        stopScan();
        
        super.onDestroyView();
    }

    @Override
    public void onClick(View view) {
        
        switch (view.getId()) {
            
            // The 'Cancel' button is clicked
            case R.id.fragment_scanner_dialog_button_cancel:
                
                // Stop the previously initiated scan
                stopScan();
                
                // Close the dialog
                getDialog().cancel();
                
                break;
            
            // The 'Scan' button is clicked
            case R.id.fragment_scanner_dialog_button_scan: 
                
                // Check whether Bluetooth is enabled
                if(mBluetoothAdapter.isEnabled()) {
                    startScan();
                    
                // Else, request the user to enable it
                } else {
                    
                    // Create an intent to enable Bluetooth
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE);
                }
                
                break;
            
            // The 'Inbuilt' device layout is clicked
            case R.id.fragment_scanner_dialog_layout_list_internal:

                // Reset the Shared Preference corresponding to the device's MAC Address
                PrefUtils.setStringPreference(getActivity(), PrefUtils.MAC_ADDRESS_KEY, null);

                // Output the name of the selected device
                if(BuildConfig.DEBUG) Log.d(TAG, "Name: Internal Accelerometer");
                
                // Initiate the callback indicating successful completion
                Bundle bundle = new Bundle();
                bundle.putString(RETURN_DEVICE_NAME, "Internal Accelerometer");
                mDialogFinishListener.onFinish(FragmentScannerDialog.ID, bundle);
                
                // Dismiss this dialog
                getDialog().dismiss();
                
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        
        switch (adapter.getId()) {
            
            // An item in the external-devices ListView is clicked
            case R.id.fragment_scanner_dialog_list_view_external:
                
                /* If the internal-device layout is simultaneously pressed, stop executing.
                 * This is to ensure that clicks on the two views are mutually exclusive */
                if(mLayoutInternal.isPressed()) {
                    return;
                }
                
                // Fetch the corresponding ExtendedBluetoothDevice object
                ExtendedBluetoothDevice device = (ExtendedBluetoothDevice) adapter.getItemAtPosition(position);
                
                // Update the Shared Preference corresponding to the bluetooth device
                PrefUtils.setStringPreference(
                        getActivity(),
                        PrefUtils.MAC_ADDRESS_KEY,
                        device.bluetoothDevice.getAddress()
                );

                // Output the name the selected device
                if(BuildConfig.DEBUG) Log.d(TAG, "Name: " + device.name);
                
                // Initiate the callback indicating successful completion
                Bundle bundle = new Bundle();
                bundle.putString(RETURN_DEVICE_NAME, device.name);
                mDialogFinishListener.onFinish(FragmentScannerDialog.ID, bundle);
                
                // Dismiss this dialog
                getDialog().dismiss();
                
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        switch (requestCode) {
            
            // The Bluetooth activity returned with a response
            case BLUETOOTH_REQUEST_CODE:
                
                // Bluetooth was enabled by the user (activity completed successfully)
                if(resultCode == Activity.RESULT_OK) {
                    
                    /* Simulate a 'Scan' button-click. Note: This is not a cylic 
                     * operation since bluetooth is guaranteed to be enabled */
                    mButtonScan.performClick();
                 }
        }
    }

    /**
     * Initiates a Bluetooth LE scan to find and display neighbouring Bluetooth devices in a 
     * separate thread, for the specified interval. If scanning with a custom UUID, filters 
     * any unspecified devices. 
     */
    private void startScan() {
        
        // Sanity check: If already in scanning mode or bluetooth is disabled, return to the caller
        if(mIsScanning || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        
        // Clear the list of previously displayed devices. Also update the adapter
        mIsScanning = true;
        mExternalDeviceList.clear();
        mExternalAdapter.notifyDataSetChanged();
        
        // Modify the appearance of the buttons to indicate the scanning mode
        mButtonScan.setEnabled(false);
        mButtonScan.setBackgroundColor(getResources().getColor(color.light_gray));
        mButtonScan.setText("Scanning...");
        
        // Configure the scanning parameters
        ScanSettings settings = new ScanSettings.Builder().setScanMode(
                ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        List<ScanFilter> filters = new ArrayList<>();
        
        /* Initiate the BluetoothLE scan. Note: Since scanning based on UUID is unstable (see
         * Javadocs for bleScanCallback below), filtering is deferred until the devices are to
         * be added to the list */
        BluetoothLeScanner bleScanner = mBluetoothAdapter.getBluetoothLeScanner();         
        bleScanner.startScan(filters, settings, bleScanCallback);
        
        // Continue scanning for the specified duration
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {

                if (mIsScanning) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
     }

    /**
     * Stops any previously initiated BluetoothLE scan
     */
    private void stopScan() {
        
        // Sanity check: If not currently scanning, return to the caller
        if(!mIsScanning || !mBluetoothAdapter.isEnabled()) {
            return;
        }
        
        // Terminate the BLE scan
        BluetoothLeScanner bleScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bleScanner.stopScan(bleScanCallback);
        
        // Modify the appearance of the buttons to indicate the scanning mode
        mButtonScan.setEnabled(true);
        mButtonScan.setBackgroundColor(getResources().getColor(R.color.blue));
        mButtonScan.setText("Scan");
        
        // Update the scanning mode
        mIsScanning = false;
    }
    
    /**
     * Anonymous class to handle callbacks resulting from BLE scans. Note: In Android, scanning 
     * based on UUID is still experimental as of API 22. Use the decodeDevice...() method in 
     * ScannerServiceParser.java to decode and filter devices based on service UUIDs.
     */
    private ScanCallback bleScanCallback = new ScanCallback() {
        
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            
            // Retrieve the returned BluetoothDevice object
            BluetoothDevice device = result.getDevice();
            
            /* Sanity check: Ensure that the parametrized result is valid 
             * before attempting to do any further processing on the object */
            if(device == null) {
                return;
            }
            
            // Fetch the device parameters
            int deviceRssi = result.getRssi();
            String deviceName = device.getName();
            byte[] scanRecord = result.getScanRecord().getBytes();
            boolean isBonded = (device.getBondState() == BluetoothDevice.BOND_BONDED);
            
            // If a set of custom UUIDs are to be used to filter the scan results
            if(mIsCustomUuid && mUuid != null) {
                
                try {
                    
                    // Check whether the returned device is to be added to the list
                    if(ScannerServiceParser.decodeDeviceAdvData(scanRecord, mUuid)) {
                                                
                        // Construct a new ExtendedBLuetoothDevice object and populate it
                        ExtendedBluetoothDevice extendedDevice = 
                                new ExtendedBluetoothDevice(device, 
                                                            deviceName, 
                                                            deviceRssi, 
                                                            isBonded);
                        
                        // Call the helper method to update the list
                        addOrUpdateDevice(extendedDevice);
                    }
                    
                // In debug mode, display an appropriate error
                } catch (Exception e) {
                    if(BuildConfig.DEBUG) e.printStackTrace();
                }
                
            // If filtering based on UUIDs is disabled, add the device to the list    
            } else {
                
                // Construct a new ExtendedBLuetoothDevice object and populate it
                ExtendedBluetoothDevice extendedDevice = 
                        new ExtendedBluetoothDevice(device, 
                                                    deviceName, 
                                                    deviceRssi, 
                                                    isBonded);

                // Call the helper method to update the list
                addOrUpdateDevice(extendedDevice);
            }
        }
    };
    
    /**
     * Helper method to facilitate adding bluetooth devices to the array-list. Accounts
     * for duplicates by updating the parameters of existing devices. Note: Devices with
     * identical MAC addresses are considered duplicates.
     * 
     * @param newDevice The ExtendedBluetoothDevice object to add/update in the list
     */
    private void addOrUpdateDevice(ExtendedBluetoothDevice newDevice) {
        
        // Sanity check: Ensure that the parametrized object is valid
        if(newDevice == null) {
            return;
        }
        
        // Search for a duplicate entry in the list
        mComparator.address = newDevice.bluetoothDevice.getAddress();
        int duplicateIndex = mExternalDeviceList.indexOf(mComparator);
        
        // If a list-entry corresponding to the device exists
        if(duplicateIndex >= 0) {
            
            // Fetch a reference to the existing device entry
            ExtendedBluetoothDevice existingDevice = mExternalDeviceList.get(duplicateIndex);
            
            // Update the device parameters
            existingDevice.name = newDevice.name;
            existingDevice.rssi = newDevice.rssi;
            existingDevice.isBonded = newDevice.isBonded;
            
        // If there is no list-entry corresponding to the parametrized device    
        } else {
            
            // Add the device to the list
            mExternalDeviceList.add(newDevice);
        }
        
        // Update the underlying list-adapter and refresh the list
        mExternalAdapter.notifyDataSetChanged();
    }
    
    // An interface to communicate with the parent activity through a callback
    public interface CustomDialogFinishListener {
        
        void onFinish(int id, Bundle bundle);
    }
}
