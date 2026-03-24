package com.swiftshare.app.p2p;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Core P2P socket manager for high-speed data transfer over Wi-Fi Direct.
 */
public class P2PSocketManager {

    private static final String TAG = "P2PSocketManager";
    public static final int P2P_PORT = 8988;
    private static final int SOCKET_TIMEOUT = 5000;

    private final Handler mainHandler;
    private final SocketCallback callback;

    private ServerThread serverThread;
    private ClientThread clientThread;
    private TransferThread transferThread;

    public P2PSocketManager(SocketCallback callback) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.callback = callback;
    }

    /**
     * Starts listening as a server.
     */
    public synchronized void startServer() {
        stop();
        serverThread = new ServerThread();
        serverThread.start();
    }

    /**
     * Connects to a server at the specified IP address.
     */
    public synchronized void startClient(String hostAddress) {
        stop();
        clientThread = new ClientThread(hostAddress);
        clientThread.start();
    }

    /**
     * Stops all threads and close sockets.
     */
    public synchronized void stop() {
        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
        }
        if (clientThread != null) {
            clientThread.cancel();
            clientThread = null;
        }
        if (transferThread != null) {
            transferThread.cancel();
            transferThread = null;
        }
    }

    /**
     * Sends data through the active transfer thread.
     */
    public void writeMessage(byte[] data) {
        if (transferThread != null) {
            transferThread.write(data);
        }
    }

    private synchronized void onSocketConnected(Socket socket) {
        transferThread = new TransferThread(socket);
        transferThread.start();
        mainHandler.post(() -> callback.onSocketConnected(socket));
    }

    private class ServerThread extends Thread {
        private ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(P2P_PORT);
                Log.d(TAG, "Server started, listening on port " + P2P_PORT);
                Socket socket = serverSocket.accept();
                onSocketConnected(socket);
            } catch (IOException e) {
                Log.e(TAG, "Server socket error", e);
                mainHandler.post(() -> callback.onSocketError(e.getMessage()));
            }
        }

        void cancel() {
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close server socket", e);
            }
        }
    }

    private class ClientThread extends Thread {
        private final String host;
        private Socket socket;

        ClientThread(String host) {
            this.host = host;
        }

        @Override
        public void run() {
            socket = new Socket();
            try {
                socket.bind(null);
                socket.connect(new InetSocketAddress(host, P2P_PORT), SOCKET_TIMEOUT);
                Log.d(TAG, "Client connected to " + host);
                onSocketConnected(socket);
            } catch (IOException e) {
                Log.e(TAG, "Client socket error", e);
                mainHandler.post(() -> callback.onSocketError(e.getMessage()));
                try {
                    socket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Failed close client socket", e2);
                }
            }
        }

        void cancel() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close client socket", e);
            }
        }
    }

    private class TransferThread extends Thread {
        private final Socket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        TransferThread(Socket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error creating streams", e);
            }
            this.inputStream = tmpIn;
            this.outputStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024 * 64]; // 64KB buffer for high speed
            int bytes;

            while (!isInterrupted()) {
                try {
                    if (inputStream == null) break;
                    bytes = inputStream.read(buffer);
                    if (bytes == -1) {
                        mainHandler.post(callback::onSocketDisconnected);
                        break;
                    }
                    byte[] data = new byte[bytes];
                    System.arraycopy(buffer, 0, data, 0, bytes);
                    mainHandler.post(() -> callback.onDataReceived(data));
                } catch (IOException e) {
                    Log.e(TAG, "Read error", e);
                    mainHandler.post(callback::onSocketDisconnected);
                    break;
                }
            }
        }

        void write(byte[] data) {
            try {
                if (outputStream != null) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Write error", e);
                mainHandler.post(callback::onSocketDisconnected);
            }
        }

        void cancel() {
            try {
                interrupt();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing transfer socket", e);
            }
        }
    }

    public interface SocketCallback {
        void onSocketConnected(Socket socket);
        void onSocketDisconnected();
        void onDataReceived(byte[] data);
        void onSocketError(String error);
    }
}
