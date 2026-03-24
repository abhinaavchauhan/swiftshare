package com.swiftshare.app.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.swiftshare.app.data.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared ViewModel for managing media selection state across different categories.
 */
public class SendViewModel extends ViewModel {

    private final MutableLiveData<List<MediaItem>> selectedItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> totalSize = new MutableLiveData<>(0L);

    public LiveData<List<MediaItem>> getSelectedItems() {
        return selectedItems;
    }

    public LiveData<Long> getTotalSize() {
        return totalSize;
    }

    public void toggleSelection(MediaItem item) {
        List<MediaItem> current = new ArrayList<>(selectedItems.getValue());
        if (current.contains(item)) {
            current.remove(item);
        } else {
            current.add(item);
        }
        selectedItems.setValue(current);
        calculateTotalSize(current);
    }

    public void clearSelection() {
        selectedItems.setValue(new ArrayList<>());
        totalSize.setValue(0L);
    }

    private void calculateTotalSize(List<MediaItem> items) {
        long size = 0;
        for (MediaItem item : items) {
            size += item.getSize();
        }
        totalSize.setValue(size);
    }

    public boolean isSelected(MediaItem item) {
        return selectedItems.getValue() != null && selectedItems.getValue().contains(item);
    }
}
