package com.swiftshare.app.ui.settings;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.swiftshare.app.BuildConfig;
import com.swiftshare.app.R;
import com.swiftshare.app.SwiftShareApp;
import com.swiftshare.app.data.preferences.AppPreferences;
import com.swiftshare.app.databinding.FragmentSettingsBinding;

/**
 * Settings fragment for app configuration.
 * Dark mode, device name, save location, and auto-accept toggles.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AppPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = SwiftShareApp.getInstance().getPreferences();

        loadSettings();
        setupListeners();
    }

    /**
     * Loads current settings into the UI.
     */
    private void loadSettings() {
        binding.textDeviceName.setText(preferences.getDeviceName());
        binding.textSaveLocation.setText(preferences.getSaveLocation());
        binding.switchDarkMode.setChecked(preferences.isDarkMode());
        binding.switchAutoAccept.setChecked(preferences.isAutoAccept());
        binding.textVersion.setText(getString(R.string.version, BuildConfig.VERSION_NAME));
    }

    /**
     * Sets up all click and toggle listeners.
     */
    private void setupListeners() {
        // Dark Mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Auto-Accept toggle
        binding.switchAutoAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.setAutoAccept(isChecked);
        });

        // Device Name edit
        binding.cardDeviceName.setOnClickListener(v -> showEditDeviceNameDialog());

        // Save Location
        binding.cardSaveLocation.setOnClickListener(v -> {
            // In a full implementation, this would open a directory picker
            // For now, show the current path in a dialog
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.save_location)
                    .setMessage(preferences.getSaveLocation())
                    .setPositiveButton(R.string.ok, null)
                    .show();
        });
    }

    /**
     * Shows a dialog to edit the device name.
     */
    private void showEditDeviceNameDialog() {
        TextInputLayout inputLayout = new TextInputLayout(requireContext());
        inputLayout.setPadding(56, 16, 56, 0);
        inputLayout.setHint(getString(R.string.device_name));

        TextInputEditText editText = new TextInputEditText(inputLayout.getContext());
        editText.setText(preferences.getDeviceName());
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setSelectAllOnFocus(true);
        inputLayout.addView(editText);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.device_name)
                .setView(inputLayout)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String name = editText.getText() != null ?
                            editText.getText().toString().trim() : "";
                    if (!name.isEmpty()) {
                        preferences.setDeviceName(name);
                        binding.textDeviceName.setText(name);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
