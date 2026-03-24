package com.swiftshare.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * DEPRECATED: This service was used for Bluetooth discovery.
 * SwiftShare has migrated to Wi-Fi Direct for high-speed transfers.
 * See WiFiDirectManager and FileTransferService for the new implementation.
 */
public class BluetoothConnectionService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
