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
import com.swiftshare.app.data.model.DeviceItem;
import com.swiftshare.app.databinding.ItemDeviceBinding;

/**
 * RecyclerView adapter for discovered Bluetooth devices.
 * Shows device name, pairing status, and signal strength.
 */
public class DeviceAdapter extends ListAdapter<DeviceItem, DeviceAdapter.ViewHolder> {

    private OnDeviceClickListener clickListener;

    public DeviceAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<DeviceItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DeviceItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull DeviceItem oldItem,
                                               @NonNull DeviceItem newItem) {
                    return oldItem.getAddress().equals(newItem.getAddress());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DeviceItem oldItem,
                                                  @NonNull DeviceItem newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.isPaired() == newItem.isPaired();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceBinding binding = ItemDeviceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceItem item = getItem(position);
        holder.bind(item);

        // Staggered animation
        holder.itemView.startAnimation(
                AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.fade_scale_in));
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.clickListener = listener;
    }

    // ── ViewHolder ───────────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeviceBinding binding;

        ViewHolder(ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onDeviceClick(getItem(pos));
                }
            });
        }

        void bind(DeviceItem item) {
            binding.textDeviceName.setText(item.getName());

            // Status text
            if (item.isConnecting()) {
                binding.textDeviceStatus.setText("Connecting...");
                binding.textDeviceStatus.setTextColor(
                        itemView.getContext().getColor(R.color.warning));
            } else if (item.isPaired()) {
                binding.textDeviceStatus.setText("Paired");
                binding.textDeviceStatus.setTextColor(
                        itemView.getContext().getColor(R.color.success));
            } else {
                binding.textDeviceStatus.setText("Available");
                binding.textDeviceStatus.setTextColor(
                        itemView.getContext().getColor(R.color.on_surface_variant));
            }

            // Signal strength
            binding.textSignal.setText(item.getSignalLabel());

            int signalColor;
            if (item.getSignalStrength() >= -70) {
                signalColor = R.color.success;
            } else if (item.getSignalStrength() >= -85) {
                signalColor = R.color.warning;
            } else {
                signalColor = R.color.error;
            }
            binding.textSignal.setTextColor(itemView.getContext().getColor(signalColor));
        }
    }

    // ── Click Listener ───────────────────────────────

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }
}
