package com.swiftshare.app.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel for the transfer progress screen.
 * Tracks real-time transfer metrics: progress, speed, ETA, file info.
 */
public class TransferViewModel extends AndroidViewModel {

    private final MutableLiveData<Integer> progress;
    private final MutableLiveData<Long> speed;
    private final MutableLiveData<Long> eta;
    private final MutableLiveData<String> fileName;
    private final MutableLiveData<Long> fileSize;
    private final MutableLiveData<String> status;
    private final MutableLiveData<Boolean> isComplete;
    private final MutableLiveData<Boolean> isFailed;
    private final MutableLiveData<Long> bytesTransferred;

    public TransferViewModel(@NonNull Application application) {
        super(application);
        progress = new MutableLiveData<>(0);
        speed = new MutableLiveData<>(0L);
        eta = new MutableLiveData<>(-1L);
        fileName = new MutableLiveData<>("");
        fileSize = new MutableLiveData<>(0L);
        status = new MutableLiveData<>("Preparing...");
        isComplete = new MutableLiveData<>(false);
        isFailed = new MutableLiveData<>(false);
        bytesTransferred = new MutableLiveData<>(0L);
    }

    // Getters
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<Long> getSpeed() { return speed; }
    public LiveData<Long> getEta() { return eta; }
    public LiveData<String> getFileName() { return fileName; }
    public LiveData<Long> getFileSize() { return fileSize; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getIsComplete() { return isComplete; }
    public LiveData<Boolean> getIsFailed() { return isFailed; }
    public LiveData<Long> getBytesTransferred() { return bytesTransferred; }

    // Setters
    public void updateProgress(int progressVal, long speedVal, long etaVal,
                               long bytesTransferredVal) {
        progress.setValue(progressVal);
        speed.setValue(speedVal);
        eta.setValue(etaVal);
        bytesTransferred.setValue(bytesTransferredVal);
    }

    public void setFileName(String name) { fileName.setValue(name); }
    public void setFileSize(long size) { fileSize.setValue(size); }
    public void setStatus(String statusText) { status.setValue(statusText); }
    public void setComplete(boolean complete) { isComplete.setValue(complete); }
    public void setFailed(boolean failed) { isFailed.setValue(failed); }
}
