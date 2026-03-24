package com.swiftshare.app.p2p;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for Wi-Fi Direct (P2P) operations.
 * Handles peer discovery, connection management, and P2P info retrieval.
 */
public class WiFiDirectManager {

    private static final String TAG = "WiFiDirectManager";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final P2PCallback callback;
    private final Handler mainHandler;

    private boolean isDiscoveryActive = false;
    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pInfo connectionInfo;

    public WiFiDirectManager(Context context, P2PCallback callback) {
        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, Looper.getMainLooper(), null);
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Starts discovering nearby Wi-Fi Direct devices.
     */
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        if (isDiscoveryActive) return;

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovery started successfully");
                isDiscoveryActive = true;
                mainHandler.post(callback::onDiscoveryStarted);
                // Proactively request peers once to populate initial list
                requestPeers();
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Discovery failed: " + reason);
                mainHandler.post(() -> callback.onDiscoveryFailed(reason));
            }
        });
    }

    /**
     * Stops peer discovery.
     */
    public void stopDiscovery() {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                isDiscoveryActive = false;
                mainHandler.post(callback::onDiscoveryStopped);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Stop discovery failed: " + reason);
            }
        });
    }

    /**
     * Connects to a specific Wi-Fi Direct device.
     */
    @SuppressLint("MissingPermission")
    public void connect(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connect init successful for: " + device.deviceName);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connect failed: " + reason);
                mainHandler.post(() -> callback.onConnectionFailed(reason));
            }
        });
    }

    /**
     * Disconnects from the current P2P group.
     */
    public void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnected from group");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Disconnect failed: " + reason);
            }
        });
    }

    /**
     * Called when Wi-Fi Direct state changes.
     */
    public void onP2PStateChanged(boolean enabled) {
        mainHandler.post(() -> callback.onP2PStateChanged(enabled));
    }

    /**
     * Returns the list of discovered peers.
     */
    public List<WifiP2pDevice> getPeers() {
        return peers;
    }

    /**
     * Updates the peer list when notified by the BroadcastReceiver.
     */
    @SuppressLint("MissingPermission")
    public void requestPeers() {
        manager.requestPeers(channel, peersList -> {
            peers.clear();
            peers.addAll(peersList.getDeviceList());
            mainHandler.post(() -> callback.onPeersAvailable(peers));
        });
    }

    /**
     * Requests connection info when notified by the BroadcastReceiver.
     */
    public void requestConnectionInfo() {
        manager.requestConnectionInfo(channel, info -> {
            this.connectionInfo = info;
            mainHandler.post(() -> callback.onConnectionInfoAvailable(info));
        });
    }

    /**
     * Callback interface for Wi-Fi Direct events.
     */
    public interface P2PCallback {
        void onDiscoveryStarted();
        void onDiscoveryFailed(int reason);
        void onDiscoveryStopped();
        void onPeersAvailable(List<WifiP2pDevice> peers);
        void onConnectionInfoAvailable(WifiP2pInfo info);
        void onConnectionFailed(int reason);
        void onP2PStateChanged(boolean enabled);
    }
}
