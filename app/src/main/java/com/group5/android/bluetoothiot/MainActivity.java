package com.group5.android.bluetoothiot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothConnection.ConnectionFuture connectionFuture = null;
    private Button transmitButton;

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private void debug(String text) {
        Log.i("MainActivity", text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //if adapter is null, then Bluetooth is not supported on this device
        if (bluetoothAdapter == null) {
            debug("Bluetooth is not available!");
            finish();
            return;
        }

        TextView displayedTextBox = (TextView) findViewById(R.id.recieved_text);
        displayedTextBox.setMovementMethod(new ScrollingMovementMethod());

        final EditText transmitTextBox = (EditText) findViewById(R.id.transmit_text);
        transmitButton = (Button) findViewById(R.id.button_transmit);
        transmitButton.setEnabled(false);
        transmitButton.setText("Connecting...");
        transmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (transmitTextBox) {
                    Editable transmitText = transmitTextBox.getText();
                    String text = transmitText.toString();
                    transmitText.clear();
                    try {
                        // Disable and block until this is ready
                        connectionFuture.get().write(text.getBytes());
                        debug("Wrote message: " + text);
                    } catch (IOException e) {
                        debug("write failed");
                    }
                }
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        debug("result!");
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                onSelectDeviceActivityResult(resultCode, data);
                break;
            case REQUEST_ENABLE_BT:
                onEnableBluetoothActivityResult(resultCode, data);
                break;
        }
    }

    private void onEnableBluetoothActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // do something interesting?
            debug("Setting complete!");
        } else {
            debug("Setting up bluetooth failed.");
            finish();
        }
    }

    private void onSelectDeviceActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String address = data.getExtras()
                    .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
//      debug("GRD");
            debug("extras");

//      debug("Connecting to: " + address);

            Log.i("BlueDuino", "Creating connection");
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            connectionFuture = new BluetoothConnection.ConnectionFuture(device, readHandler);
            if (connectionFuture.failed()) {
                debug("Connection failed");
            } else {
                final BluetoothConnection.ConnectionFuture localConnection = connectionFuture;
                final Button localButton = transmitButton;
                Log.i("BlueDuino", "Starting AsyncTask");
                AsyncTask<Integer, Integer, Boolean> execute = new AsyncTask<Integer, Integer, Boolean>() {
                    public Boolean doInBackground(Integer... params) {
                        localConnection.block();
                        Log.i("BlueDuino", "done blocking for connection");
                        return localConnection.failed();
                    }

                    public void onPostExecute(Boolean failed) {
                        if (!failed) {
                            localButton.setEnabled(true);
                            transmitButton.setText("Transmit");
                        }
                    }
                }.execute();
//        connection = connectionFuture.get();

//        debug("Established connection to: " + address);
                // try {
                //   connection.write("+RR-".getBytes());
                //   debug("Writing message");
                // } catch (IOException e) {
                //   debug("Write failed.");
                // }
            }
        }
    }

    private final Handler readHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothConnection.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
//                debug(readMessage);
                    TextView displayedTextBox = (TextView) findViewById(R.id.recieved_text);
                    displayedTextBox.append(readMessage);

                    final int scrollAmount = displayedTextBox.getLayout().getLineTop(
                            displayedTextBox.getLineCount()) - displayedTextBox.getHeight();

                    // if there is no need to scroll, scrollAmount will be <=0
                    if (scrollAmount > 0) {
                        displayedTextBox.scrollTo(0, scrollAmount);
                    }
// else {
//                  displayedTextBox.scrollTo(0,0);
//                }
            }
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
