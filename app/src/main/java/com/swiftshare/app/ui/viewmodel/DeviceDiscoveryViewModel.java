package com.swiftshare.app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swiftshare.app.data.model.DeviceItem;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for device discovery.
 * Maintains the list of discovered Bluetooth devices and scanning state.
 */
public class DeviceDiscoveryViewModel extends AndroidViewModel {

    private final MutableLiveData<List<DeviceItem>> discoveredDevices;
    private final MutableLiveData<Boolean> isScanning;
    private final MutableLiveData<String> scanStatus;

    public DeviceDiscoveryViewModel(@NonNull Application application) {
        super(application);
        discoveredDevices = new MutableLiveData<>(new ArrayList<>());
        isScanning = new MutableLiveData<>(false);
        scanStatus = new MutableLiveData<>("Tap scan to find nearby devices");
    }

    public LiveData<List<DeviceItem>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }

    public LiveData<String> getScanStatus() {
        return scanStatus;
    }

    public void addDevice(DeviceItem device) {
        List<DeviceItem> current = discoveredDevices.getValue();
        if (current == null) current = new ArrayList<>();

        // Avoid duplicates
        if (!current.contains(device)) {
            current.add(device);
            discoveredDevices.setValue(new ArrayList<>(current));
        }
    }

    public void clearDevices() {
        discoveredDevices.setValue(new ArrayList<>());
    }

    public void setScanning(boolean scanning) {
        isScanning.setValue(scanning);
    }

    public void setScanStatus(String status) {
        scanStatus.setValue(status);
    }
}
