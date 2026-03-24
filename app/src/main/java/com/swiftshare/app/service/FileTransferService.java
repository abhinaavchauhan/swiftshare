package com.swiftshare.app.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.swiftshare.app.R;
import com.swiftshare.app.SwiftShareApp;
import com.swiftshare.app.p2p.P2PFileTransferManager;
import com.swiftshare.app.p2p.P2PSocketManager;
import com.swiftshare.app.p2p.WiFiDirectBroadcastReceiver;
import com.swiftshare.app.p2p.WiFiDirectManager;
import com.swiftshare.app.data.model.TransferEntity;
import com.swiftshare.app.data.repository.TransferRepository;
import com.swiftshare.app.ui.MainActivity;
import com.swiftshare.app.utils.FileUtils;

import java.net.Socket;
import java.util.List;

/**
 * Foreground service managing Wi-Fi Direct file transfers.
 * Orchestrates device discovery, connection, and high-speed data transfer.
 */
public class FileTransferService extends Service implements
        WiFiDirectManager.P2PCallback,
        P2PSocketManager.SocketCallback,
        P2PFileTransferManager.TransferCallback {

    private static final String TAG = "FileTransferService";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new TransferBinder();
    private WiFiDirectManager p2pManager;
    private P2PSocketManager socketManager;
    private P2PFileTransferManager transferManager;
    private WiFiDirectBroadcastReceiver receiver;
    private TransferRepository repository;
    private NotificationManager notificationManager;

    private Socket activeSocket;
    private long currentTransferId = -1;
    private ServiceCallback serviceCallback;
    private Uri pendingFileUri;
    private boolean isReceiver = false;

    @Override
    public void onCreate() {
        super.onCreate();
        p2pManager = new WiFiDirectManager(this, this);
        socketManager = new P2PSocketManager(this);
        transferManager = new P2PFileTransferManager();
        repository = new TransferRepository(getApplication());
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Register Wi-Fi Direct BroadcastReceiver
        receiver = new WiFiDirectBroadcastReceiver(p2pManager);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
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
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.e(TAG, "Receiver already unregistered", e);
        }
        socketManager.stop();
        p2pManager.disconnect();
    }

    // ── Public API ───────────────────────────────────

    public void startDiscovery() {
        p2pManager.startDiscovery();
    }

    public void stopDiscovery() {
        p2pManager.stopDiscovery();
    }

    public void connect(WifiP2pDevice device, Uri fileUri) {
        this.pendingFileUri = fileUri;
        this.isReceiver = false;
        p2pManager.connect(device);
    }

    public void startReceiverMode() {
        this.isReceiver = true;
        p2pManager.startDiscovery(); // Need discovery to be visible
        updateNotification("Waiting for Sender", "Discoverable via Wi-Fi Direct");
    }

    public void cancelTransfer() {
        socketManager.stop();
        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "CANCELLED");
        }
        if (serviceCallback != null) {
            serviceCallback.onTransferCancelled();
        }
    }

    public void setServiceCallback(ServiceCallback callback) {
        this.serviceCallback = callback;
    }

    public WiFiDirectManager getP2pManager() {
        return p2pManager;
    }

    // ── WiFiDirectManager.P2PCallback ────────────────

    @Override
    public void onDiscoveryStarted() {
        Log.d(TAG, "Discovery started");
        if (serviceCallback != null) {
            serviceCallback.onDiscoveryStarted();
        }
    }

    @Override
    public void onDiscoveryFailed(int reason) {
        Log.e(TAG, "Discovery failed: " + reason);
        if (serviceCallback != null) {
            serviceCallback.onDiscoveryFailed(reason);
        }
    }

    @Override
    public void onDiscoveryStopped() {
        Log.d(TAG, "Discovery stopped");
    }

    @Override
    public void onPeersAvailable(List<WifiP2pDevice> peers) {
        if (serviceCallback != null) {
            serviceCallback.onPeersDiscovered(peers);
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d(TAG, "Connection info available. Group Owner: " + info.isGroupOwner);
        
        if (info.groupFormed) {
            if (info.isGroupOwner) {
                // I am the receiver (usually)
                socketManager.startServer();
            } else {
                // I am the sender
                socketManager.startClient(info.groupOwnerAddress.getHostAddress());
            }
        }
    }

    @Override
    public void onConnectionFailed(int reason) {
        updateNotification("Connection Failed", "Reason: " + reason);
        if (serviceCallback != null) {
            serviceCallback.onConnectionFailed("P2P Error " + reason);
        }
    }

    @Override
    public void onP2PStateChanged(boolean enabled) {
        Log.d(TAG, "Wi-Fi P2P Enabled: " + enabled);
    }

    // ── P2PSocketManager.SocketCallback ──────────────

    @Override
    public void onSocketConnected(Socket socket) {
        Log.d(TAG, "Socket connected!");
        handleConnection(socket);
        if (serviceCallback != null) {
            serviceCallback.onDeviceConnected("Nearby Device");
        }
    }

    @Override
    public void onSocketDisconnected() {
        updateNotification("Disconnected", "Connection lost");
        if (serviceCallback != null) {
            serviceCallback.onConnectionLost();
        }
    }

    @Override
    public void onDataReceived(byte[] data) {
        // Not used directly here; P2PFileTransferManager handles the streams
    }

    @Override
    public void onSocketError(String error) {
        updateNotification("Socket Error", error);
        if (serviceCallback != null) {
            serviceCallback.onConnectionFailed(error);
        }
    }

    // ── Bridge connecting Socket -> FileTransfer ─────

    /**
     * Called by P2PSocketManager internal threads when a raw socket is ready.
     * We override this to trigger our file transfer logic.
     */
    public void handleConnection(Socket socket) {
        this.activeSocket = socket;
        if (pendingFileUri != null) {
            // I am the Sender
            createTransferRecord(pendingFileUri, true);
            transferManager.sendFile(this, socket, pendingFileUri, this);
        } else {
            // I am the Receiver
            transferManager.receiveFile(this, socket, (fileName, size) -> FileUtils.saveReceivedFile(this, fileName, size), this);
        }
    }

    // ── P2PFileTransferManager.TransferCallback ──────

    @Override
    public void onProgress(int progress, long speed, long eta, long transferred, long total) {
        updateProgressNotification(progress);
        if (currentTransferId > 0) {
            repository.updateProgress(currentTransferId, "IN_PROGRESS", progress);
            repository.updateSpeed(currentTransferId, speed);
        }
        if (serviceCallback != null) {
            serviceCallback.onTransferProgress(progress, speed, eta, transferred, total);
        }
    }

    @Override
    public void onComplete(String fileName) {
        updateNotification("Transfer complete", fileName);
        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "COMPLETED");
        }
        if (serviceCallback != null) {
            serviceCallback.onTransferComplete(fileName, 0, !isReceiver);
        }
    }

    @Override
    public void onError(String error) {
        updateNotification("Transfer Failed", error);
        if (currentTransferId > 0) {
            repository.updateStatus(currentTransferId, "FAILED");
        }
        if (serviceCallback != null) {
            serviceCallback.onTransferFailed(error);
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
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
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
        Notification notification = new NotificationCompat.Builder(this, SwiftShareApp.CHANNEL_TRANSFER)
                .setContentTitle("Transferring...")
                .setContentText(progress + "% complete")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
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
        transfer.setDirection("SENT");
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
        void onDiscoveryStarted();
        void onDiscoveryFailed(int reason);
        void onPeersDiscovered(List<WifiP2pDevice> peers);
        void onDeviceConnected(String deviceName);
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
