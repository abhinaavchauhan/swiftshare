package com.swiftshare.app.ui.send;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.tabs.TabLayoutMediator;
import com.swiftshare.app.R;
import com.swiftshare.app.data.model.MediaItem;
import com.swiftshare.app.databinding.FragmentSendBinding;
import com.swiftshare.app.ui.adapter.SendPagerAdapter;
import com.swiftshare.app.ui.viewmodel.SendViewModel;
import com.swiftshare.app.utils.FileUtils;
import com.swiftshare.app.utils.PermissionUtils;

import java.util.List;

/**
 * Fragment for selecting multiple media items (Photos, Videos, etc.) to send.
 * Uses ViewPager2 with category-specific pickers.
 */
public class SendFragment extends Fragment {

    private FragmentSendBinding binding;
    private SendViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSendBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SendViewModel.class);

        checkPermissions();
        setupToolbar();
        setupViewPager();
        observeSelection();
    }

    private void checkPermissions() {
        if (!PermissionUtils.hasAllPermissions(requireContext())) {
            PermissionUtils.requestAllPermissions(requireActivity());
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_selection) {
                viewModel.clearSelection();
                return true;
            }
            return false;
        });
    }

    private void setupViewPager() {
        SendPagerAdapter adapter = new SendPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("App"); break;
                case 1: tab.setText("Files"); break;
                case 2: tab.setText("Music"); break;
                case 3: tab.setText("Photo"); break;
                case 4: tab.setText("Video"); break;
            }
        }).attach();
    }

    private void observeSelection() {
        viewModel.getSelectedItems().observe(getViewLifecycleOwner(), items -> {
            updateSelectionToolbar(items);
        });

        viewModel.getTotalSize().observe(getViewLifecycleOwner(), size -> {
            binding.textTotalSize.setText(FileUtils.formatFileSize(size));
        });

        binding.btnSend.setOnClickListener(v -> {
            List<MediaItem> selected = viewModel.getSelectedItems().getValue();
            if (selected != null && !selected.isEmpty()) {
                // For simplicity, we pass the first file URI to the discovery fragment
                // In a full implementation, we would register the whole batch for transfer
                Bundle args = new Bundle();
                args.putString("file_uri", selected.get(0).getUri().toString());
                Navigation.findNavController(v).navigate(R.id.action_send_to_device_discovery, args);
            }
        });
    }

    private void updateSelectionToolbar(List<MediaItem> items) {
        if (items.isEmpty()) {
            binding.cardSelectionToolbar.setVisibility(View.GONE);
        } else {
            binding.cardSelectionToolbar.setVisibility(View.VISIBLE);
            binding.textSelectedCount.setText(getString(R.string.files_selected, items.size()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
