package com.swiftshare.app.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.swiftshare.app.data.model.MediaItem;
import com.swiftshare.app.ui.send.MediaPickerFragment;

/**
 * PagerAdapter for the Send screen tabs.
 */
public class SendPagerAdapter extends FragmentStateAdapter {

    public SendPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MediaItem.Type type;
        switch (position) {
            case 0: type = MediaItem.Type.APP; break;
            case 1: type = MediaItem.Type.FILE; break;
            case 2: type = MediaItem.Type.MUSIC; break;
            case 3: type = MediaItem.Type.PHOTO; break;
            case 4: type = MediaItem.Type.VIDEO; break;
            default: type = MediaItem.Type.PHOTO; break;
        }
        return MediaPickerFragment.newInstance(type);
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
