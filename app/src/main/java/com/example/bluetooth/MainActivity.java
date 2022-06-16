package com.example.bluetooth;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
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
import android.widget.EditText;
import android.widget.SeekBar;
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
    private BluetoothLeDeviceFilter filter =
            new BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile("Aaron")).build();
    private final int RCCARRESULTCODE = 7;
    private BluetoothBackground bt;
    private CompanionDeviceManager devManage;
    private BluetoothManager bluetoothManager;
    private SeekBar rangeSlide;
    private int sliderVal = 0;
    private final String TAG = "MAIN";


    /**
     * onCreate is ran when the activity first begins to set up an initial state for the
     * activity.
     *
     * @param savedInstanceState -last known state of the application
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devManage = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        rangeSlide = findViewById(R.id.speedBar);
        rangeSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                 sliderVal = rangeSlide.getProgress();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(bt != null)
                {
                    bt.updateSpeed((byte)sliderVal);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            Log.e("ACTIVITY", "Error in result");
            return;
        }
        if (requestCode == RCCARRESULTCODE && data != null) {
            result = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if (result.getDevice() != null) {
                bt.setEsp32(result.getDevice());
                bt.connectGatt();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }



    public void scanAndConnectBluetooth(View v) {
        setBluetoothName();
        AssociationRequest pairRequest = new AssociationRequest.Builder().addDeviceFilter(filter)
                .setSingleDevice(true)
                .build();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bt = new BluetoothBackground(this, bluetoothManager);
        devManage.associate(pairRequest, new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        try {
                            startIntentSenderForResult(
                                    chooserLauncher, RCCARRESULTCODE, null, 0, 0, 0
                            );
                        } catch (IntentSender.SendIntentException e) {
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


    public void drive(View v)
    {
        if(bt != null) {
            switch (v.getId()) {
                case R.id.fButton:
                    bt.goForward();
                    break;
                case R.id.bButton:
                    bt.goBackward();
                    break;
                case R.id.lButton:
                    bt.goLeft();
                    break;
                case R.id.rButton:
                    bt.goRight();
                    break;
                default:
                    Log.e(TAG, "Wht the heck did we just press???");
                    break;
            }
        }
        else
        {
            Log.e(TAG, "CONNECT TO BT DEV FIRST");
        }
    }


    private void setBluetoothName()
    {
        EditText btName = findViewById(R.id.editBtName);
        filter = new BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile(btName.getText().toString())).build();
    }

    private void notifyBluetoothDisabled() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.toast_layout_root));
        TextView text = layout.findViewById(R.id.text);
        text.setText("Bluetooth disabled");

        Toast notif = new Toast(getApplicationContext());
        notif.setGravity(Gravity.BOTTOM, 0, 0);
        notif.setDuration(Toast.LENGTH_LONG);
        notif.setView(layout);
        notif.show();
    }

}

