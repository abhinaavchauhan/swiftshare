package com.swiftshare.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.swiftshare.app.R;
import com.swiftshare.app.SwiftShareApp;
import com.swiftshare.app.databinding.ActivityMainBinding;
import com.swiftshare.app.utils.PermissionUtils;

/**
 * Main activity hosting the bottom navigation bar and NavHostFragment.
 * Handles initial permission requests and Bluetooth availability checks.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        applyTheme();

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        checkBluetoothSupport();
        requestPermissions();
    }

    /**
     * Applies the saved dark/light theme preference.
     */
    private void applyTheme() {
        boolean isDark = SwiftShareApp.getInstance().getPreferences().isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Sets up the Navigation Component with BottomNavigationView.
     */
    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

            // Hide bottom nav on certain screens
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                boolean showBottomNav = destId == R.id.navigation_home ||
                        destId == R.id.navigation_history ||
                        destId == R.id.navigation_settings;

                binding.bottomNavigation.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
            });
        }
    }

    /**
     * Verifies Bluetooth hardware is available on the device.
     */
    private void checkBluetoothSupport() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.error_no_bluetooth)
                    .setMessage(R.string.bluetooth_not_supported)
                    .setPositiveButton(R.string.ok, (d, w) -> finish())
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Requests all required runtime permissions.
     */
    private void requestPermissions() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            PermissionUtils.requestAllPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionUtils.REQUEST_ALL) {
            if (!PermissionUtils.hasBluetoothPermissions(this)) {
                Toast.makeText(this, R.string.bluetooth_permission_rationale, Toast.LENGTH_LONG).show();
            }
            if (!PermissionUtils.hasStoragePermissions(this)) {
                Toast.makeText(this, R.string.storage_permission_rationale, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Provides NavController access to fragments.
     */
    public NavController getNavController() {
        return navController;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
