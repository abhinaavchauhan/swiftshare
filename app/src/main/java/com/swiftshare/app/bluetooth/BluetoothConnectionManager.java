package com.swiftshare.app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Core Bluetooth connection manager handling RFCOMM socket operations.
 * Manages server acceptance threads, client connection threads,
 * and bidirectional data communication.
 */
public class BluetoothConnectionManager {

    private static final String TAG = "BTConnectionManager";
    private static final String APP_NAME = "SwiftShare";
    // Standard SPP UUID for Bluetooth serial communication
    public static final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private final ConnectionCallback callback;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private int currentState;

    // Connection States
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public BluetoothConnectionManager(Context context, ConnectionCallback callback) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.callback = callback;
        this.currentState = STATE_NONE;
    }

    /**
     * Returns the current connection state.
     */
    public synchronized int getState() {
        return currentState;
    }

    /**
     * Starts the server mode - listens for incoming connections.
     */
    public synchronized void startListening() {
        Log.d(TAG, "startListening");

        // Cancel any active connection attempts
        cancelConnectThread();
        cancelConnectedThread();

        // Start the accept thread
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        currentState = STATE_LISTENING;
        notifyStateChanged();
    }

    /**
     * Connects to a remote Bluetooth device.
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting to: " + device.getAddress());

        // Cancel any ongoing discovery or connection
        cancelConnectThread();
        cancelConnectedThread();

        // Start the connection thread
        connectThread = new ConnectThread(device);
        connectThread.start();

        currentState = STATE_CONNECTING;
        notifyStateChanged();
    }

    /**
     * Manages the established connection for data transfer.
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "Connected to: " + device.getAddress());

        // Cancel previous threads
        cancelConnectThread();
        cancelConnectedThread();
        cancelAcceptThread();

        // Start the connected thread for data transfer
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        currentState = STATE_CONNECTED;
        mainHandler.post(() -> callback.onConnected(device));
    }

    /**
     * Writes data to the connected remote device.
     */
    public void write(byte[] data) {
        ConnectedThread thread;
        synchronized (this) {
            if (currentState != STATE_CONNECTED) return;
            thread = connectedThread;
        }
        if (thread != null) {
            thread.write(data);
        }
    }

    /**
     * Stops all threads and resets state.
     */
    public synchronized void stop() {
        Log.d(TAG, "Stopping all threads");
        cancelConnectThread();
        cancelConnectedThread();
        cancelAcceptThread();
        currentState = STATE_NONE;
        notifyStateChanged();
    }

    // ── Helper Methods ───────────────────────────────

    private void cancelConnectThread() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private void cancelConnectedThread() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private void cancelAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }

    private void notifyStateChanged() {
        mainHandler.post(() -> callback.onStateChanged(currentState));
    }

    private void connectionFailed() {
        currentState = STATE_NONE;
        mainHandler.post(() -> callback.onConnectionFailed("Connection failed"));
    }

    private void connectionLost() {
        currentState = STATE_NONE;
        mainHandler.post(() -> callback.onConnectionLost());
    }

    // ── Accept Thread (Server) ───────────────────────

    /**
     * Thread that listens for incoming Bluetooth connections.
     */
    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        AcceptThread() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "AcceptThread: listen() failed", e);
            }
        }

        @Override
        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;

            while (currentState != STATE_CONNECTED && serverSocket != null) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "AcceptThread: accept() failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (BluetoothConnectionManager.this) {
                        switch (currentState) {
                            case STATE_LISTENING:
                            case STATE_CONNECTING:
                                try {
                                    connected(socket, socket.getRemoteDevice());
                                } catch (SecurityException e) {
                                    Log.e(TAG, "Security exception on connected", e);
                                }
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        void cancel() {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: close() failed", e);
            }
        }
    }

    // ── Connect Thread (Client) ──────────────────────

    /**
     * Thread that initiates a connection to a remote device.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice device;

        ConnectThread(BluetoothDevice device) {
            this.device = device;
            try {
                socket = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "ConnectThread: create() failed", e);
            }
        }

        @Override
        public void run() {
            setName("ConnectThread");

            // Cancel discovery to speed up connection
            try {
                bluetoothAdapter.cancelDiscovery();
            } catch (SecurityException e) {
                Log.w(TAG, "Cannot cancel discovery", e);
            }

            try {
                if (socket != null) {
                    socket.connect();
                }
            } catch (IOException | SecurityException e) {
                Log.e(TAG, "ConnectThread: connect() failed", e);
                try {
                    if (socket != null) socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "ConnectThread: close() failed", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the connect thread
            synchronized (BluetoothConnectionManager.this) {
                connectThread = null;
            }

            // Transition to connected state
            connected(socket, device);
        }

        void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: close() failed", e);
            }
        }
    }

    // ── Connected Thread (Data Transfer) ─────────────

    /**
     * Thread managing data transfer over an established Bluetooth connection.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: stream creation failed", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        @Override
        public void run() {
            setName("ConnectedThread");
            byte[] buffer = new byte[8192]; // 8KB buffer for efficient transfer
            int bytesRead;

            while (true) {
                try {
                    bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        connectionLost();
                        break;
                    }

                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);

                    mainHandler.post(() -> callback.onDataReceived(data));
                } catch (IOException e) {
                    Log.e(TAG, "ConnectedThread: read() failed", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Writes data to the output stream.
         */
        void write(byte[] data) {
            try {
                outputStream.write(data);
                outputStream.flush();
                mainHandler.post(() -> callback.onDataSent(data.length));
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: write() failed", e);
                connectionLost();
            }
        }

        void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: close() failed", e);
            }
        }
    }

    // ── Callback Interface ───────────────────────────

    /**
     * Callback interface for Bluetooth connection events.
     */
    public interface ConnectionCallback {
        void onStateChanged(int state);
        void onConnected(BluetoothDevice device);
        void onConnectionFailed(String error);
        void onConnectionLost();
        void onDataReceived(byte[] data);
        void onDataSent(int byteCount);
    }
}
