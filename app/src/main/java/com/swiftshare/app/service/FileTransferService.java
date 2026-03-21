package com.swiftshare.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.swiftshare.app.R;
import com.swiftshare.app.SwiftShareApp;
import com.swiftshare.app.bluetooth.BluetoothConnectionManager;
import com.swiftshare.app.bluetooth.FileTransferManager;
import com.swiftshare.app.data.model.TransferEntity;
import com.swiftshare.app.data.repository.TransferRepository;
import com.swiftshare.app.ui.MainActivity;
import com.swiftshare.app.utils.FileUtils;

/**
 * Foreground service managing Bluetooth file transfers.
 * Runs in the background with a persistent notification showing transfer progress.
 */
public class FileTransferService extends Service implements
        BluetoothConnectionManager.ConnectionCallback,
        FileTransferManager.TransferCallback {

    private static final String TAG = "FileTransferService";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new TransferBinder();
    private BluetoothConnectionManager connectionManager;
    private FileTransferManager transferManager;
    private TransferRepository repository;
    private NotificationManager notificationManager;

    private long currentTransferId = -1;
    private ServiceCallback serviceCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        connectionManager = new BluetoothConnectionManager(this, this);
        transferManager = new FileTransferManager(this, connectionManager, this);
        repository = new TransferRepository(getApplication());
        notificationManager = getSystemService(NotificationManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("SwiftShare", "Ready for file transfer"));
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectionManager.stop();
    }

    // ── Public API ───────────────────────────────────

    /**
     * Starts listening for incoming Bluetooth connections (receiver mode).
     */
    public void startListening() {
        connectionManager.startListening();
        updateNotification("Waiting for connection", "Discoverable and ready to receive");
    }

    /**
     * Connects to a remote Bluetooth device and sends a file.
     */
    public void connectAndSend(BluetoothDevice device, Uri fileUri) {
        connectionManager.connect(device);
        // File send will be triggered after connection is established
        // Store the URI to send after connection
        pendingFileUri = fileUri;
    }

    private Uri pendingFileUri;

    /**
     * Sends a file over the established connection.
     */
    public void sendFile(Uri fileUri) {
        if (connectionManager.getState() == BluetoothConnectionManager.STATE_CONNECTED) {
            createTransferRecord(fileUri, true);
            transferManager.sendFile(fileUri);
        }
    }

    /**
     * Cancels the current transfer.
     */
    public void cancelTransfer() {
        transferManager.cancelTransfer();
        connectionManager.stop();
        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "CANCELLED");
        }
    }

    public void setServiceCallback(ServiceCallback callback) {
        this.serviceCallback = callback;
    }

    public BluetoothConnectionManager getConnectionManager() {
        return connectionManager;
    }

    // ── BluetoothConnectionManager Callbacks ─────────

    @Override
    public void onStateChanged(int state) {
        if (serviceCallback != null) {
            serviceCallback.onConnectionStateChanged(state);
        }
    }

    @Override
    public void onConnected(BluetoothDevice device) {
        String deviceName;
        try {
            deviceName = device.getName() != null ? device.getName() : "Unknown Device";
        } catch (SecurityException e) {
            deviceName = "Unknown Device";
        }
        updateNotification("Connected", "Connected to " + deviceName);

        if (serviceCallback != null) {
            serviceCallback.onDeviceConnected(device);
        }

        // If we have a pending file to send, send it now
        if (pendingFileUri != null) {
            sendFile(pendingFileUri);
            pendingFileUri = null;
        }
    }

    @Override
    public void onConnectionFailed(String error) {
        updateNotification("Connection Failed", error);
        if (serviceCallback != null) {
            serviceCallback.onConnectionFailed(error);
        }
    }

    @Override
    public void onConnectionLost() {
        updateNotification("Connection Lost", "The connection was interrupted");
        if (currentTransferId > 0 && transferManager.isTransferring()) {
            repository.updateStatus(currentTransferId, "FAILED");
        }
        if (serviceCallback != null) {
            serviceCallback.onConnectionLost();
        }
    }

    @Override
    public void onDataReceived(byte[] data) {
        transferManager.onDataReceived(data);
    }

    @Override
    public void onDataSent(int byteCount) {
        // Handled by FileTransferManager
    }

    // ── FileTransferManager Callbacks ────────────────

    @Override
    public void onTransferStarted(String fileName, long fileSize, boolean isSending) {
        String action = isSending ? "Sending" : "Receiving";
        updateNotification(action + ": " + fileName, FileUtils.formatFileSize(fileSize));

        if (!isSending) {
            createReceiveRecord(fileName, fileSize);
        }

        if (serviceCallback != null) {
            serviceCallback.onTransferStarted(fileName, fileSize, isSending);
        }
    }

    @Override
    public void onProgressUpdated(int progress, long speed, long eta,
                                  long bytesTransferred, long totalBytes) {
        updateProgressNotification(progress);

        if (currentTransferId > 0) {
            repository.updateProgress(currentTransferId, "IN_PROGRESS", progress);
            repository.updateSpeed(currentTransferId, speed);
        }

        if (serviceCallback != null) {
            serviceCallback.onTransferProgress(progress, speed, eta, bytesTransferred, totalBytes);
        }
    }

    @Override
    public void onTransferComplete(String fileName, long fileSize, boolean wasSending) {
        String action = wasSending ? "Sent" : "Received";
        updateNotification("Transfer Complete", action + ": " + fileName);

        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "COMPLETED");
        }

        if (serviceCallback != null) {
            serviceCallback.onTransferComplete(fileName, fileSize, wasSending);
        }
    }

    @Override
    public void onTransferFailed(String error) {
        updateNotification("Transfer Failed", error);

        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "FAILED");
        }

        if (serviceCallback != null) {
            serviceCallback.onTransferFailed(error);
        }
    }

    @Override
    public void onTransferCancelled() {
        updateNotification("Transfer Cancelled", "The transfer was cancelled");

        if (serviceCallback != null) {
            serviceCallback.onTransferCancelled();
        }
    }

    // ── Notification Helpers ─────────────────────────

    private Notification buildNotification(String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, SwiftShareApp.CHANNEL_TRANSFER)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void updateNotification(String title, String text) {
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(title, text));
        }
    }

    private void updateProgressNotification(int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, SwiftShareApp.CHANNEL_TRANSFER)
                .setContentTitle("Transferring...")
                .setContentText(progress + "% complete")
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .setProgress(100, progress, false)
                .build();

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    // ── Transfer Record Helpers ──────────────────────

    private void createTransferRecord(Uri fileUri, boolean isSending) {
        TransferEntity transfer = new TransferEntity();
        transfer.setFileName(FileUtils.getFileName(this, fileUri));
        transfer.setFilePath(fileUri.toString());
        transfer.setFileSize(FileUtils.getFileSize(this, fileUri));
        transfer.setMimeType(FileUtils.getMimeType(this, fileUri));
        transfer.setDirection(isSending ? "SENT" : "RECEIVED");
        transfer.setStatus("IN_PROGRESS");
        transfer.setStartTime(System.currentTimeMillis());

        repository.insert(transfer, id -> currentTransferId = id);
    }

    private void createReceiveRecord(String fileName, long fileSize) {
        TransferEntity transfer = new TransferEntity();
        transfer.setFileName(fileName);
        transfer.setFileSize(fileSize);
        transfer.setDirection("RECEIVED");
        transfer.setStatus("IN_PROGRESS");
        transfer.setStartTime(System.currentTimeMillis());

        repository.insert(transfer, id -> currentTransferId = id);
    }

    // ── Binder ───────────────────────────────────────

    public class TransferBinder extends Binder {
        public FileTransferService getService() {
            return FileTransferService.this;
        }
    }

    // ── Service Callback Interface ───────────────────

    public interface ServiceCallback {
        void onConnectionStateChanged(int state);
        void onDeviceConnected(BluetoothDevice device);
        void onConnectionFailed(String error);
        void onConnectionLost();
        void onTransferStarted(String fileName, long fileSize, boolean isSending);
        void onTransferProgress(int progress, long speed, long eta,
                                long bytesTransferred, long totalBytes);
        void onTransferComplete(String fileName, long fileSize, boolean wasSending);
        void onTransferFailed(String error);
        void onTransferCancelled();
    }
}
