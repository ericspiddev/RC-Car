package com.example.bluetooth;

import android.content.Context;
import android.text.Layout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<String> deviceNames;

    public DeviceAdapter( List<String> deviceNames)
    {
        this.deviceNames = deviceNames;
    }

    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_item, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position)
    {
       holder.textView.setText(this.deviceNames.get(position));
    }

    @Override
    public int getItemCount()
    {
        return deviceNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView textView;

        public ViewHolder(View v)
        {
            super(v);
            itemView.setOnClickListener(this);
            this.textView = v.findViewById(R.id.btName);
        }

        @Override
        public void onClick(View view)
        {
            Toast.makeText(view.getContext(), "Clicked element", Toast.LENGTH_LONG).show();
        }
    }
}
