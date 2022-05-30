package com.example.bluetooth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothScanActivity extends AppCompatActivity {
   // private ArrayList<String> data = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);


        RecyclerView rv = findViewById(R.id.rvDevs);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter( new DeviceAdapter(generateData()));
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private List<String> generateData()
    {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            data.add(i + "th Element");
        }
        return data;
    }
}
