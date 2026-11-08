package com.example.connection.ui.view.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.adapter.ViewPagerHomeAdapter;
import com.example.connection.constants.MediaPlayerUtil;

public class FragmentHome extends Fragment {

    private ViewPager2 viewPager2;

    public FragmentHome(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager2 = view.findViewById(R.id.view_pager);
        ViewPagerHomeAdapter homeAdapter = new ViewPagerHomeAdapter(requireActivity());
        viewPager2.setAdapter(homeAdapter);
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {

            final MainActivity mainActivity = (MainActivity) requireActivity();

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if(position == 0){
                    mainActivity.setShowFab();
                    MediaPlayerUtil.getInstance().setPosition(0);
                    MediaPlayerUtil.getInstance().pauseMusic();
                }
                else {
                    mainActivity.setHideFab();
                    MediaPlayerUtil.getInstance().setPosition(1);
                    MediaPlayerUtil.getInstance().resumeMusic();
                }
            }
        });

    }

}