package com.example.bluetooth;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private BluetoothDevice thunder;
    private BluetoothAdapter adapter;
    private String ourDev = "Eric Spidle";
    private String hardwareaddr = "";
    private Set<BluetoothDevice> pairedDevs;
    private BluetoothSocket connection;
    private String TAG = "Main";
    private BluetoothLeService myBluetoothLeService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startBluetoothActivity(View view)
    {

        Intent mIntent = new Intent(this, BluetoothScanActivity.class);
        startActivity(mIntent);
    }

    public void sayHiToEric(View view)
    {
       snackBarAlert(view, "Hi Eric!");
    }

    private final ServiceConnection myServiceConnector = new ServiceConnection() { // object used to connect main activity to bluetoothLE Service
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            myBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService(); //cast service to BluetoothLeService and then get the service
            if(!myBluetoothLeService.initialize()) // if we cannot init bluetooth
            {
                notifyBluetoothDisabled(); // display Toast to let user know bluetooth is not enabeld
                finish(); // end activity
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBluetoothLeService = null; //remove reference to bluetooth service when we disconnect
        }
    };
    private void snackBarAlert(View v, String message)
    {
        Snackbar mySnackbar = Snackbar.make(v, message, BaseTransientBottomBar.LENGTH_LONG);
        mySnackbar.show();
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
}