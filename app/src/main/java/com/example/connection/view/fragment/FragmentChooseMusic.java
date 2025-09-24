package com.example.connection.view.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.connection.R;
import com.example.connection.adapter.MusicAdapter;
import com.example.connection.model.MyMusic;
import com.example.connection.viewmodel.ChooseMusicViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class FragmentChooseMusic extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private ImageView img_play;
    private onSelectedMusic selectedMusic;
    private final String TAG = getClass().getName();
    private MusicAdapter adapter;
    private TextInputEditText txt_music;
    private TextView tv_cancel;
    private List<MyMusic> initMusic = new ArrayList<>();
    private MusicAdapter.MusicYoutubeViewHolder viewHolder;
    private ChooseMusicViewModel chooseMusicViewModel;

    public FragmentChooseMusic() {}

    public FragmentChooseMusic(onSelectedMusic selectedMusic) {
        this.selectedMusic = selectedMusic;
    }

    public interface onSelectedMusic {
        void onSelected(String name, String singer, String url, int position);

        void onShow();

        void onHide();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chooseMusicViewModel = new ViewModelProvider(this).get(ChooseMusicViewModel.class);
        chooseMusicViewModel.setMediaPlayerInChooseFrag();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choose_music, container, false);
        initView(view);
        return view;
    }

    //fix bug nút play nhạc
    // TODO: 9/23/2025  
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chooseMusicViewModel.getInitListMusic();

        chooseMusicViewModel.getListInitListMusic().observe(getViewLifecycleOwner(), initListMusic -> {
            adapter.setData(initListMusic);
            initMusic = initListMusic;
        });

        txt_music.setOnFocusChangeListener((view1, b) -> {
            if (b) {
                tv_cancel.setVisibility(View.VISIBLE);
            }
        });

        tv_cancel.setOnClickListener(view1 -> {
            txt_music.setText("");
            tv_cancel.setVisibility(View.INVISIBLE);
        });

        txt_music.addTextChangedListener(new TextWatcher() {

            private final Handler searchHandler = new Handler();
            private Runnable runnableSearch;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.e(TAG, "beforeTextChanged: ");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.e(TAG, "onTextChanged: ");
                tv_cancel.setVisibility(View.VISIBLE);
                if (runnableSearch != null) {
                    searchHandler.removeCallbacks(runnableSearch);
                }

                if (charSequence.toString().trim().isEmpty()) {
                    adapter.setData(initMusic);
                }

                else {
                    runnableSearch = () -> {
                        String name = charSequence.toString().trim() + "*";
                        chooseMusicViewModel.searchMusic(name);
                    };
                    searchHandler.postDelayed(runnableSearch, 300);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String title = editable.toString();
                Log.e(TAG, "afterTextChanged: ");
                chooseMusicViewModel.handleMusicAfterTextChange(title);
            }
        });

        chooseMusicViewModel.getMyMusicListLiveData().observe(getViewLifecycleOwner(), myMusicList -> adapter.setData(myMusicList));

        adapter = new MusicAdapter(requireContext(), initMusic, new MusicAdapter.OnItemClick() {
            @Override
            public void onNameItemClick(String musicName, String singerName, String url, int position) {
                selectedMusic.onSelected(musicName, singerName, url, position);
                requireDialog().dismiss();
            }

            @Override
            public void onPlayMusic(String url, int position, boolean isPlaying, ImageView imgPlay) {
                img_play = imgPlay;
                chooseMusicViewModel.handleMusic(url, position, isPlaying, requireContext().getApplicationContext());
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(true);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

            }
        });

        chooseMusicViewModel.getHideTvCancel().observe(getViewLifecycleOwner(), isHideTv -> {
            if(isHideTv){
                tv_cancel.setVisibility(View.INVISIBLE);
            }
        });

        chooseMusicViewModel.getChangeIconOldMusic().observe(getViewLifecycleOwner(), list -> {
            if(list != null){
                recyclerView.post(() -> {
                    viewHolder = (MusicAdapter.MusicYoutubeViewHolder)
                            recyclerView.findViewHolderForAdapterPosition((Integer) list[0]);
                    if (viewHolder != null) {
                        ImageView oldPositionImageView = viewHolder.itemView.findViewById(R.id.img_play);
                        oldPositionImageView.setImageResource(R.drawable.icon_play);
                    }
                    else {
                        adapter.notifyItemChanged((Integer) list[0]);
                    }
                });
            }
        });

        chooseMusicViewModel.getPlayComplete().observe(getViewLifecycleOwner(), complete -> {
            if(complete){
                img_play.setImageResource(R.drawable.icon_play);
            }
        });

        chooseMusicViewModel.getChangeIconPlayMusic().observe(getViewLifecycleOwner(), isChangePlayMusic -> {
            if(isChangePlayMusic){
                img_play.setImageResource(R.drawable.icon_pause);
            }
            else {
                img_play.setImageResource(R.drawable.icon_play);
            }
        });

        chooseMusicViewModel.getSetPlaying().observe(getViewLifecycleOwner(), playing -> adapter.setPlaying(playing));

    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_music_list);
        txt_music = view.findViewById(R.id.edt_search_music);
        tv_cancel = view.findViewById(R.id.tv_cancel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        chooseMusicViewModel.setMediaOnDestroy();
        Log.e(TAG, "onDestroy: ");
    }

    @Override
    public void onStart() {
        super.onStart();
        selectedMusic.onShow();
    }

    @Override
    public void onPause() {
        super.onPause();
        selectedMusic.onHide();
    }

}