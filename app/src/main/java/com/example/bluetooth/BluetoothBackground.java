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
 * This class is responsible for running all of the backend
 * bluetooth functionality. This includes connecting,
 * disconnecting, sending data and reading data from a
 * bluetooth LE device.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class BluetoothBackground {
    /** Passed in Activity Context.*/
    private Context activityContext;
    /** Variable to hold Gatt Server Object. */
    private BluetoothGatt btGatt;
    /** Manages all of the bluetooth devices (RC Car). */
    private BluetoothManager
            btManage;
    /** Phone's physical bluetooth Adapter. */
    private BluetoothAdapter btAdapt;
    /** Object* for the microcontroller. */
    private BluetoothDevice esp32;
    /**Byte to send over bluetooth. */
    private byte carData = 0;
    /** Byte value to send the car forward. */
    private final byte driveForward = 8;
    /** Byte value to send the car backward. */
    private final byte driveBackward = 4;
    /** Byte value to send the left. */
    private final byte driveLeft = 2;
    /** Byte value to send the car right. */
    private final byte driveRight = 1;
    /** Holds the four bits for speed. */
    private byte currSpeed = 0;



    /** Holds the four bits for direction. */
    private byte currDir = 0;
    /** Value to shift the byte for direction. */
    private final int shift = 4;

    /** BT Service UUID.*/
    private final UUID serviceUUID =
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    /**RX Char UUID.*/
    private final UUID rxUUID =
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /** Object to hold the uartBtService.*/
    private BluetoothGattService uartBtService;
    /** Object to handle the rx BLE Char.*/
    private BluetoothGattCharacteristic rxCharacteristic;

    /**
     * Bluetooth Background constructor that creates a background object
     * so our main activity can bind to a bluetooth LE gatt.
     * @param c - passed context from the
     * @param manager - the bluetooth manager that is passed in from the activity
     */
    public BluetoothBackground(final Context c, final BluetoothManager manager) {
        this.activityContext = c;
        this.btManage = manager;
        btAdapt = btManage.getAdapter();
        if (!isBluetoothSupported()) {
            Log.e("BT", "Bluetooth not supported!");
        }

    }
    public BluetoothBackground() {

    }


    /**
     * This function is called to drive the car forward once we are
     * connected to a car.
     */
    public void goForward() {
        currDir = driveForward;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *  This function is called to drive the car backward once we are
     *  connected to a car.
     */
    public void goBackward() {
        currDir = driveBackward;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *  This function is called to drive the car left once we are
     *  connected to a car.
     */
    public void goLeft() {
        currDir = driveLeft;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     * This function is called to drive the car right once we are
     * connected to a car.
     */
    public void goRight() {
        currDir = driveRight;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     * This function is called when the user
     * slides the speed slider to adjust the
     * speed of the car.
     * @param speed - the speed value to send to the car
     */
    public void updateSpeed(final byte speed) {
        currSpeed = speed;
        writeData(setCarData(currSpeed, currDir));
    }

    /**
     *  This function is used to set the esp32 so we can communicate over
     *  bluetooth with the esp.
     * @param device - the bluetooth device that will represent the esp32 in our code
     */
    public void setEsp32(final BluetoothDevice device) {
        this.esp32 = device;
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
     *  Function that calls back each time we encounter a bluetooth event
     *  this can be a write, read, connection or disconnection.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            final int status,
                                            final int newState) {
            String address = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Connected to " + address);
                    btGatt = gatt;
                    if (checkBtPermission()) {
                        btGatt.discoverServices();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Disconnected from " + address);
                    if (checkBtPermission()) {
                        btGatt.close();
                    }
                }
            } else {
                Log.w("BluetoothGattCallback", "Error " + status + " encountered for device"
                        + address);
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        /**
         * Callback function used for when we discover the services for a gatt
         * @param gatt - bluetooth gatt object we checked for services on
         * @param status - the status of our service discovery
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.w("BluetoothGattCallback", "Discovered " + gatt.getServices().size()
                    + " for "
                    + gatt.getDevice().getAddress());
            getServiceAndCharacteristics(gatt);
        }

        /**
         *  Callback that gets called when we do a characteristic write
         *  this checks for successful write and logs the results
         * @param gatt - the gatt used to get the characteristic
         * @param characteristic - char we are writing too
         * @param status - the status of write from the characteristic
         */
        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt,
                                          final BluetoothGattCharacteristic characteristic,
                                          final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("WRITE", "We wrote to the characteristic "
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

    };

    /**
     * Writes the byte array to the bluetooth LE device.
     * @param data - the data to be written to the bluetooth LE device
     */
    private void writeData(final byte[] data) {
        if (btGatt != null && rxCharacteristic != null) {
            writeCharacteristic(btGatt, rxCharacteristic, data);
        } else {
            Log.e("SEND_DATA", "Please connect to bluetooth device first");
        }
    }

    /**
     * Gets the  Uart service and RX characteristic for writing data to the
     * bluetooth device.
     * @param gatt - the gatt object that has our service and characteristic on it
     */
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

    /**
     * Writes the data to the passed in characteristic.
     * @param gatt - bluetooth gatt object that represents our connection
     * @param characteristic - the gatt char we are writing the data to
     * @param data - the data to be written to characteristic
     */
    private void writeCharacteristic(final BluetoothGatt gatt,
                                     final BluetoothGattCharacteristic characteristic,
                                     final byte[] data) {
        if (isWritable(characteristic)) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(data);

        } else if (isWritableNoResponse(characteristic)) {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(data);
            if (checkBtPermission()) {
                gatt.writeCharacteristic(characteristic);
            }
        } else {
          // Log.e("writeCharacteristic", "This bluetooth characteristic is not writable");
        }
    }

    /**
     * Checks to see if the characteristic is writable but does not provide a
     * response.
     * @param characteristic - char to be checked if it is writable
     * @return - boolean if the characteristic is writable with no response or not
     */
    private boolean isWritableNoResponse(final BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0);
    }

    /**
     * Checks to see if the passed in char is writable with response.
     * @param characteristic - char ot be checked if it is writable
     * @return boolean if the characteristic is writable or not
     */
    private boolean isWritable(final BluetoothGattCharacteristic characteristic) {
        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
    }

    /**
     * Checks to see if the current device supports
     * bluetooth le and has it enabled.
     * @return - bool that returns whether or bt is available/enabled
     */
    private Boolean isBluetoothSupported() {
        return (btAdapt != null && btAdapt.isEnabled());
    }

    /**
     * Called to ensure our application has the correct
     * permissions before executing the bluetooth commands.
     * @return - a boolean indicating whether the app has permission or not
     */
    private boolean checkBtPermission() {
        if (ActivityCompat.checkSelfPermission(activityContext, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("PERM_CHECK", "Uh oh no bluetooth permissions please allow bluetooth");
            return false;
        }
        return true;
    }

    /**
     * Sets the car data into a byte array with the data to
     * send the car.
     * @param speed - speed value to drive the car
     * @param dir - the direction to drive the car
     * @return - byte array that will be sent to the car via bluetoothLE
     */
    public byte[] setCarData(final byte speed, final byte dir) {
        this.carData = 0;
        this.carData |= dir << shift;
        this.carData |= speed;
        return new byte[] {this.carData};
    }

    public byte getCurrDir() {
        return currDir;
    }

}


