package com.swiftshare.app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.swiftshare.app.data.model.TransferEntity;
import com.swiftshare.app.data.repository.TransferRepository;

import java.util.List;

/**
 * ViewModel for the History screen.
 * Supports filtering transfers by direction (All/Sent/Received).
 */
public class HistoryViewModel extends AndroidViewModel {

    private final TransferRepository repository;
    private final MediatorLiveData<List<TransferEntity>> filteredTransfers;
    private final MutableLiveData<String> currentFilter;

    private LiveData<List<TransferEntity>> currentSource;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new TransferRepository(application);
        filteredTransfers = new MediatorLiveData<>();
        currentFilter = new MutableLiveData<>("ALL");

        // Initialize with all transfers
        setFilter("ALL");
    }

    /**
     * Returns the filtered transfer list as LiveData.
     */
    public LiveData<List<TransferEntity>> getFilteredTransfers() {
        return filteredTransfers;
    }

    public LiveData<String> getCurrentFilter() {
        return currentFilter;
    }

    /**
     * Sets the filter and updates the data source accordingly.
     */
    public void setFilter(String filter) {
        currentFilter.setValue(filter);

        // Remove old source
        if (currentSource != null) {
            filteredTransfers.removeSource(currentSource);
        }

        // Add new source based on filter
        switch (filter) {
            case "SENT":
                currentSource = repository.getTransfersByDirection("SENT");
                break;
            case "RECEIVED":
                currentSource = repository.getTransfersByDirection("RECEIVED");
                break;
            default:
                currentSource = repository.getAllTransfers();
                break;
        }

        filteredTransfers.addSource(currentSource, filteredTransfers::setValue);
    }

    /**
     * Clears all transfer history.
     */
    public void clearHistory() {
        repository.deleteAll();
    }
}
