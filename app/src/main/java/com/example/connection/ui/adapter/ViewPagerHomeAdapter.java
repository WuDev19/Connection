package com.example.connection.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.connection.ui.view.fragment.FragmentShowPhoto;
import com.example.connection.ui.view.fragment.FragmentTakePhoto;

public class ViewPagerHomeAdapter extends FragmentStateAdapter {

    public ViewPagerHomeAdapter(@NonNull Fragment fragment) {
        super(fragment);
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
