package com.swiftshare.app.ui.discovery;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
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
import com.swiftshare.app.service.BluetoothConnectionService;
import com.swiftshare.app.service.FileTransferService;
import com.swiftshare.app.ui.adapter.DeviceAdapter;
import com.swiftshare.app.ui.viewmodel.DeviceDiscoveryViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for discovering nearby Bluetooth devices.
 * Shows scanning animation, device list, and handles device selection for file sending.
 */
public class DeviceDiscoveryFragment extends Fragment implements
        BluetoothConnectionService.DiscoveryCallback {

    private FragmentDeviceDiscoveryBinding binding;
    private DeviceDiscoveryViewModel viewModel;
    private DeviceAdapter adapter;

    private BluetoothConnectionService discoveryService;
    private FileTransferService transferService;
    private boolean isDiscoveryBound = false;
    private boolean isTransferBound = false;

    private String fileUriString;

    private final ServiceConnection discoveryConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothConnectionService.ConnectionBinder binder =
                    (BluetoothConnectionService.ConnectionBinder) service;
            discoveryService = binder.getService();
            discoveryService.setDiscoveryCallback(DeviceDiscoveryFragment.this);
            isDiscoveryBound = true;
            startDiscovery();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isDiscoveryBound = false;
        }
    };

    private final ServiceConnection transferConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder =
                    (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            isTransferBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isTransferBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceDiscoveryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DeviceDiscoveryViewModel.class);

        // Get file URI from arguments
        if (getArguments() != null) {
            fileUriString = getArguments().getString("file_uri");
        }

        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        observeData();
        bindServices();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupRecyclerView() {
        adapter = new DeviceAdapter();
        binding.recyclerDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerDevices.setAdapter(adapter);

        adapter.setOnDeviceClickListener(this::onDeviceSelected);
    }

    private void setupClickListeners() {
        binding.btnScanControl.setOnClickListener(v -> {
            if (discoveryService != null) {
                if (discoveryService.isDiscovering()) {
                    discoveryService.stopDiscovery();
                    binding.btnScanControl.setText(R.string.start_scanning);
                } else {
                    startDiscovery();
                    binding.btnScanControl.setText(R.string.stop_scanning);
                }
            }
        });

        binding.btnRetryScan.setOnClickListener(v -> startDiscovery());
    }

    private void observeData() {
        viewModel.getDiscoveredDevices().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null && !devices.isEmpty()) {
                adapter.submitList(new ArrayList<>(devices));
                binding.recyclerDevices.setVisibility(View.VISIBLE);
                binding.layoutEmptyDevices.setVisibility(View.GONE);
            }
        });

        viewModel.getIsScanning().observe(getViewLifecycleOwner(), scanning -> {
            binding.progressScanning.setVisibility(scanning ? View.VISIBLE : View.GONE);
            binding.textScanStatus.setText(scanning ?
                    getString(R.string.discovering) :
                    getString(R.string.tap_to_connect));
        });
    }

    /**
     * Starts Bluetooth device discovery.
     */
    private void startDiscovery() {
        if (discoveryService != null) {
            viewModel.clearDevices();
            viewModel.setScanning(true);
            discoveryService.startDiscovery();
            binding.btnScanControl.setText(R.string.stop_scanning);
            binding.layoutEmptyDevices.setVisibility(View.GONE);
        }
    }

    /**
     * Called when the user selects a device to connect to.
     */
    private void onDeviceSelected(DeviceItem device) {
        if (discoveryService != null) {
            discoveryService.stopDiscovery();
        }

        // Connect and send file
        if (transferService != null && fileUriString != null) {
            Uri fileUri = Uri.parse(fileUriString);
            transferService.connectAndSend(device.getDevice(), fileUri);

            // Navigate to transfer progress screen
            Bundle args = new Bundle();
            args.putString("file_uri", fileUriString);
            args.putString("device_name", device.getName());
            Navigation.findNavController(requireView()).navigate(
                    R.id.action_discovery_to_transfer, args);
        } else {
            Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindServices() {
        // Bind discovery service
        Intent discoveryIntent = new Intent(requireContext(), BluetoothConnectionService.class);
        requireContext().startService(discoveryIntent);
        requireContext().bindService(discoveryIntent, discoveryConnection, Context.BIND_AUTO_CREATE);

        // Bind transfer service
        Intent transferIntent = new Intent(requireContext(), FileTransferService.class);
        requireContext().startService(transferIntent);
        requireContext().bindService(transferIntent, transferConnection, Context.BIND_AUTO_CREATE);
    }

    // ── Discovery Callbacks ──────────────────────────

    @Override
    public void onDiscoveryStarted() {
        if (binding != null) {
            viewModel.setScanning(true);
        }
    }

    @Override
    public void onDeviceFound(DeviceItem device) {
        if (viewModel != null) {
            viewModel.addDevice(device);
        }
    }

    @Override
    public void onDiscoveryFinished() {
        if (binding != null) {
            viewModel.setScanning(false);
            binding.btnScanControl.setText(R.string.start_scanning);

            List<DeviceItem> devices = viewModel.getDiscoveredDevices().getValue();
            if (devices == null || devices.isEmpty()) {
                binding.layoutEmptyDevices.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onDiscoveryError(String error) {
        if (binding != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            viewModel.setScanning(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isDiscoveryBound) {
            if (discoveryService != null) {
                discoveryService.stopDiscovery();
            }
            requireContext().unbindService(discoveryConnection);
            isDiscoveryBound = false;
        }
        if (isTransferBound) {
            requireContext().unbindService(transferConnection);
            isTransferBound = false;
        }
        binding = null;
    }
}
