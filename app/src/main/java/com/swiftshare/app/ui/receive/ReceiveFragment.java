package com.swiftshare.app.ui.receive;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
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

import java.util.List;

/**
 * Receiver mode fragment for Wi-Fi Direct transfers.
 * Shows pulse animation and waits for incoming P2P connections.
 */
public class ReceiveFragment extends Fragment implements FileTransferService.ServiceCallback {

    private FragmentReceiveBinding binding;
    private FileTransferService transferService;
    private boolean isBound = false;
    private AnimatorSet pulseAnimator;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileTransferService.TransferBinder binder = (FileTransferService.TransferBinder) service;
            transferService = binder.getService();
            transferService.setServiceCallback(ReceiveFragment.this);
            isBound = true;
            
            // Start listening for incoming Wi-Fi Direct connections
            transferService.startReceiverMode();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void setupClickListeners() {
        binding.btnDiscoverable.setOnClickListener(v -> {
            if (!PermissionUtils.hasLocationPermissions(requireContext())) {
                PermissionUtils.requestLocationPermissions(requireActivity());
                return;
            }
            Toast.makeText(requireContext(), "Wi-Fi Direct Receiver Mode Active", Toast.LENGTH_SHORT).show();
        });
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) return;

        ObjectAnimator scaleX1 = ObjectAnimator.ofFloat(binding.pulseRing1, "scaleX", 0.5f, 1.4f);
        ObjectAnimator scaleY1 = ObjectAnimator.ofFloat(binding.pulseRing1, "scaleY", 0.5f, 1.4f);
        ObjectAnimator alpha1 = ObjectAnimator.ofFloat(binding.pulseRing1, "alpha", 0.3f, 0f);

        ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(binding.pulseRing2, "scaleX", 0.4f, 1.2f);
        ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(binding.pulseRing2, "scaleY", 0.4f, 1.2f);
        ObjectAnimator alpha2 = ObjectAnimator.ofFloat(binding.pulseRing2, "alpha", 0.4f, 0f);

        ObjectAnimator scaleX3 = ObjectAnimator.ofFloat(binding.pulseRing3, "scaleX", 0.3f, 1.1f);
        ObjectAnimator scaleY3 = ObjectAnimator.ofFloat(binding.pulseRing3, "scaleY", 0.3f, 1.1f);
        ObjectAnimator alpha3 = ObjectAnimator.ofFloat(binding.pulseRing3, "alpha", 0.6f, 0.1f);

        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX1, scaleY1, alpha1, scaleX2, scaleY2, alpha2, scaleX3, scaleY3, alpha3);
        pulseAnimator.setDuration(2500);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (pulseAnimator != null) pulseAnimator.start();
            }
        });
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
    }

    private void bindTransferService() {
        Intent intent = new Intent(requireContext(), FileTransferService.class);
        requireContext().startService(intent);
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
        if (binding != null) {
            binding.textStatus.setText("Connected to " + deviceName);
            binding.textDescription.setText("Waiting for file...");
            stopPulseAnimation();
        }
    }

    @Override
    public void onConnectionFailed(String error) {
        if (binding != null) binding.textStatus.setText("Connection failed");
    }

    @Override
    public void onConnectionLost() {
        if (binding != null) {
            binding.textStatus.setText("Disconnected");
            startPulseAnimation();
        }
    }

    @Override
    public void onTransferStarted(String fileName, long fileSize, boolean isSending) {
        if (binding != null && isAdded()) {
            Bundle args = new Bundle();
            args.putString("file_name", fileName);
            args.putLong("file_size", fileSize);
            args.putBoolean("is_sending", isSending);
            Navigation.findNavController(binding.getRoot()).navigate(R.id.action_receive_to_transfer, args);
        }
    }

    @Override
    public void onTransferProgress(int progress, long speed, long eta, long transferred, long total) {}

    @Override
    public void onTransferComplete(String fileName, long fileSize, boolean wasSending) {
        if (binding != null) {
            binding.textStatus.setText("Transfer complete!");
            binding.textDescription.setText("Received: " + fileName);
        }
    }

    @Override
    public void onTransferFailed(String error) {
        if (binding != null) {
            binding.textStatus.setText("Transfer failed");
            binding.textDescription.setText(error);
        }
    }

    @Override
    public void onTransferCancelled() {
        if (binding != null) binding.textStatus.setText("Transfer cancelled");
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
