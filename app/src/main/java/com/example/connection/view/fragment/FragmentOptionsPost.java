package com.example.connection.view.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.example.connection.R;
import com.example.connection.utils.MediaPlayerUtil;
import com.example.connection.viewmodel.OptionsPostViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FragmentOptionsPost extends BottomSheetDialogFragment {

    public RelativeLayout relative_delete, relative_download, relative_share;
    private String postId, url;
    private final String TAG = getClass().getName();
    private OptionsPostViewModel optionsPostViewModel;

    public FragmentOptionsPost() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        optionsPostViewModel = new ViewModelProvider(this).get(OptionsPostViewModel.class);
        if(getArguments() != null){
            postId = getArguments().getString("postIdOptions");
            url = getArguments().getString("postUrl");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_options_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

        optionsPostViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success -> {
            if(success != null && success.equals("deleteSuccess")){
                MediaPlayerUtil.getInstance().getMediaPlayer().reset();
            }
            else {
                Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show();
            }
            this.dismiss();
        });

        optionsPostViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            relative_delete.setEnabled(true);
            relative_download.setEnabled(true);
            relative_share.setEnabled(true);
        });

        relative_delete.setOnClickListener(view12 -> {
            optionsPostViewModel.deletePost(postId);
            relative_delete.setEnabled(false);
            relative_share.setEnabled(false);
            relative_download.setEnabled(false);
        });

        relative_download.setOnClickListener(view13 -> optionsPostViewModel.downloadPost(url, requireContext()));

        relative_share.setOnClickListener(view1 -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Connection - Kết nối tới mọi người, tôi chia sẻ cho bạn, hãy cùng thưởng thức nhé :" + url);
            requireContext().startActivity(Intent.createChooser(shareIntent, "Chia sẻ url post"));
            this.dismiss();
        });

    }

    private void initView(View view) {
        relative_delete = view.findViewById(R.id.relative_delete_post);
        relative_download = view.findViewById(R.id.relative_download_post);
        relative_share = view.findViewById(R.id.relative_share_post);
    }

}