package com.swiftshare.app.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.swiftshare.app.utils.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Manages bidirectional file transfer over Bluetooth.
 * Handles file chunking, metadata headers, progress tracking, and speed calculation.
 *
 * Protocol:
 * 1. Sender transmits a header: [4 bytes fileNameLength][fileName][8 bytes fileSize]
 * 2. Sender streams file data in 4KB chunks
 * 3. Receiver reads header, then accumulates data until fileSize is reached
 */
public class FileTransferManager {

    private static final String TAG = "FileTransferManager";
    private static final int CHUNK_SIZE = 4096; // 4KB chunks for Bluetooth throughput

    private final Context context;
    private final BluetoothConnectionManager connectionManager;
    private final Handler mainHandler;
    private final TransferCallback callback;

    private boolean isSending;
    private boolean isReceiving;
    private long totalBytesToSend;
    private long totalBytesSent;
    private long totalBytesToReceive;
    private long totalBytesReceived;
    private long transferStartTime;
    private String receivingFileName;

    // Receive buffer
    private ByteArrayOutputStream receiveBuffer;
    private boolean headerReceived;

    public FileTransferManager(Context context, BluetoothConnectionManager connectionManager,
                               TransferCallback callback) {
        this.context = context;
        this.connectionManager = connectionManager;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.callback = callback;
        this.receiveBuffer = new ByteArrayOutputStream();
    }

    /**
     * Sends a file to the connected device.
     *
     * @param uri The content URI of the file to send
     */
    public void sendFile(Uri uri) {
        if (isSending) {
            Log.w(TAG, "Already sending a file");
            return;
        }

        new Thread(() -> {
            try {
                isSending = true;
                transferStartTime = System.currentTimeMillis();

                String fileName = FileUtils.getFileName(context, uri);
                long fileSize = FileUtils.getFileSize(context, uri);
                totalBytesToSend = fileSize;
                totalBytesSent = 0;

                mainHandler.post(() -> callback.onTransferStarted(fileName, fileSize, true));

                // ── Send header ──────────────────────────
                byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
                ByteBuffer header = ByteBuffer.allocate(4 + nameBytes.length + 8);
                header.putInt(nameBytes.length);
                header.put(nameBytes);
                header.putLong(fileSize);
                connectionManager.write(header.array());

                // Small delay for header processing
                Thread.sleep(100);

                // ── Stream file data ─────────────────────
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("Cannot open file input stream");
                }

                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byte[] chunk = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                    connectionManager.write(chunk);
                    totalBytesSent += bytesRead;

                    // Calculate and report progress
                    int progress = (int) ((totalBytesSent * 100) / totalBytesToSend);
                    long elapsedMs = System.currentTimeMillis() - transferStartTime;
                    long speed = elapsedMs > 0 ? (totalBytesSent * 1000) / elapsedMs : 0;
                    long remainingBytes = totalBytesToSend - totalBytesSent;
                    long eta = speed > 0 ? remainingBytes / speed : -1;

                    mainHandler.post(() -> callback.onProgressUpdated(
                            progress, speed, eta, totalBytesSent, totalBytesToSend));

                    // Throttle slightly for BT stability
                    Thread.sleep(5);
                }

                inputStream.close();
                isSending = false;

                mainHandler.post(() -> callback.onTransferComplete(
                        fileName, fileSize, true));

            } catch (Exception e) {
                Log.e(TAG, "Send failed", e);
                isSending = false;
                mainHandler.post(() -> callback.onTransferFailed(e.getMessage()));
            }
        }).start();
    }

    /**
     * Processes received data chunks. Assembles file from protocol header + data.
     */
    public void onDataReceived(byte[] data) {
        try {
            receiveBuffer.write(data);
            byte[] allData = receiveBuffer.toByteArray();

            // ── Parse header if not yet received ─────
            if (!headerReceived) {
                if (allData.length < 4) return; // Wait for more data

                ByteBuffer headerBuf = ByteBuffer.wrap(allData);
                int nameLength = headerBuf.getInt();

                if (allData.length < 4 + nameLength + 8) return; // Wait for complete header

                byte[] nameBytes = new byte[nameLength];
                headerBuf.get(nameBytes);
                receivingFileName = new String(nameBytes, StandardCharsets.UTF_8);
                totalBytesToReceive = headerBuf.getLong();
                totalBytesReceived = 0;
                headerReceived = true;
                transferStartTime = System.currentTimeMillis();
                isReceiving = true;

                mainHandler.post(() -> callback.onTransferStarted(
                        receivingFileName, totalBytesToReceive, false));

                // Remove header data from buffer
                int headerSize = 4 + nameLength + 8;
                int remaining = allData.length - headerSize;
                receiveBuffer.reset();
                if (remaining > 0) {
                    byte[] fileData = new byte[remaining];
                    System.arraycopy(allData, headerSize, fileData, 0, remaining);
                    receiveBuffer.write(fileData);
                    totalBytesReceived += remaining;
                }
            } else {
                // Accumulate file data
                totalBytesReceived = receiveBuffer.size();
            }

            // ── Report progress ──────────────────────
            if (headerReceived && totalBytesToReceive > 0) {
                int progress = (int) ((totalBytesReceived * 100) / totalBytesToReceive);
                long elapsedMs = System.currentTimeMillis() - transferStartTime;
                long speed = elapsedMs > 0 ? (totalBytesReceived * 1000) / elapsedMs : 0;
                long remainingBytes = totalBytesToReceive - totalBytesReceived;
                long eta = speed > 0 ? remainingBytes / speed : -1;

                mainHandler.post(() -> callback.onProgressUpdated(
                        progress, speed, eta, totalBytesReceived, totalBytesToReceive));

                // ── Check if transfer is complete ────
                if (totalBytesReceived >= totalBytesToReceive) {
                    saveReceivedFile();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing received data", e);
            mainHandler.post(() -> callback.onTransferFailed(e.getMessage()));
        }
    }

    /**
     * Saves the fully received file to the device storage.
     */
    private void saveReceivedFile() {
        try {
            String savePath = context.getExternalFilesDir(null) +
                    File.separator + "SwiftShare" + File.separator + receivingFileName;

            File saveFile = new File(savePath);
            saveFile.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.write(receiveBuffer.toByteArray());
            fos.close();

            isReceiving = false;
            headerReceived = false;
            receiveBuffer.reset();

            long fileSize = totalBytesToReceive;
            String fileName = receivingFileName;

            mainHandler.post(() -> callback.onTransferComplete(fileName, fileSize, false));

        } catch (Exception e) {
            Log.e(TAG, "Error saving file", e);
            mainHandler.post(() -> callback.onTransferFailed("Failed to save file: " + e.getMessage()));
        }
    }

    /**
     * Cancels the current transfer.
     */
    public void cancelTransfer() {
        isSending = false;
        isReceiving = false;
        headerReceived = false;
        receiveBuffer.reset();
        mainHandler.post(() -> callback.onTransferCancelled());
    }

    /**
     * Returns whether a transfer is currently active.
     */
    public boolean isTransferring() {
        return isSending || isReceiving;
    }

    // ── Transfer Callback Interface ──────────────────

    /**
     * Callback for file transfer lifecycle events.
     */
    public interface TransferCallback {
        void onTransferStarted(String fileName, long fileSize, boolean isSending);
        void onProgressUpdated(int progress, long speed, long eta,
                               long bytesTransferred, long totalBytes);
        void onTransferComplete(String fileName, long fileSize, boolean wasSending);
        void onTransferFailed(String error);
        void onTransferCancelled();
    }
}
