package com.swiftshare.app.p2p;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.swiftshare.app.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Manages file transfer protocol over P2P sockets.
 * Handles metadata exchange and large file stream processing.
 */
public class P2PFileTransferManager {

    private static final String TAG = "P2PFileTransferManager";
    private static final int BUFFER_SIZE = 1024 * 128; // 128KB buffer

    public interface TransferCallback {
        void onProgress(int progress, long speed, long eta, long transferred, long total);
        void onComplete(String fileName);
        void onError(String error);
    }

    /**
     * Sends a file to the connected socket.
     */
    public void sendFile(Context context, Socket socket, Uri fileUri, TransferCallback callback) {
        new Thread(() -> {
            try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                 DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

                String fileName = FileUtils.getFileName(context, fileUri);
                long fileSize = FileUtils.getFileSize(context, fileUri);

                // 1. Send Metadata
                outputStream.writeUTF(fileName);
                outputStream.writeLong(fileSize);
                outputStream.flush();

                // 2. Send File Data
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesTransferred = 0;
                int bytesRead;
                long startTime = System.currentTimeMillis();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesTransferred += bytesRead;

                    // Calculate speed and ETA
                    long currentTime = System.currentTimeMillis();
                    long timeElapsed = currentTime - startTime;
                    if (timeElapsed > 500) { // Update ogni 500ms
                         long speed = (bytesTransferred * 1000) / timeElapsed; // bytes/s
                         int progress = (int) ((bytesTransferred * 100) / fileSize);
                         long eta = (fileSize - bytesTransferred) / (speed > 0 ? speed : 1);
                         
                         callback.onProgress(progress, speed, eta, bytesTransferred, fileSize);
                    }
                }
                outputStream.flush();
                callback.onComplete(fileName);

            } catch (IOException e) {
                Log.e(TAG, "Send failed", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Receives a file from the connected socket.
     */
    public void receiveFile(Context context, Socket socket, FileUtils.SaveCallback saveCallback, TransferCallback callback) {
        new Thread(() -> {
            try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

                // 1. Read Metadata
                String fileName = inputStream.readUTF();
                long fileSize = inputStream.readLong();

                // 2. Prepare Output Stream
                OutputStream outputStream = saveCallback.getOutputStream(fileName, fileSize);
                if (outputStream == null) {
                    callback.onError("Could not create output stream");
                    return;
                }

                // 3. Receive File Data
                byte[] buffer = new byte[BUFFER_SIZE];
                long bytesReceived = 0;
                int bytesRead;
                long startTime = System.currentTimeMillis();

                while (bytesReceived < fileSize && (bytesRead = inputStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize - bytesReceived))) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesReceived += bytesRead;

                    // Update UI
                    long currentTime = System.currentTimeMillis();
                    long timeElapsed = currentTime - startTime;
                    if (timeElapsed > 500) {
                        long speed = (bytesReceived * 1000) / (timeElapsed > 0 ? timeElapsed : 1);
                        int progress = (int) ((bytesReceived * 100) / (fileSize > 0 ? fileSize : 1));
                        long eta = (fileSize - bytesReceived) / (speed > 0 ? speed : 1);
                        callback.onProgress(progress, speed, eta, bytesReceived, fileSize);
                    }
                }
                outputStream.flush();
                outputStream.close();
                callback.onComplete(fileName);

            } catch (IOException e) {
                Log.e(TAG, "Receive failed", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}
