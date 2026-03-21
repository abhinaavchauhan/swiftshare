package com.swiftshare.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.swiftshare.app.R;
import com.swiftshare.app.data.model.TransferEntity;
import com.swiftshare.app.databinding.ItemTransferHistoryBinding;
import com.swiftshare.app.utils.FileUtils;

/**
 * RecyclerView adapter for transfer history items.
 * Uses ListAdapter with DiffUtil for efficient list updates.
 */
public class TransferHistoryAdapter extends ListAdapter<TransferEntity, TransferHistoryAdapter.ViewHolder> {

    private OnItemClickListener clickListener;

    public TransferHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<TransferEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransferEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransferEntity oldItem,
                                               @NonNull TransferEntity newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransferEntity oldItem,
                                                  @NonNull TransferEntity newItem) {
                    return oldItem.getFileName().equals(newItem.getFileName()) &&
                            oldItem.getStatus().equals(newItem.getStatus()) &&
                            oldItem.getProgress() == newItem.getProgress();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransferHistoryBinding binding = ItemTransferHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransferEntity item = getItem(position);
        holder.bind(item);

        // Staggered entrance animation
        holder.itemView.startAnimation(
                AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.fade_scale_in));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    // ── ViewHolder ───────────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransferHistoryBinding binding;

        ViewHolder(ItemTransferHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onItemClick(getItem(pos));
                }
            });
        }

        void bind(TransferEntity item) {
            binding.textFileName.setText(item.getFileName());
            binding.textFileSize.setText(FileUtils.formatFileSize(item.getFileSize()));
            binding.textDate.setText(FileUtils.formatTimestamp(item.getTimestamp()));

            // Direction label
            boolean isSent = item.isSent();
            binding.textDirection.setText(isSent ? "Sent" : "Received");

            // Status icon
            if (item.isCompleted()) {
                binding.iconStatus.setImageResource(R.drawable.ic_check_circle);
                binding.iconStatus.setColorFilter(
                        itemView.getContext().getColor(R.color.success));
            } else if ("FAILED".equals(item.getStatus()) || "CANCELLED".equals(item.getStatus())) {
                binding.iconStatus.setImageResource(R.drawable.ic_error_circle);
                binding.iconStatus.setColorFilter(
                        itemView.getContext().getColor(R.color.error));
            } else {
                binding.iconStatus.setImageResource(R.drawable.ic_history);
                binding.iconStatus.setColorFilter(
                        itemView.getContext().getColor(R.color.warning));
            }

            // File type icon tint
            binding.iconFileType.setColorFilter(
                    itemView.getContext().getColor(isSent ? R.color.primary : R.color.secondary));
        }
    }

    // ── Click Listener ───────────────────────────────

    public interface OnItemClickListener {
        void onItemClick(TransferEntity item);
    }
}
