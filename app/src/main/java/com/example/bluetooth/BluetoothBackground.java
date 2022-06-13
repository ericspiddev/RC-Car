package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.UUID;


@RequiresApi(api = Build.VERSION_CODES.O)
public class BluetoothBackground {

    private Context activityContext;
    private CompanionDeviceManager devManage;
    private BluetoothGatt bluetoothGatt;

    private final byte[] FORWARD = convertToByteArray(65);
    private final byte[] BACKWARD = convertToByteArray(2);
    private final byte[] LEFT = convertToByteArray(4);
    private final byte[] RIGHT = convertToByteArray(8);
    private final byte[] SPEED = convertToByteArray(50);

    private BluetoothDevice esp32;
    private final UUID serviceUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"); // do we need to use this? 00000000-0000-1000-8000-00805f9b34fb
    private final UUID txUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private final UUID rxUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private BluetoothGattCharacteristic rxCharacteristic;

    public BluetoothBackground(Context c, CompanionDeviceManager manager) {
        this.activityContext = c;
        this.devManage = manager;
    }

    public void connectToDevice(String devName) {

    }

    public void goForward() {
        writeData(FORWARD);
    }

    public void goBackward() {
        writeData(BACKWARD);
    }

    public void goLeft() {
        writeData(LEFT);
    }

    public void goRight() {
        writeData(RIGHT);
    }

    public void writeData(byte direction[]) {
        if (bluetoothGatt != null && rxCharacteristic != null) {
            writeCharacteristic(bluetoothGatt, rxCharacteristic, direction);
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
            esp32.connectGatt(activityContext, false,gattCallback);
        }
    }

    private void printGattTable(BluetoothGatt gatt) {
        if (gatt.getServices() == null)
        {
            Log.i("printGattTable", "No service or characteristics available, call " +
                    "discover services first?");
        }
        else {
            for (BluetoothGattService service : gatt.getServices())
            {
                Log.i("printGattTable", "Service is " + service.getUuid());
                for (BluetoothGattCharacteristic character : service.getCharacteristics())
                {
                    Log.e("UUID's", "char uuid is " + character.getUuid().toString());
                    if (character.getUuid().equals(rxUUID))
                    {
                        rxCharacteristic = character;
                    }
                    Log.i("printGattTable", "\tCharacteristic is " + character.toString());
                }
            }
        }
    }

    private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] data) {
        if (isWritable(characteristic))
        {
            characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE);
            characteristic.setValue(data);
            if(checkBtPermission())
            {
                Boolean test = gatt.writeCharacteristic(characteristic);
                Log.i("DATA", "write is " + test.toString());
            }
        }
        else if(isWritableNoResponse(characteristic))
        {
            characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
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

    public BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String devAddr = gatt.getDevice().getAddress();
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                if(newState == BluetoothProfile.STATE_CONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Connected to " + devAddr);
                    bluetoothGatt = gatt;
                    if(checkBtPermission()) {
                        bluetoothGatt.discoverServices();
                    }
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.w("BluetoothGattCallback", "Disconnected from " + devAddr);
                    if(checkBtPermission()) {
                        bluetoothGatt.close();
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
            printGattTable(gatt);
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
    };

    private boolean checkBtPermission()
    {
        if (ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("PERM_CHECK", "Uh oh no bluetooth permissions please allow bluetooth");
            return false;
        }
        return true;
    }


    private byte[] convertToByteArray(int num)
    {
        return new byte[] {
                (byte) ((num >> 24) & 0xff), //MS 8 bits 32-24
                (byte) ((num >> 16) & 0xff), //16-24
                (byte) ((num >> 8) & 0xff),//8-16
                (byte) (num & 0xff) // LS 8 bits 0 - 8
        };
    }
}


