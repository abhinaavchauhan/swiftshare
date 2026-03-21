package com.swiftshare.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.swiftshare.app.data.dao.TransferDao;
import com.swiftshare.app.data.database.AppDatabase;
import com.swiftshare.app.data.model.TransferEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository mediating between TransferDao and ViewModels.
 * Ensures all database operations run on background threads.
 */
public class TransferRepository {

    private final TransferDao transferDao;
    private final ExecutorService executor;

    // LiveData caches
    private final LiveData<List<TransferEntity>> allTransfers;
    private final LiveData<List<TransferEntity>> recentTransfers;

    public TransferRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        transferDao = database.transferDao();
        executor = Executors.newFixedThreadPool(4);

        allTransfers = transferDao.getAllTransfers();
        recentTransfers = transferDao.getRecentTransfers(5);
    }

    // ── Queries ──────────────────────────────────────

    public LiveData<List<TransferEntity>> getAllTransfers() {
        return allTransfers;
    }

    public LiveData<List<TransferEntity>> getRecentTransfers() {
        return recentTransfers;
    }

    public LiveData<List<TransferEntity>> getTransfersByDirection(String direction) {
        return transferDao.getTransfersByDirection(direction);
    }

    public LiveData<List<TransferEntity>> getTransfersByStatus(String status) {
        return transferDao.getTransfersByStatus(status);
    }

    public LiveData<TransferEntity> getTransferById(long id) {
        return transferDao.getTransferById(id);
    }

    // ── Mutations ────────────────────────────────────

    /**
     * Inserts a new transfer record and returns the generated ID via callback.
     */
    public void insert(TransferEntity transfer, InsertCallback callback) {
        executor.execute(() -> {
            long id = transferDao.insert(transfer);
            if (callback != null) {
                callback.onInserted(id);
            }
        });
    }

    /**
     * Inserts a new transfer record (fire and forget).
     */
    public void insert(TransferEntity transfer) {
        insert(transfer, null);
    }

    public void update(TransferEntity transfer) {
        executor.execute(() -> transferDao.update(transfer));
    }

    public void delete(TransferEntity transfer) {
        executor.execute(() -> transferDao.delete(transfer));
    }

    public void deleteAll() {
        executor.execute(transferDao::deleteAll);
    }

    public void updateProgress(long id, String status, int progress) {
        executor.execute(() -> transferDao.updateProgress(id, status, progress));
    }

    public void updateStatus(long id, String status) {
        executor.execute(() -> transferDao.updateStatus(id, status, System.currentTimeMillis()));
    }

    public void updateSpeed(long id, long speed) {
        executor.execute(() -> transferDao.updateSpeed(id, speed));
    }

    /**
     * Callback interface for insert operations that need the generated ID.
     */
    public interface InsertCallback {
        void onInserted(long id);
    }
}
