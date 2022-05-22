package com.example.bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothDevice thunder;
    private String ourDev = "Eric Spidle";
    private String hardwareaddr = "";
    private Set<BluetoothDevice>  pairedDevs;
    private UUID unique = new UUID(21,15);
    private BluetoothSocket connection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(reciever, filter);
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if(bt == null) {
            System.out.println("Bluetooth not supported");
        }
        else if(isDevicePaired(bt))
        {

        }
        else
        {
            try{
                bt.startDiscovery();
            }
            catch(SecurityException e)
            {

            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(reciever);
    }

    private final BroadcastReceiver reciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                try{
                    if(dev.getName() == ourDev){
                        hardwareaddr = dev.getAddress();
                        try{

                            //connection = dev.createRfcommSocketToServiceRecord(unique);

                        }
                        catch(SecurityException e)
                        {

                        }

                    }
                }
                catch (SecurityException e)
                {

                }
            }
        }
    };

    private Boolean isDevicePaired(BluetoothAdapter bt)
    {
        try{
            pairedDevs = bt.getBondedDevices();
            if(pairedDevs.size() > 0)
            {
                for(BluetoothDevice dev : pairedDevs)
                {
                    String name= dev.getName();
                    String addr = dev.getAddress();
                    if(name == ourDev)
                        return true;
                }
            }
        }
        catch(SecurityException e)
        {
            System.out.println("bluetooth not enabled for location");
        }
        return false;
    }


}