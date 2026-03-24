package com.swiftshare.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.swiftshare.app.R;
import com.swiftshare.app.data.model.MediaItem;
import com.swiftshare.app.databinding.ItemMediaBinding;
import com.swiftshare.app.utils.FileUtils;

/**
 * Adapter for displaying media items (photos, videos, etc.) in a grid.
 */
public class MediaAdapter extends ListAdapter<MediaItem, MediaAdapter.ViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MediaItem item);
        void onItemLongClick(MediaItem item);
    }

    public MediaAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<MediaItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MediaItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
                    return oldItem.isSelected() == newItem.isSelected() &&
                            oldItem.getId() == newItem.getId();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMediaBinding binding = ItemMediaBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMediaBinding binding;

        ViewHolder(ItemMediaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(pos));
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemLongClick(getItem(pos));
                    return true;
                }
                return false;
            });
        }

        void bind(MediaItem item) {
            binding.textSize.setText(FileUtils.formatFileSize(item.getSize()));
            
            if (item.getDuration() != null && !item.getDuration().isEmpty()) {
                binding.textDuration.setVisibility(View.VISIBLE);
                binding.textDuration.setText(item.getDuration());
            } else {
                binding.textDuration.setVisibility(View.GONE);
            }

            // Selection state
            if (item.isSelected()) {
                binding.getRoot().setStrokeColor(itemView.getContext().getColor(R.color.primary));
                binding.cardSelectionOuter.setCardBackgroundColor(itemView.getContext().getColor(R.color.primary));
                binding.imageCheck.setVisibility(View.VISIBLE);
            } else {
                binding.getRoot().setStrokeColor(0);
                binding.cardSelectionOuter.setCardBackgroundColor(0x66000000);
                binding.imageCheck.setVisibility(View.GONE);
            }

            // Load Thumbnail
            Glide.with(itemView.getContext())
                    .load(item.getUri())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.color.surface_container)
                    .centerCrop()
                    .into(binding.imageThumbnail);
        }
    }
}
