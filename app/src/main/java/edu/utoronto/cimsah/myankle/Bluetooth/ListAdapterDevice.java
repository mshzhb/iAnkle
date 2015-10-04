package edu.utoronto.cimsah.myankle.Bluetooth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import edu.utoronto.cimsah.myankle.R;

public class ListAdapterDevice extends ArrayAdapter<ExtendedBluetoothDevice> {
    
    // Adapter-related objects
    private int mLayoutResource = 0;
    private Context mContext = null;
    private ArrayList<ExtendedBluetoothDevice> mDevicesList = null;
    
    public ListAdapterDevice(Context context, int resource, ArrayList<ExtendedBluetoothDevice> devices) {
        super(context, resource, devices);
        
        mContext = context;
        mLayoutResource = resource;
        mDevicesList = devices;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        View row = convertView;
        ViewHolder viewHolder;
        
        if(row == null) {

            // Get the layout inflater
            LayoutInflater inflater = LayoutInflater.from(mContext);

            // Inflate a child container from the resource file
            row = inflater.inflate(mLayoutResource, null);
            
            /* Instantiate the ViewHolder class and populate it with the child 
             * views. Link (cache) the child UI elements to the holder object */
            viewHolder = new ViewHolder();
            viewHolder.textName = (TextView) row.findViewById(R.id.layout_scanner_list_row_text_name);
            viewHolder.textAddress = (TextView) row.findViewById(R.id.layout_scanner_list_row_text_address);
            viewHolder.imageBonded = (ImageView) row.findViewById(R.id.layout_scanner_list_row_image_bonded);
            
            row.setTag(viewHolder);
            
        } else {
            
            // Get the cached holder object associated with the view
            viewHolder = (ViewHolder) row.getTag();            
        }
        
        // Fetch the corresponding ExtendedBluetoothDevice object at this position
        ExtendedBluetoothDevice device = mDevicesList.get(position);
        
        // If the name is invalid, reset it
        if(device.name == null || device.name.isEmpty()) {
            device.name = "Unknown";
        }

        // Device parameters
        String name = device.name;
        String address = device.bluetoothDevice.getAddress();
        boolean isBonded = device.isBonded;
        
        // Set the TextViews associated with this device
        viewHolder.textName.setText(name);
        viewHolder.textAddress.setText(address);
        
        // If the device is bonded (paired via Bluetooth)
        if(isBonded) {

            // Set the ImageView associated with this device (indicates that the device is paired)
            viewHolder.imageBonded.setImageResource(R.drawable.ic_bluetooth_bonded);
            
        // Removes any previously attached bitmap
        } else {
            viewHolder.imageBonded.setImageBitmap(null);
        }
        
        return row;
    }

    /**
     * The adapter implements the ViewHolder pattern to reduce overheads resulting
	 * from expensive findViewById() calls. The child views (layout objects) are 
	 * cached, resulting in faster rendering of the list 
     */
    private static class ViewHolder
    {
        TextView textName;
        TextView textAddress;
        ImageView imageBonded;
    }
}
