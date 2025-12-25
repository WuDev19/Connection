package com.example.connection.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.connection.ui.view.fragment.FragmentShowPhoto;
import com.example.connection.ui.view.fragment.FragmentTakePhoto;

public class ViewPagerHomeAdapter extends FragmentStateAdapter {

    public ViewPagerHomeAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 0) return new FragmentTakePhoto();
        else return new FragmentShowPhoto();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

}
