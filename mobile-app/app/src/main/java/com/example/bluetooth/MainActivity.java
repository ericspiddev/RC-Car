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
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import java.util.regex.Pattern;

/**
 * Main activity that runs the app from the start and handles system
 * processing between screens.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    /** filter used to look for certain bluetooth le devices. */
    private BluetoothLeDeviceFilter filter =
            new BluetoothLeDeviceFilter.Builder()
                    .setNamePattern(
                            Pattern.compile("Aaron")
                    ).build();
    /** code used to represent connection to RC Car. */
    private final int rcCarResultCode = 7;
    /** bluetooth background.  */
    private BluetoothBackground bt;
    /**Companion device manager used to prompt the user for bluetooth connection. */
    private CompanionDeviceManager devManage;

    /** Represents the speed slider bar and its associated data.*/
    private SeekBar rangeSlide;
    /** variable used to hold values from the slider bar.  */
    private int sliderVal = 0;



    /**
     * onCreate is ran when the activity first begins to set up an initial state for the
     * activity.
     *
     * @param savedInstanceState -last known state of the application
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devManage = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
        rangeSlide = findViewById(R.id.speedBar);
        rangeSlide.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * Callback triggered from when the seekbar progress changes
             * (user moves up or down)
             * @param seekBar - the seekBar object that triggered the event
             * @param progress - the position of the seekbar (0-15)
             * @param fromUser - boolean stating whether the event was from the user
             */
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                 sliderVal = rangeSlide.getProgress();
            }

            /**
             * This callback function is called when the user begins to touch the seek bar
             * @param seekBar - the seekbar object that triggered the on start touch event
             */
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) { //needed to override tracking

            }

            /**
             * This function is called when the user stops touching the slider bar
             * @param seekBar - the seekBar object that is released
             */
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (bt != null) {
                    bt.updateSpeed((byte) sliderVal);
                }
            }
        });

    }

    /**
     *  When the app switches screens or activities and comes back the
     *  onResume function is ran to bring us back to where we were.
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Callback function for when we launch an activity for a result.
     * In this case the bluetooth connect prompt.
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        ScanResult result;
        if (resultCode != Activity.RESULT_OK) {
            Log.e("ACTIVITY", "Error in result");
            return;
        }
        if (requestCode == rcCarResultCode && data != null) {
            result = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);
            if (result.getDevice() != null) {
                bt.setEsp32(result.getDevice());
                bt.connectGatt();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    /**
     * This function is called when the user presses the connect
     * button.
     * @param v  - the view that the prompt should appear on
     */
    public void scanAndConnectBluetooth(final View v) {
        BluetoothManager bluetoothManager;
        setBluetoothName();
        AssociationRequest pairRequest = new AssociationRequest.Builder().addDeviceFilter(filter)
                .setSingleDevice(true)
                .build();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bt = new BluetoothBackground(this, bluetoothManager);
        devManage.associate(pairRequest, new CompanionDeviceManager.Callback() {
                    /**
                     * Called when a bluetooth device matching the name is found
                     * @param chooserLauncher - the launcher to show prompt to user
                     */
                    @Override
                    public void onDeviceFound(final IntentSender chooserLauncher) {
                        try {
                            startIntentSenderForResult(
                                    chooserLauncher, rcCarResultCode, null, 0, 0, 0
                            );
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("MAIN", "Scan failed");
                        }
                    }

                    /**
                     * Called if we fail to connect to a bluetooth le device
                     * @param error - the error from trying to connect to the device
                     */
                    @Override
                    public void onFailure(final CharSequence error) {
                        Log.e("SCAN", "Cannot find a BLE device with error"
                                + error.toString());
                    }
                }, null);
    }

    /**
     * Function called when any of the direction buttons are pressed
     * based on the button pressed we send certain data.
     * @param v - view that the button called by
     */
    public void drive(final View v) {
        if (bt != null) {
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
                    Log.e("MAIN", "What the heck did we just press???");
                    break;
            }
        } else {
            Log.e("MAIN", "CONNECT TO BT DEV FIRST");
        }
    }

    /**
     * Helper function used to set the bluetooth name for the device.
     * This is read in from a edittext box on the activity screen
     */
    private void setBluetoothName() {
        EditText btName = findViewById(R.id.editBtName);
        filter = new BluetoothLeDeviceFilter.Builder().setNamePattern(Pattern.compile(btName.getText().toString())).build();
    }
}

