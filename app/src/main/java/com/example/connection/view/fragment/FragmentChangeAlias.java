package com.example.connection.view.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.connection.R;
import com.example.connection.adapter.ChangeAliasAdapter;
import com.example.connection.model.ChatRoom;
import com.example.connection.viewmodel.FragmentChangeAliasViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FragmentChangeAlias extends BottomSheetDialogFragment {

    private ChatRoom chatRoom;
    private final String TAG = getClass().getName();
    private ImageView img_back;
    private RecyclerView recyclerView;
    private FragmentChangeAliasViewModel fragmentChangeAliasViewModel;
    private ChangeAliasAdapter adapter;

    public FragmentChangeAlias(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentChangeAliasViewModel = new ViewModelProvider(this).get(FragmentChangeAliasViewModel.class);
        if (getArguments() != null) {
            ChatRoom chatRoomTmp = getArguments().getParcelable("chat_room_to_change_alias");
            Log.e("kiemtrachatroom", "onCreate: alias" + chatRoomTmp);
            if (chatRoomTmp != null) chatRoom = chatRoomTmp;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_alias, container, false);
        initView(view);
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        img_back.setOnClickListener(view12 -> requireDialog().dismiss());

        fragmentChangeAliasViewModel.getChatRoomListeningLiveData().observe(getViewLifecycleOwner(), tmp -> {
            chatRoom = tmp;
            adapter.setData(chatRoom.getAvtSender(), chatRoom.getAlias());
        });

        adapter = new ChangeAliasAdapter(requireContext(), chatRoom.getUserId(), chatRoom.getAvtSender(), chatRoom.getAlias(), (text_alias, id) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(FragmentChangeAlias.this.requireContext());
            @SuppressLint("InflateParams") View view1 = FragmentChangeAlias.this.requireActivity().getLayoutInflater().inflate(R.layout.custom_dialog_change_alias, null);
            Button btn_cancel = view1.findViewById(R.id.btn_cancel_change_alias);
            Button btn_remove = view1.findViewById(R.id.btn_remove_alias);
            Button btn_confirm = view1.findViewById(R.id.btn_confirm_alias);
            EditText edt_alias = view1.findViewById(R.id.edt_set_alias);
            String alias = chatRoom.getAlias().get(id);
            Log.e(TAG, "onViewCreated: " + alias);
            if (alias != null) {
                if (alias.contains("_")) {
                    edt_alias.setText(alias.split("_")[1]);
                } else {
                    edt_alias.setText(alias);
                }
            }
            builder.setTitle("Chỉnh sửa biệt danh");
            builder.setMessage("Biệt danh này chỉ được nhìn thấy trong đoạn chat");
            builder.setView(view1);
            AlertDialog dialog = builder.create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                dialog.show();
            } else {
                Toast.makeText(FragmentChangeAlias.this.requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
            }
            btn_cancel.setOnClickListener(view2 -> dialog.dismiss());
            btn_remove.setOnClickListener(view22 -> {
                if (alias != null) {
                    if (!alias.contains("_")) return;
                    edt_alias.setText("");
                    text_alias.setText(alias.split("_")[0]);
                }
                fragmentChangeAliasViewModel.updateAlias(chatRoom, id, null, chatRoom.getAlias());
                dialog.dismiss();
            });
            btn_confirm.setOnClickListener(view22 -> {
                String aliasNew = edt_alias.getText().toString().trim();
                if (alias != null && !alias.contains("_") && alias.equals(aliasNew)) return;
                if (!aliasNew.isEmpty()) {
                    text_alias.setText(aliasNew);
                    Log.e("aliascheck", "onViewCreated: truoc" + chatRoom.getAlias());
                    fragmentChangeAliasViewModel.updateAlias(chatRoom, id, aliasNew, chatRoom.getAlias());
                    Log.e("aliascheck", "onViewCreated: sau" + chatRoom.getAlias());
                    dialog.dismiss();
                }
            });
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void initView(View view) {
        img_back = view.findViewById(R.id.img_back_change_alias);
        recyclerView = view.findViewById(R.id.recycler_view_alias);
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

    @Override
    public void onStart() {
        super.onStart();
        fragmentChangeAliasViewModel.listeningChatRoom(chatRoom);
    }

    @Override
    public void onStop() {
        super.onStop();
        fragmentChangeAliasViewModel.unregisterListeningChatRoom();
    }

}