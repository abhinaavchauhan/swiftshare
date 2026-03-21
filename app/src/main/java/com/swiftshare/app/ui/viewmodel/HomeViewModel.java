package com.swiftshare.app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.swiftshare.app.data.model.TransferEntity;
import com.swiftshare.app.data.repository.TransferRepository;

import java.util.List;

/**
 * ViewModel for the Home screen.
 * Provides recent transfers and navigation events.
 */
public class HomeViewModel extends AndroidViewModel {

    private final TransferRepository repository;
    private final LiveData<List<TransferEntity>> recentTransfers;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new TransferRepository(application);
        recentTransfers = repository.getRecentTransfers();
    }

    public LiveData<List<TransferEntity>> getRecentTransfers() {
        return recentTransfers;
    }
}
