package com.bignerdranch.android.visual_control_by_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

class SimpleBluetooth implements Serializable {

    //下面是蓝牙传输的声明
    private transient UUID BT_UUID = UUID.randomUUID();
    // bluetooth background worker thread to send and receive data
    private transient BluetoothAdapter mBluetoothAdapter;
    private transient ConnectThread mConnectThread;
    private transient ConnectedThread mConnectedThread;
    private transient boolean ConnectSuccess = false;

    SimpleBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    boolean ConnectDevice(String address) {
        //建立传输信息通道
        mConnectThread = new SimpleBluetooth.ConnectThread(mBluetoothAdapter.getRemoteDevice(address));
        mConnectThread.run();
        return ConnectSuccess;
    }

    void ConnectCancel() {
        mConnectThread.cancel();
        mConnectedThread.cancel();
    }

    class ConnectThread extends Thread implements Serializable {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BT_UUID);
            } catch (IOException ignored) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                //下面处理是在 StackOverflow 找的，貌似没用上
                Log.e("Socket connect:",connectException.getMessage());
                try {
                    Log.e("","trying fallback...");
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();
                    Log.e("","Connected");
                } catch (Exception e2) {
                    Log.e("", "Couldn't establish Bluetooth connection!\n" + e2.getMessage());
                    try {
                        mmSocket.close();
                    } catch (IOException ignored) { }
                    return;
                }
            }
            ConnectSuccess = true;
            // Do work to manage the connection (in a separate thread)
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        /** Will cancel an in-progress connection, and close the socket */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    class ConnectedThread extends Thread implements Serializable {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            // Get the output streams, using temp objects because
            // member streams are final
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmOutStream = tmpOut;
        }

        /* Call this from the main activity to send data to the remote device */
        void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("OutStream write:",e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    ConnectedThread getConnectedThread() {
        return mConnectedThread;
    }

    public boolean isConnectSuccess() {
        return ConnectSuccess;
    }
}
