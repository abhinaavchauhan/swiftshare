package com.swiftshare.app.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import java.io.File;

/**
 * Manages user preferences using SharedPreferences.
 * Handles dark mode, device name, save location, and notification settings.
 */
public class AppPreferences {

    private static final String PREF_NAME = "swiftshare_prefs";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_SAVE_LOCATION = "save_location";
    private static final String KEY_AUTO_ACCEPT = "auto_accept";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private final SharedPreferences preferences;

    public AppPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── Dark Mode ────────────────────────────────────

    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        preferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    // ── Device Name ──────────────────────────────────

    public String getDeviceName() {
        String defaultName = Build.MODEL != null ? Build.MODEL : "SwiftShare Device";
        return preferences.getString(KEY_DEVICE_NAME, defaultName);
    }

    public void setDeviceName(String name) {
        preferences.edit().putString(KEY_DEVICE_NAME, name).apply();
    }

    // ── Save Location ────────────────────────────────

    public String getSaveLocation() {
        String defaultPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
                File.separator + "SwiftShare";
        return preferences.getString(KEY_SAVE_LOCATION, defaultPath);
    }

    public void setSaveLocation(String path) {
        preferences.edit().putString(KEY_SAVE_LOCATION, path).apply();
    }

    // ── Auto Accept ──────────────────────────────────

    public boolean isAutoAccept() {
        return preferences.getBoolean(KEY_AUTO_ACCEPT, false);
    }

    public void setAutoAccept(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_ACCEPT, enabled).apply();
    }

    // ── Notifications ────────────────────────────────

    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply();
    }

    // ── First Launch ─────────────────────────────────

    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchComplete() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    /**
     * Ensures the save directory exists, creating it if necessary.
     */
    public void ensureSaveDirectoryExists() {
        File saveDir = new File(getSaveLocation());
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
    }
}
