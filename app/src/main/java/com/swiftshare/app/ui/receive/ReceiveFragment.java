package com.swiftshare.app.ui.receive;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.swiftshare.app.R;
import com.swiftshare.app.databinding.FragmentReceiveBinding;
import com.swiftshare.app.service.FileTransferService;
import com.swiftshare.app.utils.PermissionUtils;

/**
 * Receiver mode fragment.
 * Enables Bluetooth discoverability, shows pulse animation,
 * and listens for incoming file transfer connections.
 */
public class ReceiveFragment extends Fragment {

    private FragmentReceiveBinding binding;
    private FileTransferService transferService;
    private boolean isBound = false;
    private AnimatorSet pulseAnimator;

    private static final int DISCOVERABLE_DURATION = 300; // 5 minutes

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder =
                    (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            isBound = true;

            // Set up service callback
            transferService.setServiceCallback(new FileTransferService.ServiceCallback() {
                @Override
                public void onConnectionStateChanged(int state) {}

                @Override
                public void onDeviceConnected(BluetoothDevice device) {
                    if (binding != null) {
                        String deviceName;
                        try {
                            deviceName = device.getName() != null ? device.getName() : "Unknown Device";
                        } catch (SecurityException e) {
                            deviceName = "Unknown Device";
                        }
                        binding.textStatus.setText(getString(R.string.connected_to, deviceName));
                        binding.textDescription.setText("Waiting for file transfer...");
                        stopPulseAnimation();
                    }
                }

                @Override
                public void onConnectionFailed(String error) {
                    if (binding != null) {
                        binding.textStatus.setText(R.string.error_connection_failed);
                    }
                }

                @Override
                public void onConnectionLost() {
                    if (binding != null) {
                        binding.textStatus.setText(R.string.disconnected);
                        startPulseAnimation();
                    }
                }

                @Override
                public void onTransferStarted(String fileName, long fileSize, boolean isSending) {
                    if (binding != null) {
                        binding.textStatus.setText(getString(R.string.receiving) + " " + fileName);
                    }
                }

                @Override
                public void onTransferProgress(int progress, long speed, long eta,
                                               long bytesTransferred, long totalBytes) {
                    if (binding != null) {
                        binding.textDescription.setText(progress + "% complete");
                    }
                }

                @Override
                public void onTransferComplete(String fileName, long fileSize, boolean wasSending) {
                    if (binding != null) {
                        binding.textStatus.setText(R.string.transfer_complete);
                        binding.textDescription.setText("Received: " + fileName);
                        Toast.makeText(requireContext(),
                                getString(R.string.notification_complete, fileName),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onTransferFailed(String error) {
                    if (binding != null) {
                        binding.textStatus.setText(R.string.transfer_failed);
                        binding.textDescription.setText(error);
                    }
                }

                @Override
                public void onTransferCancelled() {
                    if (binding != null) {
                        binding.textStatus.setText(R.string.transfer_cancelled);
                    }
                }
            });

            // Start listening for incoming connections
            transferService.startListening();
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
        binding = FragmentReceiveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupToolbar();
        setupClickListeners();
        startPulseAnimation();
        bindTransferService();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(v).navigateUp());
    }

    private void setupClickListeners() {
        binding.btnDiscoverable.setOnClickListener(v -> {
            if (!PermissionUtils.hasBluetoothPermissions(requireContext())) {
                PermissionUtils.requestBluetoothPermissions(requireActivity());
                return;
            }
            makeDiscoverable();
        });
    }

    /**
     * Requests Bluetooth discoverability from the system.
     */
    private void makeDiscoverable() {
        try {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                    DISCOVERABLE_DURATION);
            startActivity(discoverableIntent);

            binding.textStatus.setText(getString(R.string.discoverable_for, DISCOVERABLE_DURATION));
            binding.textDescription.setText(R.string.waiting_for_connection);
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.bluetooth_permission_rationale,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts the radar-like pulse animation on the scan rings.
     */
    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) return;

        // Outer ring pulse
        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(binding.pulseRing1, "scaleX", 0.5f, 1.2f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(binding.pulseRing1, "scaleY", 0.5f, 1.2f);
        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(binding.pulseRing1, "alpha", 0.3f, 0f);

        // Inner ring pulse
        ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(binding.pulseRing2, "scaleX", 0.4f, 1.1f);
        ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(binding.pulseRing2, "scaleY", 0.4f, 1.1f);
        ObjectAnimator alpha2 = ObjectAnimator.ofFloat(binding.pulseRing2, "alpha", 0.4f, 0f);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX1, scaleY1, alpha1, scaleX2, scaleY2, alpha2);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (pulseAnimator != null) {
                    pulseAnimator.start(); // Loop
                }
            }
        });
        pulseAnimator.setStartDelay(300);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
    }

    /**
     * Binds to the FileTransferService.
     */
    private void bindTransferService() {
        Intent intent = new Intent(requireContext(), FileTransferService.class);
        requireContext().startService(intent);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPulseAnimation();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
        }
        binding = null;
    }
}
