package com.group5.android.bluetoothiot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class DeviceListActivity extends Activity {


    public static String EXTRA_DEVICE_ADDRESS ="device_address";
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> pairedDevicesArrayAdapter;
    private ArrayAdapter<String> newDevicesArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        //Set result Canceled incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        //Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        newDevicesArrayAdapter =new ArrayAdapter<String>(this, R.layout.device_name);

        //Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(deviceClickListener);

        //Find and set up the ListView for newly devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(deviceClickListener);

        //Register for broatcasting when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        //Register for broadcasting when discover has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);

        //get the local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0){
            findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device: pairedDevices){
                pairedDevicesArrayAdapter.add(device.getName() +"\n"+ device.getAddress());
            }

        }
        else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
        //Make sure we're not discovering anymore
        if (bluetoothAdapter != null){
            bluetoothAdapter.cancelDiscovery();
        }

        //Unregister broadcast listeners
        this.unregisterReceiver(receiver);
    }



    private void doDiscovery() {
        //Indicate scanning the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        //Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        //if we're already discovered, stop it
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }

        //Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }


    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            bluetoothAdapter.cancelDiscovery();

            //Get device MAC address, last 17 character in the view
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() -17);

            //Create the result Intent and include the Mac address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            //Set result and finish this Acitivity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //When discovery found a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                //get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //if already paired, skip it, because it's already listed
                if (device.getBondState() != BluetoothDevice.BOND_BONDED){
                    newDevicesArrayAdapter.add(device.getName() +"\n" + device.getAddress());
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (newDevicesArrayAdapter.getCount() == 0){
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}
