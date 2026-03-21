package com.swiftshare.app.ui.transfer;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

/**
 * Transfer progress fragment.
 * Shows circular progress, speed, ETA, and file information.
 * Binds to FileTransferService for real-time updates.
 */
public class TransferFragment extends Fragment {

    private FragmentTransferBinding binding;
    private TransferViewModel viewModel;
    private FileTransferService transferService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder =
                    (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            isBound = true;

            transferService.setServiceCallback(new FileTransferService.ServiceCallback() {
                @Override
                public void onConnectionStateChanged(int state) {}

                @Override
                public void onDeviceConnected(BluetoothDevice device) {
                    if (viewModel != null) {
                        String name1;
                        try {
                            name1 = device.getName() != null ? device.getName() : "Unknown Device";
                        } catch (SecurityException e) {
                            name1 = "Unknown Device";
                        }
                        viewModel.setStatus("Connected to " + name1);
                    }
                }

                @Override
                public void onConnectionFailed(String error) {
                    if (viewModel != null) {
                        viewModel.setStatus("Connection failed");
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
                public void onTransferProgress(int progress, long speed, long eta,
                                               long bytesTransferred, long totalBytes) {
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
                    if (viewModel != null) {
                        viewModel.setStatus("Transfer cancelled");
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransferViewModel.class);

        // Get arguments
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
            if (transferService != null) {
                transferService.cancelTransfer();
            }
            Navigation.findNavController(v).navigateUp();
        });
    }

    /**
     * Observes ViewModel LiveData and updates the UI.
     */
    private void observeData() {
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            binding.progressTransfer.setProgress(progress);
            binding.textProgressPercent.setText(progress + "%");
        });

        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> {
            String speedText = FileUtils.formatSpeed(speed);
            binding.textTransferSpeed.setText(speedText);
            binding.textSpeed.setText(speedText);
        });

        viewModel.getEta().observe(getViewLifecycleOwner(), eta ->
                binding.textEta.setText(FileUtils.formatETA(eta)));

        viewModel.getFileName().observe(getViewLifecycleOwner(), name ->
                binding.textFileName.setText(name));

        viewModel.getFileSize().observe(getViewLifecycleOwner(), size ->
                binding.textFileSize.setText(FileUtils.formatFileSize(size)));

        viewModel.getStatus().observe(getViewLifecycleOwner(), status ->
                binding.textTransferStatus.setText(status));

        viewModel.getIsComplete().observe(getViewLifecycleOwner(), complete -> {
            if (complete != null && complete) {
                showSuccess();
            }
        });

        viewModel.getIsFailed().observe(getViewLifecycleOwner(), failed -> {
            if (failed != null && failed) {
                binding.btnCancel.setText(R.string.retry_transfer);
            }
        });
    }

    /**
     * Displays the success overlay with animation.
     */
    private void showSuccess() {
        binding.progressContainer.setVisibility(View.GONE);
        binding.cardTransferInfo.setVisibility(View.GONE);
        binding.layoutSuccess.setVisibility(View.VISIBLE);
        binding.layoutSuccess.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in));
        binding.btnCancel.setText(R.string.ok);
        binding.btnCancel.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void bindService() {
        Intent intent = new Intent(requireContext(), FileTransferService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
