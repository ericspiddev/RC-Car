package com.example.bluetooth;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.UUID;


@RequiresApi(api = Build.VERSION_CODES.O)
public class BluetoothBackground {

    private Context activityContext;
    private CompanionDeviceManager devManage;
    private BluetoothGatt btGatt;
    private BluetoothManager btManage;
    private BluetoothAdapter btAdapt;
    private BluetoothDevice esp32;

    private final byte[] FORWARD = convertToByteArray(1);
    private final byte[] BACKWARD = convertToByteArray(2);
    private final byte[] LEFT = convertToByteArray(4);
    private final byte[] RIGHT = convertToByteArray(8);
    private final byte[] SPEED = convertToByteArray(50);


    private final UUID serviceUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"); // do we need to use this? 00000000-0000-1000-8000-00805f9b34fb
    private final UUID txUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID rxUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private BluetoothGattService uartBtService;
    private BluetoothGattCharacteristic rxCharacteristic;
    private BluetoothGattCharacteristic txCharacteristic;

    private String TAG = "BACKGROUND";

    public BluetoothBackground(Context c, BluetoothManager manager)
    {
        this.activityContext = c;
        this.btManage = manager;
        btAdapt = btManage.getAdapter();
        if(!isBluetoothSupported())
            Log.e("BT", "Bluetooth not supported!");
    }

    public void goForward() {
        byte dir = 1;
        writeData(FORWARD);
    }

    public void goBackward() {
        byte dir = 2;
        writeData(BACKWARD);
    }

    public void goLeft() {
        writeData(LEFT);
    }

    public void goRight() {
        writeData(RIGHT);
    }

    public void writeData(byte[] direction) {
        if (btGatt != null && rxCharacteristic != null) {
            writeCharacteristic(btGatt, rxCharacteristic, direction);
        } else {
            Log.e("SENDDATA", "Please connect to bluetooth device first");
        }
    }

    public void setEsp32(BluetoothDevice esp32) {
        this.esp32 = esp32;
    }

    public void connectGatt()
    {
        if(esp32 != null && checkBtPermission())
        {
            esp32.connectGatt(activityContext, false, gattCallback, 2);
        }
    }

    public void readData()
    {
        readCharacteristic(btGatt, txCharacteristic);
    }

    private void getServiceAndCharacteristics(BluetoothGatt gatt) {
        if (gatt.getServices() == null)
        {
            Log.i("printGattTable", "No service or characteristics available, call " +
                    "discover services first?");
        }
        else
        {
            uartBtService = gatt.getService(serviceUUID);
            if(uartBtService != null)
            {
                rxCharacteristic = uartBtService.getCharacteristic(rxUUID);
                txCharacteristic = uartBtService.getCharacteristic(txUUID);
            }
        }
    }


    private void readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
    {
        if(gatt == null || characteristic == null)
        {
            Log.e("BACKEND", "gatt or characteristic is null");
            return;
        }
        if(isReadable(characteristic)){
            if(checkBtPermission())
                gatt.readCharacteristic(characteristic);
        }

    }
    private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] data) {
        if (isWritable(characteristic))
        {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(data);
            if(checkBtPermission())
            {
                Boolean test = gatt.writeCharacteristic(characteristic);
                Log.i("DATA", "write is " + test);
            }
        }
        else if(isWritableNoResponse(characteristic))
        {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(data);
            if(checkBtPermission())
            {
                gatt.writeCharacteristic(characteristic);
            }
        }
        else
        {
            Log.e("writeCharacteristic", "This bluetooth characteristic is not writable");
            return;
        }
    }

    private boolean isWritableNoResponse(BluetoothGattCharacteristic characteristic)
    {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0);
    }

    private boolean isWritable(BluetoothGattCharacteristic characteristic)
    {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
    }

    private boolean isReadable(BluetoothGattCharacteristic characteristic)
    {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    public BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            String devAddr = gatt.getDevice().getAddress();
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                if(newState == BluetoothProfile.STATE_CONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Connected to " + devAddr);
                    btGatt = gatt;
                    if(checkBtPermission()) {
                        btGatt.discoverServices();
                    }
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Disconnected from " + devAddr);
                    if(checkBtPermission()) {
                        btGatt.close();
                    }
                }
            }
            else{
                Log.w("BluetoothGattCallback", "Error " + status + " encountered for device"
                        + devAddr);
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            Log.w("BluetoothGattCallback", "Discovered " + gatt.getServices().size() + " for " +
                  gatt.getDevice().getAddress());
            getServiceAndCharacteristics(gatt);
        }

      @Override
      public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,  int status)
      {
          if(status == BluetoothGatt.GATT_SUCCESS)
          {
              Log.i("Oncharwrite", "We wrote to the characteristic " +
                      characteristic.getUuid().toString() +
                      " " +
                      characteristic.getValue().toString() );
          }
          else if(status == BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH){
              Log.e("WRITE", "Invalid attribute length");
          }
          else if(status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED)
          {
             Log.e("WRITE", "Write not permitted here");
          }
          else
          {
              Log.e("WRITE", "Status is " + status);
          }
      }

      @Override
      public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
      {
          if(status == BluetoothGatt.GATT_SUCCESS)
          {
              Log.i(TAG,"Success! Read characteristic with value of " + characteristic.getValue().toString());
          }
          else if(status == BluetoothGatt.GATT_READ_NOT_PERMITTED)
          {
              Log.e(TAG, "Read not permitted for this uuid");
          }
          else
          {
              Log.e(TAG, "Other error occured in char read");
          }
      }


    };

    private Boolean isBluetoothSupported()
    {
        if(btAdapt != null && btAdapt.isEnabled())
        {
            return true;
        }
        return false;
    }

    private boolean checkBtPermission()
    {
        if (ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {

            Log.e("PERM_CHECK", "Uh oh no bluetooth permissions please allow bluetooth");
            return false;
        }
        return true;
    }


    private byte[] convertToByteArray(int num)
    {
        byte[] byteArray = new byte[20];
        byteArray[3] = (byte) ((num >> 24) & 0xff);
        byteArray[2] = (byte) ((num >> 16) & 0xff);
        byteArray[1] = (byte) ((num >> 8) & 0xff);//8-16
        byteArray[0] = (byte) (num & 0xff);
        return byteArray;
    }

}


