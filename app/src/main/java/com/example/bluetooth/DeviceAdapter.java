package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.Layout;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used as the adapter for the recycler view when the user starts
 * the bluetooth activity. This adapter implements the logic to display
 * each device's name in a row.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<String> scannedDevices;
    private ArrayList<String> deviceNames =  new ArrayList <>();

    /**
     *  The contstructor inits a list of strings that hold the scanned devices
     * @param scannedDevices - list of available devices
     */
    public DeviceAdapter(List<String> scannedDevices)
    {
        this.scannedDevices = scannedDevices;
       // extractDeviceNames();
    }

    /**
     *  This Viewholder creates each indivdual row and is baesed on the overridden function
     *  getItemCount to determine how many row items it creates
     * @param parent - the parent of the current viewgroup
     * @param viewType - the type of view (Screen)
     * @return - returns a new viewholder item that displays one single row
     */
    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_item, parent, false);
        return new ViewHolder(rowItem);
    }

    /**
     *  Function that gets ran by the holder when we bind to the actual view. this is usually done
     *  when the activity is first created (on start or onresume)
     * @param holder - the viewholder that will be modified (holding the rowitem)
     * @param position - the position of the rowitem that will be modified
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position)
    {
       holder.textView.setText(this.scannedDevices.get(position)); //set the row to our text
    }

    /**
     * Gets the number of rows to create based on the data
     * @return - the number of items that will be listed
     */
    @Override
    public int getItemCount()
    {
        return scannedDevices.size();
    }

    /**
     * Viewholder class that will extend the recycler view to make a scrollable list of items
     * it also uses the onClickListener interface to make custom actions for when we
     * click each item
     */
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView textView;

        public ViewHolder(View v)
        {
            super(v);
            itemView.setOnClickListener(this);
            this.textView = v.findViewById(R.id.btName);
        }

        /**
         *  Function that overrides what happens when we click a row
         * @param view - the current view to listen for the click on
         */
        @Override
        public void onClick(View view)
        {
            Toast.makeText(view.getContext(), "Clicked element", Toast.LENGTH_LONG).show();
        }
    }

//    private void extractDeviceNames()
//    {
//        for (BluetoothDevice device :scannedDevices)
//        {
//            deviceNames.add(device.getName());
//        }
//    }
}
