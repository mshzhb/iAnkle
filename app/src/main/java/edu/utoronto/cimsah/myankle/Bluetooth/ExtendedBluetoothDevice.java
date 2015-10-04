/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 *
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/

/*
 * NORDIC SEMICONDUTOR EXAMPLE CODE AND LICENSE AGREEMENT
 *
 * You are receiving this document because you have obtained example code ("Software") 
 * from Nordic Semiconductor ASA * ("Licensor"). The Software is protected by copyright 
 * laws and international treaties. All intellectual property rights related to the 
 * Software is the property of the Licensor. This document is a license agreement governing 
 * your rights and obligations regarding usage of the Software. Any variation to the terms 
 * of this Agreement shall only be valid if made in writing by the Licensor.
 * 
 * == Scope of license rights ==
 * 
 * You are hereby granted a limited, non-exclusive, perpetual right to use and modify the 
 * Software in order to create your own software. You are entitled to distribute the 
 * Software in original or modified form as part of your own software.
 *
 * If distributing your software in source code form, a copy of this license document shall 
 * follow with the distribution.
 *   
 * The Licensor can at any time terminate your rights under this license agreement.
 * 
 * == Restrictions on license rights ==
 * 
 * You are not allowed to distribute the Software on its own, without incorporating it into 
 * your own software.  
 * 
 * You are not allowed to remove, alter or destroy any proprietary, 
 * trademark or copyright markings or notices placed upon or contained with the Software.
 *     
 * You shall not use Licensor's name or trademarks without Licensor's prior consent.
 * 
 * == Disclaimer of warranties and limitation of liability ==
 * 
 * YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THE SOFTWARE IS AT YOUR OWN RISK AND THAT THE 
 * SOFTWARE IS PROVIDED *AS IS" WITHOUT ANY WARRANTIES OR CONDITIONS WHATSOEVER. NORDIC SEMICONDUCTOR ASA 
 * DOES NOT WARRANT THAT THE FUNCTIONS OF THE SOFTWARE WILL MEET YOUR REQUIREMENTS OR THAT THE 
 * OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR FREE. YOU ASSUME RESPONSIBILITY FOR 
 * SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE *USE AND THE RESULTS 
 * OBTAINED FROM THE SOFTWARE.
 * 
 * NORDIC SEMICONDUCTOR ASA DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO WARRANTIES RELATED TO: NON-INFRINGEMENT, LACK OF VIRUSES, ACCURACY OR COMPLETENESS OF RESPONSES 
 * OR RESULTS, IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL OR 
 * CONSEQUENTIAL DAMAGES OR FOR ANY DAMAGES WHATSOEVER (INCLUDING BUT NOT LIMITED TO DAMAGES FOR 
 * LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, PERSONAL INJURY, 
 * LOSS OF PRIVACY OR OTHER PECUNIARY OR OTHER LOSS WHATSOEVER) ARISING OUT OF USE OR INABILITY TO 
 * USE THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * REGARDLESS OF THE FORM OF ACTION, NORDIC SEMICONDUCTOR ASA AGGREGATE LIABILITY ARISING OUT OF 
 * OR RELATED TO THIS AGREEMENT SHALL NOT EXCEED THE TOTAL AMOUNT PAYABLE BY YOU UNDER THIS AGREEMENT. 
 * THE FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE MAXIMUM EXTENT ALLOWED BY 
 * APPLICABLE LAW.
 * 
 * == Dispute resolution and legal venue ==
 * 
 * Any and all disputes arising out of the rights and obligations in this license agreement shall be 
 * submitted to ordinary court proceedings. You accept the Oslo City Court as legal venue under this agreement.
 * 
 * This license agreement shall be governed by Norwegian law.
 * 
 * == Contact information ==
 * 
 * All requests regarding the Software or the API shall be directed to: 
 * Nordic Semiconductor ASA, P.O. Box 436, Skøyen, 0213 Oslo, Norway.
 * 
 * http://www.nordicsemi.com/eng/About-us/Contact-us
 */
package edu.utoronto.cimsah.myankle.Bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Wrapper class to encapsulate a BluetoothDevice object, and various parameters such as
 * the Name, RSSI (signal strength) and Bluetooth Pairing information associated with the
 * corresponding device.
 */
public class ExtendedBluetoothDevice implements Parcelable {
    
    // Local BluetoothDevice object associated with an instance of this class
    public BluetoothDevice bluetoothDevice = null;
    
    // Device parameters
    public String name = null;
    public int rssi = -1;
    public boolean isBonded = false;
    
    // Class constructor with all device parameters
    public ExtendedBluetoothDevice(BluetoothDevice device, String name, int rssi, boolean isBonded) {
        
        // Set the data members
        this.bluetoothDevice = device;
        this.name = name;
        this.rssi = rssi;
        this.isBonded = isBonded;
    }
    
    /* Class constructor to facilitate implementation of the Parcelable interface. Note:
     * The values must be read from the parcel in the order in which they were written */
    protected ExtendedBluetoothDevice(Parcel in) {
        
        // Set the data members
        bluetoothDevice = (BluetoothDevice) in.readValue(BluetoothDevice.class.getClassLoader());
        name = in.readString();
        rssi = in.readInt();
        isBonded = (in.readInt() == 1);
    }
    
    @Override
    // Override the default object comparator method
    public boolean equals(Object o) {
        
        if (o instanceof ExtendedBluetoothDevice) {
            
            final ExtendedBluetoothDevice other = (ExtendedBluetoothDevice) o;
            return bluetoothDevice.getAddress().equals(other.bluetoothDevice.getAddress());
        }
        
        return super.equals(o);
    }
    
    /** 
     * Static comparator class to allow object comparisons for data-structures (List, 
     * for instance), which use the parameter's equals method (not the objects').
     */
    public static class AddressComparator {
        
        public String address = null;
        
        @Override
        public boolean equals(Object o) {
            
            if (o instanceof ExtendedBluetoothDevice) {
                
                final ExtendedBluetoothDevice other = (ExtendedBluetoothDevice) o;
                return address.equals(other.bluetoothDevice.getAddress());
            }
            return super.equals(o);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
        
        // Write the data values to the parcel
        out.writeValue(bluetoothDevice);
        out.writeString(name);
        out.writeInt(rssi);
        out.writeInt(isBonded ? 1 : 0);
    }
    
    // Static class to recreate the object. Part of the Parcelable implementation
    public static final Parcelable.Creator<ExtendedBluetoothDevice> CREATOR = new 
            Parcelable.Creator<ExtendedBluetoothDevice>() {
        
        @Override
        public ExtendedBluetoothDevice createFromParcel(Parcel in) {
            return new ExtendedBluetoothDevice(in);
        }
        
        @Override
        public ExtendedBluetoothDevice[] newArray(int size) {
            return new ExtendedBluetoothDevice[size];
        }
    };
}