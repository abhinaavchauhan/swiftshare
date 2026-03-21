package com.swiftshare.app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.swiftshare.app.R;
import com.swiftshare.app.SwiftShareApp;
import com.swiftshare.app.data.model.DeviceItem;
import com.swiftshare.app.ui.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that manages Bluetooth device discovery.
 * Broadcasts found devices to bound UI components.
 */
public class BluetoothConnectionService extends Service {

    private static final String TAG = "BTConnectionService";
    private static final int NOTIFICATION_ID = 1002;

    private final IBinder binder = new ConnectionBinder();
    private BluetoothAdapter bluetoothAdapter;
    private final List<DeviceItem> discoveredDevices = new ArrayList<>();
    private DiscoveryCallback discoveryCallback;
    private boolean isDiscovering = false;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                    if (device != null) {
                        boolean isPaired;
                        try {
                            isPaired = device.getBondState() == BluetoothDevice.BOND_BONDED;
                        } catch (SecurityException e) {
                            isPaired = false;
                        }

                        DeviceItem item = new DeviceItem(device, isPaired, rssi);

                        // Avoid duplicates
                        if (!discoveredDevices.contains(item)) {
                            discoveredDevices.add(item);
                            if (discoveryCallback != null) {
                                discoveryCallback.onDeviceFound(item);
                            }
                        }
                    }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    isDiscovering = true;
                    if (discoveryCallback != null) {
                        discoveryCallback.onDiscoveryStarted();
                    }
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    isDiscovering = false;
                    if (discoveryCallback != null) {
                        discoveryCallback.onDiscoveryFinished();
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register receiver for device discovery broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDiscovery();
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Receiver already unregistered");
        }
    }

    // ── Public API ───────────────────────────────────

    /**
     * Starts Bluetooth device discovery.
     */
    public void startDiscovery() {
        if (bluetoothAdapter == null) return;

        discoveredDevices.clear();

        // Add bonded (paired) devices first
        try {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                DeviceItem item = new DeviceItem(device, true, -50);
                discoveredDevices.add(item);
                if (discoveryCallback != null) {
                    discoveryCallback.onDeviceFound(item);
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot get bonded devices", e);
        }

        // Start scanning for new devices
        try {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            Log.e(TAG, "Cannot start discovery", e);
            if (discoveryCallback != null) {
                discoveryCallback.onDiscoveryError("Bluetooth permission denied");
            }
        }
    }

    /**
     * Stops Bluetooth device discovery.
     */
    public void stopDiscovery() {
        if (bluetoothAdapter != null) {
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Cannot cancel discovery", e);
            }
        }
        isDiscovering = false;
    }

    /**
     * Returns the list of discovered devices.
     */
    public List<DeviceItem> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public boolean isDiscovering() {
        return isDiscovering;
    }

    public void setDiscoveryCallback(DiscoveryCallback callback) {
        this.discoveryCallback = callback;
    }

    // ── Notification ─────────────────────────────────

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, SwiftShareApp.CHANNEL_TRANSFER)
                .setContentTitle("SwiftShare")
                .setContentText("Scanning for nearby devices...")
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    // ── Binder ───────────────────────────────────────

    public class ConnectionBinder extends Binder {
        public BluetoothConnectionService getService() {
            return BluetoothConnectionService.this;
        }
    }

    // ── Callback ─────────────────────────────────────

    public interface DiscoveryCallback {
        void onDiscoveryStarted();
        void onDeviceFound(DeviceItem device);
        void onDiscoveryFinished();
        void onDiscoveryError(String error);
    }
}
