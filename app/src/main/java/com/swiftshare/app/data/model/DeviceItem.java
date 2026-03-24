package com.swiftshare.app.data.model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Model representing a discovered P2P device.
 * Wraps WiFi Direct device with additional UI metadata.
 */
public class DeviceItem {

    private WifiP2pDevice device;
    private String name;
    private String address;
    private boolean isConnecting;
    private int status;

    public DeviceItem(WifiP2pDevice device) {
        this.device = device;
        this.name = (device.deviceName == null || device.deviceName.isEmpty()) 
                    ? "Unknown Device" : device.deviceName;
        this.address = device.deviceAddress;
        this.status = device.status;
        this.isConnecting = false;
    }

    // Getters and Setters
    public WifiP2pDevice getDevice() { return device; }
    public void setDevice(WifiP2pDevice device) { this.device = device; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isConnecting() { return isConnecting; }
    public void setConnecting(boolean connecting) { this.isConnecting = connecting; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    /**
     * Returns a human-readable status label based on WifiP2pDevice status.
     */
    public String getStatusLabel() {
        switch (status) {
            case WifiP2pDevice.CONNECTED: return "Connected";
            case WifiP2pDevice.INVITED: return "Invited";
            case WifiP2pDevice.FAILED: return "Failed";
            case WifiP2pDevice.AVAILABLE: return "Available";
            case WifiP2pDevice.UNAVAILABLE: return "Unavailable";
            default: return "Unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceItem that = (DeviceItem) o;
        return address != null && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
