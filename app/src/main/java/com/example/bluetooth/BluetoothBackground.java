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
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import java.util.UUID;

/**
 * Java.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class BluetoothBackground { /** yrdy. */
    private Context activityContext; /** Passed in Activity Context.*/
    private BluetoothGatt btGatt; /** Variable to hold Gatt Server Object. */
    private BluetoothManager
            btManage; /** Manages all of the bluetooth devices (RC Car). */
    private BluetoothAdapter btAdapt; /** Phone's physical bluetooth Adapter. */
    private BluetoothDevice esp32; /** Object* for the microcontroller. */
    private byte carData = 0; /**Byte to send over bluetooth. */
    private final byte driveForward = 8; /** Byte value to send the car forward. */
    private final byte driveBackward = 4; /** Byte value to send the car backward. */
    private final byte driveLeft = 2; /** Byte value to send the left. */
    private final byte driveRight = 1; /** Byte value to send the car right. */
    private byte currSpeed = 0; /** Holds the four bits for speed. */
    private byte currDir = 0; /** Holds the four bits for direction. */
    private final int shift = 4; /** Value to shift the byte for direction. */

    private final UUID serviceUUID =
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"); /** BT Service UUID.*/

    private final UUID rxUUID =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); /**RX Char UUID.*/
    private BluetoothGattService uartBtService; /** Object to hold the uartBtService.*/
    private BluetoothGattCharacteristic rxCharacteristic; /** Object to handle the rx BLE Char.*/
    private String tag = "BACKGROUND"; /** Tag for logging. */

    /**
     *
     * @param c
     * @param manager
     */
    public BluetoothBackground(final Context c, final BluetoothManager manager) {
        this.activityContext = c;
        this.btManage = manager;
        btAdapt = btManage.getAdapter();
        if (!isBluetoothSupported()) {
            Log.e("BT", "Bluetooth not supported!");
        }

    }

    /**
     *
     */
    public void goForward() {
        currDir = driveForward;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *
     */
    public void goBackward() {
        currDir = driveBackward;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *
     */
    public void goLeft() {
        currDir = driveLeft;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *
     */
    public void goRight() {
        currDir = driveRight;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *
     * @param speed
     */
    public void updateSpeed(final byte speed) {
        currSpeed = speed;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *  This function is used to set the esp32 so we can communicate over
     *  bluetooth with the esp.
     * @param esp32 - the bluetooth device that will represent the esp32 in our code
     */
    public void setEsp32(final BluetoothDevice esp32) {
        this.esp32 = esp32;
    }

    /**
     * This function is used to actually connect the bluetooth device
     * to the gatt server and register it with the proper callback.
     */
    public void connectGatt() {
        if (esp32 != null && checkBtPermission()) {
            esp32.connectGatt(activityContext, false, gattCallback, 2);
        }
    }

    /**
     *  The funct.
     */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            final int status,
                                            final int newState) {
            String devAddr = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Connected to " + devAddr);
                    btGatt = gatt;
                    if (checkBtPermission()) {
                        btGatt.discoverServices();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Disconnected from " + devAddr);
                    if (checkBtPermission()) {
                        btGatt.close();
                    }
                }
            } else {
                Log.w("BluetoothGattCallback", "Error " + status + " encountered for device"
                        + devAddr);
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.w("BluetoothGattCallback", "Discovered " + gatt.getServices().size()
                    + " for "
                    + gatt.getDevice().getAddress());
            getServiceAndCharacteristics(gatt);
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic,
                                          final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("Oncharwrite", "We wrote to the characteristic "
                        + characteristic.getUuid().toString()
                        + " "
                        + characteristic.getValue().toString());
            } else if (status == BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH) {
                Log.e("WRITE", "Invalid attribute length");
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.e("WRITE", "Write not permitted here");
            } else {
                Log.e("WRITE", "Status is " + status);
            }
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(tag, "Success! Read characteristic with value of " + characteristic.getValue().toString());
            } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                Log.e(tag, "Read not permitted for this uuid");
            } else {
                Log.e(tag, "Other error occured in char read");
            }
        }


    };

    private void writeData(final byte[] direction) {
        if (btGatt != null && rxCharacteristic != null) {
            writeCharacteristic(btGatt, rxCharacteristic, direction);
        } else {
            Log.e("SENDDATA", "Please connect to bluetooth device first");
        }
    }

    private void getServiceAndCharacteristics(final BluetoothGatt gatt) {
        if (gatt.getServices() == null) {
            Log.i("printGattTable", "No service or characteristics available, call "
                    + "discover services first?");
        } else {
            uartBtService = gatt.getService(serviceUUID);
            if (uartBtService != null) {
                rxCharacteristic = uartBtService.getCharacteristic(rxUUID);
            }

        }
    }

    private void writeCharacteristic(final BluetoothGatt gatt,
                                     final BluetoothGattCharacteristic characteristic,
                                     final byte[] data) {
        if (isWritable(characteristic)) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(data);
            if (checkBtPermission()) {
                Boolean test = gatt.writeCharacteristic(characteristic);
                Log.i("DATA", "write is " + test);
            }

        } else if (isWritableNoResponse(characteristic)) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(data);
            if (checkBtPermission()) {
                gatt.writeCharacteristic(characteristic);
            }
        } else {
            Log.e("writeCharacteristic", "This bluetooth characteristic is not writable");
            return;
        }
    }

    private boolean isWritableNoResponse(final BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0);
    }

    private boolean isWritable(final BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
    }

    private boolean isReadable(final BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
    }

    private Boolean isBluetoothSupported() {
        if (btAdapt != null && btAdapt.isEnabled()) {
            return true;
        }
        return false;
    }

    private boolean checkBtPermission() {
        if (ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("PERM_CHECK", "Uh oh no bluetooth permissions please allow bluetooth");
            return false;
        }

        return true;
    }

    private byte[] setCarData(final byte speed, final byte dir) {
        this.carData = 0;
        this.carData |= dir << shift;
        this.carData |= speed;
        return new byte[] {this.carData};
    }
}


