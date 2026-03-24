package com.swiftshare.app.ui.send;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.swiftshare.app.data.model.MediaItem;
import com.swiftshare.app.databinding.FragmentMediaPickerBinding;
import com.swiftshare.app.ui.adapter.MediaAdapter;
import com.swiftshare.app.ui.viewmodel.SendViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable fragment for each media category (Photo, Video, Music).
 * Loads content from MediaStore and manages grid selection.
 */
public class MediaPickerFragment extends Fragment implements MediaAdapter.OnItemClickListener {

    private static final String ARG_TYPE = "type";
    private FragmentMediaPickerBinding binding;
    private MediaItem.Type type;
    private MediaAdapter adapter;
    private SendViewModel viewModel;

    public static MediaPickerFragment newInstance(MediaItem.Type type) {
        MediaPickerFragment fragment = new MediaPickerFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = (MediaItem.Type) getArguments().getSerializable(ARG_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMediaPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SendViewModel.class);
        setupRecyclerView();
        loadMedia();
    }

    private void setupRecyclerView() {
        adapter = new MediaAdapter(this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadMedia() {
        binding.progressLoader.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<MediaItem> items = queryMediaStore();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    updateSelectionState(items);
                    adapter.submitList(items);
                    binding.progressLoader.setVisibility(View.GONE);
                    binding.textEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }

    private List<MediaItem> queryMediaStore() {
        List<MediaItem> items = new ArrayList<>();
        Uri contentUri;
        String[] projection;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        switch (type) {
            case PHOTO:
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATE_ADDED};
                break;
            case VIDEO:
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DURATION};
                break;
            case MUSIC:
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DURATION};
                selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                break;
            default:
                return items;
        }

        try (Cursor cursor = requireContext().getContentResolver().query(contentUri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
                int durationColumn = (type == MediaItem.Type.VIDEO || type == MediaItem.Type.MUSIC) ? cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION) : -1;

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    long date = cursor.getLong(dateColumn);
                    
                    String durationStr = null;
                    if (durationColumn != -1) {
                        long duration = cursor.getLong(durationColumn);
                        durationStr = formatDuration(duration);
                    }

                    Uri uri = ContentUris.withAppendedId(contentUri, id);
                    items.add(new MediaItem(id, name, size, date, uri, type, durationStr));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private void updateSelectionState(List<MediaItem> items) {
        List<MediaItem> selected = viewModel.getSelectedItems().getValue();
        if (selected != null) {
            for (MediaItem item : items) {
                if (selected.contains(item)) {
                    item.setSelected(true);
                }
            }
        }
    }

    private String formatDuration(long durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        long hours = (durationMs / (1000 * 60 * 60)) % 24;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onItemClick(MediaItem item) {
        item.setSelected(!item.isSelected());
        viewModel.toggleSelection(item);
        adapter.notifyItemChanged(adapter.getCurrentList().indexOf(item));
    }

    @Override
    public void onItemLongClick(MediaItem item) {
        onItemClick(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
