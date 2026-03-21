package com.swiftshare.app.ui.send;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.swiftshare.app.R;
import com.swiftshare.app.databinding.FragmentSendBinding;
import com.swiftshare.app.utils.FileUtils;
import com.swiftshare.app.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for selecting files to send.
 * Integrates with the system file picker and shows selected files
 * before proceeding to device discovery.
 */
public class SendFragment extends Fragment {

    private FragmentSendBinding binding;
    private final List<Uri> selectedFiles = new ArrayList<>();
    private long totalSize = 0;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            handleFileSelection(result.getData());
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        setupClickListeners();
        updateUI();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupClickListeners() {
        // Select files button
        binding.btnSelectFiles.setOnClickListener(v -> openFilePicker());

        // Proceed to device discovery
        binding.btnProceed.setOnClickListener(v -> {
            if (!selectedFiles.isEmpty()) {
                // Store selected file URI in arguments
                Bundle args = new Bundle();
                args.putString("file_uri", selectedFiles.get(0).toString());
                Navigation.findNavController(v).navigate(
                        R.id.action_send_to_device_discovery, args);
            }
        });
    }

    /**
     * Opens the system file picker for multi-file selection.
     */
    private void openFilePicker() {
        if (!PermissionUtils.hasStoragePermissions(requireContext())) {
            PermissionUtils.requestStoragePermissions(requireActivity());
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    /**
     * Processes the selected files from the file picker result.
     */
    private void handleFileSelection(Intent data) {
        selectedFiles.clear();
        totalSize = 0;

        if (data.getClipData() != null) {
            // Multiple files selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                selectedFiles.add(uri);
                totalSize += FileUtils.getFileSize(requireContext(), uri);
            }
        } else if (data.getData() != null) {
            // Single file selected
            Uri uri = data.getData();
            selectedFiles.add(uri);
            totalSize = FileUtils.getFileSize(requireContext(), uri);
        }

        updateUI();
    }

    /**
     * Updates the UI based on selected files.
     */
    private void updateUI() {
        if (selectedFiles.isEmpty()) {
            binding.textFilesCount.setText(R.string.no_files_selected);
            binding.textTotalSize.setVisibility(View.GONE);
            binding.btnProceed.setEnabled(false);
        } else {
            binding.textFilesCount.setText(
                    getString(R.string.files_selected, selectedFiles.size()));
            binding.textTotalSize.setText(
                    getString(R.string.total_size, FileUtils.formatFileSize(totalSize)));
            binding.textTotalSize.setVisibility(View.VISIBLE);
            binding.btnProceed.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
