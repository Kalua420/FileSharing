package com.example.first;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabAdaptor extends FragmentStateAdapter {
    public TabAdaptor(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 1:
                return new Audios();
            case 2:
                return new Videos();
            case 3:
                return new Files();
            default:
                return new Images();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
