package com.example.connection.ui.view.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.ui.adapter.BackGroundChatAdapter;
import com.example.connection.ui.viewmodel.ChangeBackGroundChatViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FragmentChangeBackgroundChat extends BottomSheetDialogFragment {

    private String chatId;
    private RecyclerView recyclerView;
    private ChangeBackGroundChatViewModel changeBackGroundChatViewModel;
    private BackGroundChatAdapter adapter;
    private final String TAG = getClass().getName();
    private AlertDialog dialog;

    public FragmentChangeBackgroundChat() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeBackGroundChatViewModel = new ViewModelProvider(this).get(ChangeBackGroundChatViewModel.class);
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            Log.e(TAG, "onCreate: " + chatId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_background_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view_back_ground);

        changeBackGroundChatViewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            if(success){
                dismiss();
                if(dialog.isShowing()){
                    dialog.dismiss();
                }
            }
        });

        changeBackGroundChatViewModel.getBackGround().observe(getViewLifecycleOwner(), list -> {
            if(list != null){
                adapter = new BackGroundChatAdapter(requireContext(), list, url -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    @SuppressLint("InflateParams") View view1 = requireActivity().getLayoutInflater().inflate(R.layout.custom_dialog_background_preview, null);
                    Button btn_cancel = view1.findViewById(R.id.btn_cancel_preview);
                    Button btn_select = view1.findViewById(R.id.btn_select_preview);
                    ImageView img = view1.findViewById(R.id.img_preview);
                    Glide.with(requireContext())
                            .load(url)
                            .error(R.color.background)
                            .into(img);
                    builder.setView(view1);
                    dialog = builder.create();
                    Window window = dialog.getWindow();
                    if(window != null){
                        window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                        dialog.show();
                    }
                    btn_select.setOnClickListener(view2 -> changeBackGroundChatViewModel.updateBackGround(chatId, url));
                    btn_cancel.setOnClickListener(view2 -> dialog.dismiss());
                });
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog dialog = (BottomSheetDialog) dialogInterface;
            FrameLayout frameLayout = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (frameLayout != null) {
                frameLayout.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                frameLayout.requestLayout();
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(frameLayout);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return bottomSheetDialog;
    }

}