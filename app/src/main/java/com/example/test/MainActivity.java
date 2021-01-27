package com.example.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothDevice[] btDevice;
//    private Handler handler; // handler that gets info from Bluetooth service
//    SendReceive sendReceive;

    SendReceive sendReceive;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;

    private ListView listViewDiscover, listViewPaired;
    ArrayList<String> listItems = new ArrayList<String>();
    private ArrayList<String> mDeviceList = new ArrayList<String>();

    TextView textViewDiscover, pairedDevicesTV,msg_box;
    EditText textMsg;
    Button btn, discoverabilityBtn, getPairedDevicesBtn, sendBtn;
    private BluetoothAdapter mBluetoothAdapter;

    private final static int REQUEST_DISCOVER_BT = 1; // Unique request code
    private static final String APP_NAME = "Bluetooth communication";
    private static final UUID MY_UUID = UUID.fromString("6283a4ab-c92f-4bbf-8454-1ff15c36195f");

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewDiscover = (ListView) findViewById(R.id.listViewDiscover);
        listViewDiscover.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,mDeviceList));

        listViewPaired = (ListView) findViewById(R.id.listViewPaired);
        listViewPaired.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,listItems));

        textViewDiscover    = (TextView) findViewById(R.id.textViewDiscover);
        pairedDevicesTV     = (TextView) findViewById(R.id.pairedDevicesTV);
        btn                 = (Button) findViewById(R.id.button);
        sendBtn             = (Button) findViewById(R.id.sendBtn);
        discoverabilityBtn  = (Button)findViewById(R.id.discoverabilityBtn);
        getPairedDevicesBtn = (Button) findViewById(R.id.getPairedDevicesBtn);
        textMsg             = (EditText) findViewById(R.id.textMsg);
        msg_box             = (TextView) findViewById(R.id.msg_box);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // permission for LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        }

        // If another discovery is in progress, cancels it before starting the new one.
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BaseAdapter) listViewDiscover.getAdapter()).notifyDataSetChanged();
                final IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(mReceiver, filter);
                mBluetoothAdapter.startDiscovery();
            }
        });

        // Enable discoverability click
        discoverabilityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBluetoothAdapter.isDiscovering()){
                    showToast("make the device discovering.");
                    // intent
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivityForResult(intent,REQUEST_DISCOVER_BT);
                }
            }
        });

        // Connect as a service
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();

        listViewDiscover.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                ConnectThread connectThread = new  ConnectThread(btDevice[i]);
                connectThread.start();
                showToast("connecting .. ");
            }
        });

        // bluetoothService
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string= String.valueOf(textMsg.getText());
                sendReceive.write(string.getBytes());
            }
        });

        // get paired devices btn click
        getPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                int index = 0;
                btDevice = new BluetoothDevice[pairedDevices.size()];
                if(mBluetoothAdapter.isEnabled()) {
                    if (pairedDevices.size() > 0) {
                        // There are paired devices. Get the name and address of each paired device.
                        for (BluetoothDevice device : pairedDevices) {
                            btDevice[index] = device;
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); // MAC address
                            pairedDevicesTV.append("\n" + deviceName + ", " + deviceHardwareAddress);
//                            listItems.add(deviceName + "\n" + deviceHardwareAddress);
                            index++;
                        }
//                if(bluetoothAdapter.isEnabled()){
//                    pairedDevicesTV.setText("paired devices");
//                    Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
//                    for(BluetoothDevice device : devices){
//                        pairedDevicesTV.append("\n device : " + device.getName() + ", " + device);
//                    }
                    } else {
                        // bluetooth is off (can't get devices)
                        showToast("no paired devices");
                    }
                }else{
                    showToast("turn on bluetooth to get paired devices");
                }
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    msg_box.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    msg_box.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    msg_box.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    msg_box.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
                textViewDiscover.setText("Discovering ... ");
                Log.e("STARTED", "started");
            }  else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceList.add(device.getName() + "\n" + device.getAddress());
                Log.e("BT", device.getName() + "\n" + device.getAddress());
                listViewDiscover.setAdapter(new ArrayAdapter<String>(context,
                        android.R.layout.simple_list_item_1, mDeviceList));

            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismiss progress dialog
                textViewDiscover.setText("finished");
                Log.e("FINISHED","finished");
                textViewDiscover.setText("Discovered devices");
            }
        }
    };

    // connect as a server
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                Log.e("", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    Log.e("from run block", "Socket's accept() method success");
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

//                    manageMyConnectedSocket(socket);
//                    mmServerSocket.close();
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("", "Could not close the connect socket", e);
            }
        }
    }

    //connect as a client
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
//            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("", "Socket's create() method failed", e);
            }
//            mmSocket = tmp;
        }


        public void run()
        {
            try {
                mmSocket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(mmSocket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
//        public void run() {
//            // Cancel discovery because it otherwise slows down the connection.
//            mBluetoothAdapter.cancelDiscovery();
//
//            try {
//                // Connect to the remote device through the socket. This call blocks
//                // until it succeeds or throws an exception.
//                mmSocket.connect();
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and return.
//                try {
//                    mmSocket.close();
//                } catch (IOException closeException) {
//                    Log.e("", "Could not close the client socket", closeException);
//                }
//                return;
//            }
//
//            // The connection attempt succeeded. Perform work associated with
//            // the connection in a separate thread.
//
//                  sendReceive=new SendReceive(mmSocket);
//                  sendReceive.start();
////            manageMyConnectedSocket(mmSocket);
//        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("", "Could not close the client socket", e);
            }
        }
    }




//    private class SendReceive extends Thread{
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//        private byte[] mmBuffer; // mmBuffer store for the stream
//
//
//        public SendReceive(BluetoothSocket socket){
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the input and output streams; using temp objects because
//            // member streams are final.
//
//                try {
//                    tmpIn = socket.getInputStream();
//                } catch (IOException e) {
//                    Log.e("", "Error occurred when creating input stream", e);
//                }
//                try {
//                    tmpOut = socket.getOutputStream();
//                } catch (IOException e) {
//                    Log.e("", "Error occurred when creating output stream", e);
//                }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            mmBuffer = new byte[1024];
//            int numBytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs.
//            while (true) {
//                try {
//                    // Read from the InputStream.
//                    numBytes = mmInStream.read(mmBuffer);
//                    // Send the obtained bytes to the UI activity.
//                    Message readMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_READ, numBytes, -1,
//                            mmBuffer);
//                    readMsg.sendToTarget();
//                    msg_box.setText(readMsg.toString());
//                } catch (IOException e) {
//                    Log.d("", "Input stream was disconnected", e);
//                    break;
//                }
//            }
//        }
//        public void write(byte[] bytes) {
//
//            try {
//                    mmOutStream.write(bytes);
//
//                    // Share the sent message with the UI activity.
//                    Message writtenMsg = handler.obtainMessage(
//                            MessageConstants.MESSAGE_WRITE, -1, -1, bytes);
//                    writtenMsg.sendToTarget();
//                } catch (IOException e) {
//                    Log.e("", "Error occurred when sending data", e);
//    //
//    //                // Send a failure message back to the activity.
//    //                Message writeErrorMsg =
//    //                        handler.obtainMessage(MyBluetoothService.MessageConstants.MESSAGE_TOAST);
//    //                Bundle bundle = new Bundle();
//    //                bundle.putString("toast",
//    //                        "Couldn't send data to the other device");
//    //                writeErrorMsg.setData(bundle);
//    //                handler.sendMessage(writeErrorMsg);
//                }
//
//        }
//    }

    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;
        }

        public void run()
        {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // toast message function
    private void showToast(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show();
    }
}

