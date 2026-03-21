package com.swiftshare.app.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.swiftshare.app.data.dao.TransferDao;
import com.swiftshare.app.data.model.TransferEntity;

/**
 * Room database singleton for SwiftShare.
 * Manages the transfers table through TransferDao.
 */
@Database(entities = {TransferEntity.class}, version = 1, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "swiftshare_database";
    private static volatile AppDatabase instance;

    public abstract TransferDao transferDao();

    /**
     * Thread-safe singleton database instance.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
