package com.swiftshare.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized permission management utility.
 * Handles runtime permission checks for Bluetooth, storage, and notifications.
 */
public final class PermissionUtils {

    public static final int REQUEST_BLUETOOTH = 1001;
    public static final int REQUEST_STORAGE = 1002;
    public static final int REQUEST_LOCATION = 1003;
    public static final int REQUEST_NEARBY = 1004;
    public static final int REQUEST_NOTIFICATION = 1005;
    public static final int REQUEST_ALL = 1006;

    private PermissionUtils() {}

    /**
     * Returns the list of Bluetooth permissions needed for the current API level.
     */
    public static String[] getBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            };
        } else {
            return new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    /**
     * Returns the list of storage permissions needed for the current API level.
     */
    public static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            return new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    /**
     * Returns all permissions needed by the app.
     */
    public static String[] getAllRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        for (String p : getBluetoothPermissions()) permissions.add(p);
        for (String p : getStoragePermissions()) permissions.add(p);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        return permissions.toArray(new String[0]);
    }

    /**
     * Checks if all Bluetooth permissions are granted.
     */
    public static boolean hasBluetoothPermissions(Context context) {
        for (String permission : getBluetoothPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if all storage permissions are granted.
     */
    public static boolean hasStoragePermissions(Context context) {
        for (String permission : getStoragePermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if notification permission is granted (API 33+).
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Not needed below API 33
    }

    /**
     * Checks if location permissions are granted.
     */
    public static boolean hasLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location permissions.
     */
    public static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION);
    }

    /**
     * Checks if nearby wifi permissions are granted (API 33+).
     */
    public static boolean hasNearbyPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Checks if all required permissions are granted.
     */
    public static boolean hasAllPermissions(Context context) {
        return hasBluetoothPermissions(context) &&
                hasStoragePermissions(context) &&
                hasNotificationPermission(context) &&
                hasLocationPermissions(context) &&
                hasNearbyPermission(context);
    }

    /**
     * Requests all missing permissions from the user.
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> missing = new ArrayList<>();

        for (String permission : getAllRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    missing.toArray(new String[0]), REQUEST_ALL);
        }
    }

    /**
     * Requests specific Bluetooth permissions.
     */
    public static void requestBluetoothPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                getBluetoothPermissions(), REQUEST_BLUETOOTH);
    }

    /**
     * Requests specific storage permissions.
     */
    public static void requestStoragePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                getStoragePermissions(), REQUEST_STORAGE);
    }

    /**
     * Checks whether the user should be shown a rationale for any permission.
     */
    public static boolean shouldShowRationale(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
}
