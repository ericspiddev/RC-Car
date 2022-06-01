package com.example.bluetooth;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BluetoothLeService extends Service {

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;


    private int connStatus = STATE_DISCONNECTED;
    private BluetoothManager manageBluetooth;
    private BluetoothAdapter myAdapter;
    private BluetoothGatt myGatt;

    private final static String TAG = "Bluetooth_Service";


    public class LocalBinder extends Binder {
        BluetoothLeService getService()
        {
            return BluetoothLeService.this;
        }
    }
    private final IBinder myBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        return myBinder;
    }

    public boolean initialize() // fxn to initialize bluetooth in app
    {
        if(manageBluetooth == null) // if we have not set our bluetooth manager yet
        {
            manageBluetooth = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); // try to grab the manager from system context
            if(manageBluetooth == null) // if the manager is null then
            {
                Log.e(TAG, "Bluetooth manager unavailable"); // log fail
                return false; // return false to indicate we did not init
            }
        }
        myAdapter = manageBluetooth.getAdapter(); // if we succeed get the phone's bluetooth adapter
        if(myAdapter == null) // if there is no adapter
        {
            Log.e(TAG, "Bluetooth adapter unavailable"); // log that we cannot use bluetooth
            return false; // return false
        }

        return true; // if we make it here we have the bluetooth adapter!
    }

    public boolean connect(final String hardwareAddress)
    {
        if(myAdapter == null || hardwareAddress == null) // if we have no adapter or passed in str is null
        {
            Log.w(TAG, "Bluetooth adapter not intialized or address is null");
            return false;
        }

        final BluetoothDevice dev = myAdapter.getRemoteDevice(hardwareAddress);
        if(dev == null) // if we cannot get the remote device address
        {
            Log.w(TAG, "Device cannot be found, Unable to connect.");
            return false;
        }

        //myGatt = dev.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new Gatt Connection!");
        return true;

    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int stat, int newState)
        {
            if(newState == BluetoothProfile.STATE_CONNECTED) // if we are connecting to bluetooth
            {
                connStatus = STATE_CONNECTED;

                Log.i(TAG, "Connected to Gatt Server");
                Log.i(TAG, "Attempting to start service discovery");
                  //    +  myGatt.());
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(TAG, "Service disconnected from Gatt Sever");
            }
        }

    };
}
