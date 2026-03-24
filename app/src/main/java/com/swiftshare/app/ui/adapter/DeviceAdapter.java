package com.swiftshare.app.ui.adapter;

import android.view.LayoutInflater;
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
 * RecyclerView adapter for discovered Wi-Fi Direct devices.
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
                            oldItem.getStatus() == newItem.getStatus();
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
            String name = item.getName();
            binding.textDeviceName.setText(name);
            binding.textDeviceStatus.setText(item.getStatusLabel());
            
            // Set initial
            if (name != null && !name.isEmpty()) {
                binding.textAvatarInitial.setText(name.substring(0, 1).toUpperCase());
            }
            
            // Set indicator color based on status
            int statusColor;
            switch (item.getStatusLabel()) {
                case "Connected":
                    statusColor = R.color.success;
                    break;
                case "Invited":
                    statusColor = R.color.warning;
                    break;
                default:
                    statusColor = R.color.on_surface_variant;
                    break;
            }
            binding.textDeviceStatus.setTextColor(itemView.getContext().getColor(statusColor));
            
            // Highlight if connecting
            if (item.isConnecting()) {
                binding.textDeviceStatus.setText("Connecting...");
                binding.textDeviceStatus.setTextColor(itemView.getContext().getColor(R.color.warning));
            }
        }
    }

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceItem device);
    }
}
