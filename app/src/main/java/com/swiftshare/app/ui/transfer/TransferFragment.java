package com.swiftshare.app.ui.transfer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.swiftshare.app.R;
import com.swiftshare.app.databinding.FragmentTransferBinding;
import com.swiftshare.app.service.FileTransferService;
import com.swiftshare.app.ui.viewmodel.TransferViewModel;
import com.swiftshare.app.utils.FileUtils;

import java.io.File;
import java.util.List;

/**
 * Transfer progress fragment for Wi-Fi Direct file transfers.
 */
public class TransferFragment extends Fragment implements FileTransferService.ServiceCallback {

    private FragmentTransferBinding binding;
    private TransferViewModel viewModel;
    private FileTransferService transferService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder = (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            transferService.setServiceCallback(TransferFragment.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TransferViewModel.class);

        if (getArguments() != null) {
            String deviceName = getArguments().getString("device_name", "Device");
            viewModel.setStatus("Connecting to " + deviceName + "...");
        }

        setupClickListeners();
        observeData();
        bindService();
    }

    private void setupClickListeners() {
        binding.btnCancel.setOnClickListener(v -> {
            if (transferService != null) transferService.cancelTransfer();
            Navigation.findNavController(v).navigateUp();
        });

        binding.btnDone.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.btnViewFile.setOnClickListener(v -> openSavedFileLocation());
    }

    private void openSavedFileLocation() {
        try {
            File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS);
            File swiftShareDir = new File(downloadsDir, "SwiftShare");
            
            Uri uri = Uri.parse(swiftShareDir.getAbsolutePath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "resource/folder");
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
            } catch (Exception ex) {
                android.widget.Toast.makeText(requireContext(), "Could not open file manager", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void observeData() {
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            binding.progressTransfer.setProgress(progress);
            binding.textProgressPercent.setText(progress + "%");
        });

        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> 
                binding.textTransferSpeed.setText(FileUtils.formatSpeed(speed)));

        viewModel.getEta().observe(getViewLifecycleOwner(), eta -> 
                binding.textEta.setText(FileUtils.formatETA(eta)));

        viewModel.getFileName().observe(getViewLifecycleOwner(), name -> 
                binding.textFileName.setText(name));

        viewModel.getFileSize().observe(getViewLifecycleOwner(), size -> 
                binding.textFileSize.setText(FileUtils.formatFileSize(size)));

        viewModel.getStatus().observe(getViewLifecycleOwner(), status -> 
                binding.textTransferStatus.setText(status));

        viewModel.getIsComplete().observe(getViewLifecycleOwner(), complete -> {
            if (complete != null && complete) showSuccess();
        });

        viewModel.getIsFailed().observe(getViewLifecycleOwner(), failed -> {
            if (failed != null && failed) binding.btnCancel.setText("Retry");
        });
    }

    private void showSuccess() {
        binding.cardActiveTransfer.setVisibility(View.GONE);
        binding.btnCancel.setVisibility(View.GONE);
        binding.layoutSuccessContainer.setVisibility(View.VISIBLE);
        binding.layoutCompletionButtons.setVisibility(View.VISIBLE);
        
        binding.layoutSuccessContainer.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in));
        binding.layoutCompletionButtons.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in));
    }

    private void bindService() {
        Intent intent = new Intent(requireContext(), FileTransferService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    // ── FileTransferService.ServiceCallback Implementation ──

    @Override
    public void onDiscoveryStarted() {}

    @Override
    public void onDiscoveryFailed(int reason) {}

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> peers) {}

    @Override
    public void onDeviceConnected(String deviceName) {
        if (viewModel != null) viewModel.setStatus("Connected to " + deviceName);
    }

    @Override
    public void onConnectionFailed(String error) {
        if (viewModel != null) {
            viewModel.setStatus("Connection failed: " + error);
            viewModel.setFailed(true);
        }
    }

    @Override
    public void onConnectionLost() {
        if (viewModel != null) {
            viewModel.setStatus("Connection lost");
            viewModel.setFailed(true);
        }
    }

    @Override
    public void onTransferStarted(String fileName, long fileSize, boolean isSending) {
        if (viewModel != null) {
            viewModel.setFileName(fileName);
            viewModel.setFileSize(fileSize);
            viewModel.setStatus(isSending ? "Sending..." : "Receiving...");
        }
    }

    @Override
    public void onTransferProgress(int progress, long speed, long eta, long bytesTransferred, long totalBytes) {
        if (viewModel != null) {
            viewModel.updateProgress(progress, speed, eta, bytesTransferred);
        }
    }

    @Override
    public void onTransferComplete(String fileName, long fileSize, boolean wasSending) {
        if (viewModel != null) {
            viewModel.setComplete(true);
            viewModel.setStatus("Transfer complete!");
        }
    }

    @Override
    public void onTransferFailed(String error) {
        if (viewModel != null) {
            viewModel.setFailed(true);
            viewModel.setStatus("Transfer failed: " + error);
        }
    }

    @Override
    public void onTransferCancelled() {
        if (viewModel != null) viewModel.setStatus("Transfer cancelled");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
        binding = null;
    }
}
