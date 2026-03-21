package com.swiftshare.app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.swiftshare.app.R;
import com.swiftshare.app.databinding.FragmentHomeBinding;
import com.swiftshare.app.ui.adapter.TransferHistoryAdapter;
import com.swiftshare.app.ui.viewmodel.HomeViewModel;
import com.swiftshare.app.utils.PermissionUtils;

/**
 * Home fragment displaying the main Send/Receive cards
 * and a list of recent file transfers.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransferHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeData();
        animateEntrance();
    }

    /**
     * Configures the recent transfers RecyclerView.
     */
    private void setupRecyclerView() {
        adapter = new TransferHistoryAdapter();
        binding.recyclerRecentTransfers.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerRecentTransfers.setAdapter(adapter);
        binding.recyclerRecentTransfers.setNestedScrollingEnabled(false);
    }

    /**
     * Sets up click listeners for Send, Receive, and View All buttons.
     */
    private void setupClickListeners() {
        // Send Files card
        binding.cardSend.setOnClickListener(v -> {
            if (PermissionUtils.hasBluetoothPermissions(requireContext())) {
                Navigation.findNavController(v).navigate(R.id.action_home_to_send);
            } else {
                PermissionUtils.requestBluetoothPermissions(requireActivity());
            }
        });

        // Receive Files card
        binding.cardReceive.setOnClickListener(v -> {
            if (PermissionUtils.hasBluetoothPermissions(requireContext())) {
                Navigation.findNavController(v).navigate(R.id.action_home_to_receive);
            } else {
                PermissionUtils.requestBluetoothPermissions(requireActivity());
            }
        });

        // View All history
        binding.btnViewAll.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.navigation_history);
        });
    }

    /**
     * Observes LiveData from the ViewModel.
     */
    private void observeData() {
        viewModel.getRecentTransfers().observe(getViewLifecycleOwner(), transfers -> {
            if (transfers != null && !transfers.isEmpty()) {
                adapter.submitList(transfers);
                binding.recyclerRecentTransfers.setVisibility(View.VISIBLE);
                binding.layoutEmptyState.setVisibility(View.GONE);
            } else {
                binding.recyclerRecentTransfers.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Applies staggered entrance animations to the UI elements.
     */
    private void animateEntrance() {
        binding.cardSend.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in));
        binding.cardReceive.postDelayed(() -> {
            if (binding != null) {
                binding.cardReceive.startAnimation(
                        AnimationUtils.loadAnimation(requireContext(), R.anim.fade_scale_in));
            }
        }, 150);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
