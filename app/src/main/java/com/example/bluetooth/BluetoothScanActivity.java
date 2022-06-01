package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Activty that runs when the user clicks the bluetooth button This shows the user
 * the list of nearby BluetoothLE devices in a scrollable list
 */
public class BluetoothScanActivity extends AppCompatActivity {
    private ArrayList<BluetoothDevice> scannedDevices = new ArrayList<>();

    private BluetoothAdapter mAdapter;
    private BluetoothLeScanner scanner;
    private boolean scanning = false;
    private Handler handle;
    private final int SCAN_DURATION = 10000;


    /**
     *  onCreate is ran when the activity first begins to set up an initial state for the
     *  activity.
     * @param savedInstanceState -last known state of the application
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        handle = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE not supported", Toast.LENGTH_LONG).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mAdapter = bluetoothManager.getAdapter();

        if (mAdapter == null) {
            Toast.makeText(this, "No Bluetooth Adapter Found", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    /**
     * This function is called after onStart and is what actually starts the scanning
     * and creation of the recyclerView
     */
    @Override
    public void onResume() {
        super.onResume();
        if (!mAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_LONG).show();
            finish();
        }
        scanner = mAdapter.getBluetoothLeScanner();
        scanLeDevice();
        RecyclerView rv = findViewById(R.id.rvDevs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter( new DeviceAdapter(generateData()));
    }


    private void scanLeDevice() {
        if (!scanning) {
            handle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    if(checkPermission())
                    scanner.stopScan(scanCallback);
                }
            }, SCAN_DURATION);
            scanning = true;
            if(checkPermission())
            scanner.startScan(scanCallback);
        }
        else {
            scanning = false;
            if(checkPermission())
                scanner.stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result)
                {
                    super.onScanResult(callbackType, result);
                    scannedDevices.add(result.getDevice());
                }
            };

    private List<String> generateData()
    {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add("Device " + i);
        }
        return data;
    }

    private boolean checkPermission()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
           // ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.BLUETOOTH_SCAN}, 100);
            // here to request the missing permissions, and then overriding
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }
        return false;
    }

}
