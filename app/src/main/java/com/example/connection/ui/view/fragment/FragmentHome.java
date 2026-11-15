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
    private int selectedPage;
    private ViewPager2.OnPageChangeCallback pageCallback;

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

        if (savedInstanceState != null) selectedPage = savedInstanceState.getInt("home_page", selectedPage);
        int pageToRestore = selectedPage;
        viewPager2 = view.findViewById(R.id.view_pager);
        ViewPagerHomeAdapter homeAdapter = new ViewPagerHomeAdapter(this);
        viewPager2.setAdapter(homeAdapter);
        pageCallback = new ViewPager2.OnPageChangeCallback() {

            final MainActivity mainActivity = (MainActivity) requireActivity();

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                selectedPage = position;
                if(position == 0){
                    mainActivity.setShowFab();
                    MediaPlayerUtil.getInstance().setPosition(0);
                    MediaPlayerUtil.getInstance().pauseMusic();
                }
                else {
                    mainActivity.setHideFab();
                    MediaPlayerUtil.getInstance().setPosition(1);

                }
            }
        };
        viewPager2.registerOnPageChangeCallback(pageCallback);
        viewPager2.setCurrentItem(pageToRestore, false);

    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).setClick(false);
        MediaPlayerUtil.getInstance().setIsInGeneral(false);
        MediaPlayerUtil.getInstance().setPosition(selectedPage);
        MainActivity activity = (MainActivity) requireActivity();
        if (selectedPage == 0) activity.setShowFab();
        else activity.setHideFab();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("home_page", selectedPage);
    }

    @Override
    public void onDestroyView() {
        if (viewPager2 != null && pageCallback != null) viewPager2.unregisterOnPageChangeCallback(pageCallback);
        if (viewPager2 != null) viewPager2.setAdapter(null);
        viewPager2 = null;
        super.onDestroyView();
    }
}
