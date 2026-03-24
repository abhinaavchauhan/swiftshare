package com.swiftshare.app.ui.discovery;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.swiftshare.app.R;
import com.swiftshare.app.data.model.DeviceItem;
import com.swiftshare.app.databinding.FragmentDeviceDiscoveryBinding;
import com.swiftshare.app.service.FileTransferService;
import com.swiftshare.app.ui.adapter.DeviceAdapter;
import com.swiftshare.app.ui.viewmodel.DeviceDiscoveryViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for discovering nearby Wi-Fi Direct devices.
 */
public class DeviceDiscoveryFragment extends Fragment implements
        FileTransferService.ServiceCallback {

    private FragmentDeviceDiscoveryBinding binding;
    private DeviceDiscoveryViewModel viewModel;
    private DeviceAdapter adapter;

    private FileTransferService transferService;
    private boolean isBound = false;
    private String fileUriString;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder = (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            transferService.setServiceCallback(DeviceDiscoveryFragment.this);
            isBound = true;
            startDiscovery();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DeviceDiscoveryViewModel.class);

        if (getArguments() != null) {
            fileUriString = getArguments().getString("file_uri");
        }

        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        observeData();
        bindService();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new DeviceAdapter();
        // Use GridLayoutManager with 3 columns to match the screenshot
        binding.recyclerDevices.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 3));
        binding.recyclerDevices.setAdapter(adapter);
        adapter.setOnDeviceClickListener(this::onDeviceSelected);
    }

    private void setupClickListeners() {
        binding.btnScanControl.setOnClickListener(v -> {
            if (transferService != null) {
                if (Boolean.TRUE.equals(viewModel.getIsScanning().getValue())) {
                    transferService.stopDiscovery();
                    viewModel.setScanning(false);
                } else {
                    startDiscovery();
                }
            }
        });

        binding.optionHotspot.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Setting up Hotspot...", Toast.LENGTH_SHORT).show();
            // Implement hotspot logic here if needed
        });

        binding.optionMyQr.setOnClickListener(v -> {
             // In a real app, show QR of my own P2P info
             Toast.makeText(requireContext(), "Showing My QRCode...", Toast.LENGTH_SHORT).show();
        });

        binding.optionScanQr.setOnClickListener(v -> {
             Toast.makeText(requireContext(), "Opening QR Scanner...", Toast.LENGTH_SHORT).show();
        });

        binding.btnOpenHistory.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_history);
        });
    }

    private void observeData() {
        viewModel.getDiscoveredDevices().observe(getViewLifecycleOwner(), devices -> {
            adapter.submitList(new ArrayList<>(devices));
            if (devices != null && !devices.isEmpty()) {
                binding.recyclerDevices.setVisibility(View.VISIBLE);
                binding.layoutEmptyDevices.setVisibility(View.GONE);
            } else {
                binding.recyclerDevices.setVisibility(View.GONE);
                if (!Boolean.TRUE.equals(viewModel.getIsScanning().getValue())) {
                    binding.layoutEmptyDevices.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getIsScanning().observe(getViewLifecycleOwner(), scanning -> {
            binding.progressScanning.setVisibility(scanning ? View.VISIBLE : View.GONE);
            binding.textScanStatus.setText(scanning ? "Searching for devices..." : "Select a device to send");
        });
    }

    private void startDiscovery() {
        if (transferService != null) {
            viewModel.clearDevices();
            viewModel.setScanning(true);
            transferService.startDiscovery();
        }
    }

    private void onDeviceSelected(DeviceItem device) {
        if (transferService != null && fileUriString != null) {
            transferService.connect(device.getDevice(), Uri.parse(fileUriString));
            
            // Navigate to transfer progress screen
            Bundle args = new Bundle();
            args.putString("file_uri", fileUriString);
            args.putString("device_name", device.getName());
            Navigation.findNavController(requireView()).navigate(R.id.action_discovery_to_transfer, args);
        }
    }

    private void bindService() {
        Intent intent = new Intent(requireContext(), FileTransferService.class);
        requireActivity().startService(intent); // Start as foreground service
        requireActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    // ── FileTransferService.ServiceCallback ──────────
    
    @Override
    public void onDiscoveryStarted() {
        if (viewModel != null) viewModel.setScanning(true);
    }

    @Override
    public void onDiscoveryFailed(int reason) {
        String error = "Discovery failed: " + reason;
        if (reason == 2) error = "Wi-Fi is off or P2P not supported";
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        if (viewModel != null) viewModel.setScanning(false);
    }

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> peers) {
        if (viewModel != null) {
            viewModel.clearDevices();
            for (WifiP2pDevice peer : peers) {
                viewModel.addDevice(new DeviceItem(peer));
            }
            viewModel.setScanning(false);
        }
    }

    @Override
    public void onDeviceConnected(String deviceName) {
        Toast.makeText(requireContext(), "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionLost() {
        Toast.makeText(requireContext(), "Connection Lost", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTransferStarted(String fileName, long fileSize, boolean isSending) {}

    @Override
    public void onTransferProgress(int progress, long speed, long eta, long transferred, long total) {}

    @Override
    public void onTransferComplete(String fileName, long fileSize, boolean wasSending) {}

    @Override
    public void onTransferFailed(String error) {}

    @Override
    public void onTransferCancelled() {}

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
