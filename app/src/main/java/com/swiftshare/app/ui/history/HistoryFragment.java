package com.swiftshare.app.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.swiftshare.app.R;
import com.swiftshare.app.databinding.FragmentHistoryBinding;
import com.swiftshare.app.ui.adapter.TransferHistoryAdapter;
import com.swiftshare.app.ui.viewmodel.HistoryViewModel;

/**
 * History fragment displaying all file transfer records.
 * Supports filtering by All/Sent/Received using filter chips.
 */
public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private TransferHistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        setupRecyclerView();
        setupFilterChips();
        setupClickListeners();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new TransferHistoryAdapter();
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
    }

    /**
     * Sets up filter chip click listeners for All/Sent/Received filtering.
     */
    private void setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_sent)) {
                viewModel.setFilter("SENT");
            } else if (checkedIds.contains(R.id.chip_received)) {
                viewModel.setFilter("RECEIVED");
            } else {
                viewModel.setFilter("ALL");
            }
        });
    }

    private void setupClickListeners() {
        binding.btnClearHistory.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.clear_history)
                    .setMessage(R.string.confirm_clear_history)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        viewModel.clearHistory();
                        Snackbar.make(binding.getRoot(),
                                R.string.history_cleared,
                                Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    /**
     * Observes the filtered transfer list from the ViewModel.
     */
    private void observeData() {
        viewModel.getFilteredTransfers().observe(getViewLifecycleOwner(), transfers -> {
            if (transfers != null && !transfers.isEmpty()) {
                adapter.submitList(transfers);
                binding.recyclerHistory.setVisibility(View.VISIBLE);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                binding.recyclerHistory.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
