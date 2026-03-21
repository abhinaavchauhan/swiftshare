package com.swiftshare.app.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.swiftshare.app.data.model.TransferEntity;

import java.util.List;

/**
 * Data Access Object for transfer history operations.
 * Provides CRUD operations backed by Room SQLite database.
 */
@Dao
public interface TransferDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransferEntity transfer);

    @Update
    void update(TransferEntity transfer);

    @Delete
    void delete(TransferEntity transfer);

    @Query("SELECT * FROM transfers ORDER BY timestamp DESC")
    LiveData<List<TransferEntity>> getAllTransfers();

    @Query("SELECT * FROM transfers ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<TransferEntity>> getRecentTransfers(int limit);

    @Query("SELECT * FROM transfers WHERE direction = :direction ORDER BY timestamp DESC")
    LiveData<List<TransferEntity>> getTransfersByDirection(String direction);

    @Query("SELECT * FROM transfers WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<TransferEntity>> getTransfersByStatus(String status);

    @Query("SELECT * FROM transfers WHERE id = :id")
    LiveData<TransferEntity> getTransferById(long id);

    @Query("SELECT * FROM transfers WHERE id = :id")
    TransferEntity getTransferByIdSync(long id);

    @Query("UPDATE transfers SET status = :status, progress = :progress WHERE id = :id")
    void updateProgress(long id, String status, int progress);

    @Query("UPDATE transfers SET status = :status, endTime = :endTime WHERE id = :id")
    void updateStatus(long id, String status, long endTime);

    @Query("UPDATE transfers SET transferSpeed = :speed WHERE id = :id")
    void updateSpeed(long id, long speed);

    @Query("DELETE FROM transfers")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM transfers")
    int getTransferCount();

    @Query("SELECT COUNT(*) FROM transfers WHERE status = 'IN_PROGRESS'")
    int getActiveTransferCount();
}
