package com.example.bluetooth;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Pattern;


@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    /**
     * Main activity that runs the app from the start and handles system
     * processing between screens
     */


    private ScanResult result;

    private BluetoothAdapter btAdapt;
    private BluetoothLeDeviceFilter filter =
            new BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile("Aaron")).build();
    private final int RCCARRESULTCODE = 7;
    private BluetoothDevice esp32;

    private Boolean isScanning = false;

    private BluetoothBackground bt;

    private String btPermission = Manifest.permission.BLUETOOTH_SCAN;
    private String localPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();


    private AssociationRequest pairRequest = new AssociationRequest.Builder().addDeviceFilter(filter)
            .setSingleDevice(true)
            .build();

    private CompanionDeviceManager devManage;
    private BluetoothGatt btGatt = null;

    /**
     *  onCreate is ran when the activity first begins to set up an initial state for the
     *  activity.
     * @param savedInstanceState -last known state of the application
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devManage = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        bt = new BluetoothBackground(this, devManage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapt = bluetoothManager.getAdapter();
        if (btAdapt == null || !btAdapt.isEnabled()) {
            notifyBluetoothDisabled();
            //finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if(resultCode != Activity.RESULT_OK)
        {
            Log.e("ACTIVITY", "Error in result");
            return;
        }
        if(requestCode == RCCARRESULTCODE && data != null)
        {
            result = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if( result.getDevice() != null)
            {
               bt.setEsp32(result.getDevice());
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

//    private BluetoothGattCallback  gattCallback = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            String devAddr = gatt.getDevice().getAddress();
//            if(status == BluetoothGatt.GATT_SUCCESS)
//            {
//                if(newState == BluetoothProfile.STATE_CONNECTED)
//                {
//                    Log.w("BluetoothGattCallback", "Connected to " + devAddr);
//                    btGatt = gatt;
//                    btGatt.discoverServices();
//                }
//                else if(newState == BluetoothProfile.STATE_DISCONNECTED)
//                {
//                    Log.w("BluetoothGattCallback", "Disconnected from " + devAddr);
//                    gatt.close();
//                }
//            }
//            else{
//                Log.w("BluetoothGattCallback", "Error " + status + " encountered for device "
//                 + devAddr);
//            }
//            super.onConnectionStateChange(gatt, status, newState);
//        }
//      @Override
//      public void onServicesDiscovered(BluetoothGatt gatt, int status)
//      {
//          Log.w("BluetoothGattCallback", "Discovered " + gatt.getServices().size() + " for " +
//                  gatt.getDevice().getAddress());
//          printGattTable(gatt);
//      }
//
//      @Override
//      public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,  int status)
//      {
//          if(status == BluetoothGatt.GATT_SUCCESS)
//          {
//              Log.i("Oncharwrite", "We wrote to the characteristic " +
//                      characteristic.getUuid().toString() +
//                      " " +
//                      characteristic.getValue().toString() );
//          }
//          else if(status == BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH){
//              Log.e("WRITE", "Invalid attribute length");
//          }
//          else if(status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED)
//          {
//             Log.e("WRITE", "Write not permitted here");
//          }
//          else
//          {
//              Log.e("WRITE", "Status is " + status);
//          }
//      }
//
//    };

    public void scanAndConnectBluetooth(View v) {
        devManage.associate(pairRequest, new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        try{
                            startIntentSenderForResult(
                                    chooserLauncher, RCCARRESULTCODE, null, 0,0,0
                            );
                        }
                        catch (IntentSender.SendIntentException e)
                        {
                            Log.e("MAIN", "Scan failed");
                        }
                    }

                    @Override
                    public void onFailure(CharSequence error) {
                        Log.e("SCAN", "Cannot find a BLE device with error" + error.toString());
                    }
                }
        , null);
    }




    private void notifyBluetoothDisabled()
    {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) findViewById(R.id.toast_layout_root));
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText("Bluetooth disabled");

        Toast notif = new Toast(getApplicationContext());
        notif.setGravity(Gravity.BOTTOM, 0, 0);
        notif.setDuration(Toast.LENGTH_LONG);
        notif.setView(layout);
        notif.show();
    }

//    private ActivityResultLauncher<String> requestPermissionLauncher =
//            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted ->{
//                if (isGranted) {
//                    Log.e("SCAN", "enabled");
//                } else {
//                    Log.e("SCAN", "Permission denied!");
//                    //finish();
//                }
//            });


//    private boolean isReadable(BluetoothGattCharacteristic characteristic)
//    {
//        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0);
//    }

//    private boolean isWritableNoResponse(BluetoothGattCharacteristic characteristic)
//    {
//        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0);
//    }
//    private boolean isWritable(BluetoothGattCharacteristic characteristic)
//    {
//        return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0);
//    }
//
//    private void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] data)
//    {
//        if(isWritable(characteristic))
//        {
//            characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE);
//            characteristic.setValue(data);
//           Boolean test = gatt.writeCharacteristic(characteristic);
//            Log.i("DATA", "write is " + test.toString());
//        }
//        else if(isWritableNoResponse(characteristic))
//        {
//            characteristic.setWriteType(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE);
//            characteristic.setValue(data);
//            gatt.writeCharacteristic(characteristic);
//        }
//        else
//        {
//            Log.e("writeCharacteristic", "This bluetooth characteristic is not writable");
//            return;
//        }
//    }



}