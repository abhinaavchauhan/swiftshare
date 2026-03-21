package com.swiftshare.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.swiftshare.app.data.database.AppDatabase;
import com.swiftshare.app.data.preferences.AppPreferences;

/**
 * SwiftShare Application class.
 * Initializes core dependencies: notification channels, database, and preferences.
 */
public class SwiftShareApp extends Application {

    private static SwiftShareApp instance;
    private AppDatabase database;
    private AppPreferences preferences;

    public static final String CHANNEL_TRANSFER = "transfer_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize preferences
        preferences = new AppPreferences(this);

        // Initialize Room database
        database = AppDatabase.getInstance(this);

        // Create notification channels
        createNotificationChannels();
    }

    public static SwiftShareApp getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public AppPreferences getPreferences() {
        return preferences;
    }

    /**
     * Creates notification channels for Android O+ (API 26+).
     */
    private void createNotificationChannels() {
        NotificationChannel transferChannel = new NotificationChannel(
                CHANNEL_TRANSFER,
                getString(R.string.notification_channel_transfer),
                NotificationManager.IMPORTANCE_LOW
        );
        transferChannel.setDescription(getString(R.string.notification_channel_transfer_desc));
        transferChannel.setShowBadge(true);
        transferChannel.enableVibration(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(transferChannel);
        }
    }
}
