package com.swiftshare.app.data.model;

import android.bluetooth.BluetoothDevice;

/**
 * Model representing a discovered Bluetooth device.
 * Wraps BluetoothDevice with additional UI metadata.
 */
public class DeviceItem {

    private BluetoothDevice device;
    private String name;
    private String address;
    private boolean isPaired;
    private int signalStrength; // RSSI value
    private boolean isConnecting;

    public DeviceItem(BluetoothDevice device, boolean isPaired) {
        this.device = device;
        this.address = device.getAddress();
        this.isPaired = isPaired;
        this.isConnecting = false;

        // Get device name with permission handling
        try {
            this.name = device.getName();
        } catch (SecurityException e) {
            this.name = null;
        }

        if (this.name == null || this.name.isEmpty()) {
            this.name = "Unknown Device";
        }
    }

    public DeviceItem(BluetoothDevice device, boolean isPaired, int rssi) {
        this(device, isPaired);
        this.signalStrength = rssi;
    }

    // Getters and Setters
    public BluetoothDevice getDevice() { return device; }
    public void setDevice(BluetoothDevice device) { this.device = device; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isPaired() { return isPaired; }
    public void setPaired(boolean paired) { isPaired = paired; }

    public int getSignalStrength() { return signalStrength; }
    public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }

    public boolean isConnecting() { return isConnecting; }
    public void setConnecting(boolean connecting) { isConnecting = connecting; }

    /**
     * Returns a human-readable signal strength label.
     */
    public String getSignalLabel() {
        if (signalStrength >= -50) return "Excellent";
        if (signalStrength >= -70) return "Strong";
        if (signalStrength >= -80) return "Good";
        if (signalStrength >= -90) return "Weak";
        return "Very Weak";
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
