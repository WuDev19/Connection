package com.example.connection.view.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.adapter.ChoiceEditChatGroupAdapter;
import com.example.connection.adapter.UserSearchAddToGroupAdapter;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.ChoiceEditGroupModel;
import com.example.connection.model.User;
import com.example.connection.viewmodel.FragmentChangeInfoChatViewModel;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;

public class FragmentChangeInfoChat extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private FragmentChangeInfoChatViewModel fragmentChangeInfoChatViewModel;
    private ActivityResultLauncher<Intent> resultLauncher;
    private Uri uri;
    private CircleImageView avt_group;
    private AlertDialog dialog, dialog2, dialog4, dialog5;
    private ChatRoom chatRoom;
    private UserSearchAddToGroupAdapter userSearchAddToGroupAdapter;
    private LiveData<List<User>> currentLiveData;
    private final OnStateFragmentAlias onStateFragmentAlias;

    public interface OnStateFragmentAlias{
        void onHideFrag();
    }

    public FragmentChangeInfoChat(OnStateFragmentAlias onStateFragmentAlias) {
        this.onStateFragmentAlias = onStateFragmentAlias;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentChangeInfoChatViewModel = new ViewModelProvider(this).get(FragmentChangeInfoChatViewModel.class);
        if (getArguments() != null) {
            chatRoom = (ChatRoom) getArguments().get("chat_room1");
            Log.e("Chatroom", "onCreate: " + chatRoom);
            if (chatRoom != null) {
                fragmentChangeInfoChatViewModel.setChatRoom(chatRoom);
            }
        }
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null && intent.getData() != null) {
                    uri = intent.getData();
                    avt_group.setImageURI(uri);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_info_chat, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fragmentChangeInfoChatViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success -> {
            Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            } else if (dialog2 != null && dialog2.isShowing()) {
                dialog2.dismiss();
            } else if (dialog4 != null && dialog4.isShowing()) {
                dialog4.dismiss();
            } else if (dialog5 != null && dialog5.isShowing()) {
                dialog5.dismiss();
            }
        });

        fragmentChangeInfoChatViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        fragmentChangeInfoChatViewModel.getNotifyAlreadyExistInGroup().observe(getViewLifecycleOwner(),
                notify -> Toast.makeText(requireContext(), notify, Toast.LENGTH_LONG).show());

        List<ChoiceEditGroupModel> choiceEditGroupModelList = new ArrayList<>();
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_change_name_chat, "Thay đổi tên nhóm", "changeName"));
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_change_avt_group, "Thay đổi avatar nhóm", "changeAvt"));
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_nickname, "Đặt biệt danh", "changeAlias"));
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_add_member, "Thêm thành viên", "addMember"));
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_leave_chat, "Rời khỏi nhóm", "leaveChat"));
        choiceEditGroupModelList.add(new ChoiceEditGroupModel(R.drawable.icon_delete, "Xóa đoạn chat", "deleteChat"));

        ChoiceEditChatGroupAdapter adapter = new ChoiceEditChatGroupAdapter(requireContext(), choiceEditGroupModelList, tag -> {
            switch (tag) {
                case "changeName":
                    changeName();
                    break;
                case "changeAvt":
                    changeAvatar();
                    break;
                case "changeAlias":
                    changeAlias();
                    break;
                case "addMember":
                    addMember();
                    break;
                case "leaveChat":
                    leaveChat();
                    break;
                case "deleteChat":
                    deleteChat();
                    break;
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        RecyclerView.ItemDecoration decoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);
    }

    private void changeAlias() {
        FragmentChangeAlias fragmentChangeAlias = new FragmentChangeAlias(onStateFragmentAlias::onHideFrag);
        Bundle bundle = new Bundle();
        bundle.putParcelable("chat_room_to_change_alias", chatRoom);
        fragmentChangeAlias.setArguments(bundle);
        fragmentChangeAlias.show(requireActivity().getSupportFragmentManager(), fragmentChangeAlias.getTag());
    }

    private void deleteChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rời nhóm");
        builder.setMessage("Bạn có chác chắn muốn xóa đoạn chat ?");
        builder.setNegativeButton("Không", null);
        builder.setPositiveButton("Chắc chắn", (dialogInterface, i) -> fragmentChangeInfoChatViewModel.getDelete());
        dialog5 = builder.create();
        Window window = dialog5.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.custom_dialog);
            dialog5.show();
        } else {
            Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
        }
    }

    private void leaveChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rời nhóm");
        builder.setMessage("Bạn có chác chắn muôn rời khỏi nhóm ?");
        builder.setNegativeButton("Không", null);
        builder.setPositiveButton("Chắc chắn", (dialogInterface, i) -> fragmentChangeInfoChatViewModel.leaveChat());
        dialog5 = builder.create();
        Window window = dialog5.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.custom_dialog);
            dialog5.show();
        } else {
            Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
        }
    }

    private void addMember() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        @SuppressLint("InflateParams") View view4 = getLayoutInflater().inflate(R.layout.custom_dialog_add_member_group_chat, null);
        TextInputLayout textInputLayout = view4.findViewById(R.id.txt_layout_search_dialog);
        TextInputEditText edt_search = view4.findViewById(R.id.edt_search_dialog);
        RecyclerView recyclerViewDialog = view4.findViewById(R.id.recycler_view_search_dialog);
        Button btn_cancel = view4.findViewById(R.id.btn_cancel_save_member);
        Button btn_save = view4.findViewById(R.id.btn_save_member);
        userSearchAddToGroupAdapter = new UserSearchAddToGroupAdapter(requireContext(), new ArrayList<>());
        recyclerViewDialog.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewDialog.setAdapter(userSearchAddToGroupAdapter);
        builder.setTitle("Thêm thành viên nhóm");
        builder.setView(view4);
        dialog4 = builder.create();
        Window window = dialog4.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.custom_dialog);
            dialog4.show();
        } else {
            Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
        }
        textInputLayout.setStartIconOnClickListener(view1 -> {
            String username = Objects.requireNonNull(edt_search.getText()).toString().trim();
            searchUser(username);
        });
        edt_search.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH ||
                    id == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String username = Objects.requireNonNull(edt_search.getText()).toString().trim();
                searchUser(username);
                return true;
            }
            return false;
        });
        btn_cancel.setOnClickListener(view12 -> dialog4.dismiss());
        btn_save.setOnClickListener(view13 -> fragmentChangeInfoChatViewModel.addMemberToGroup(userSearchAddToGroupAdapter.getUserInGroup(),
                userSearchAddToGroupAdapter.getAvtUserInGroup()));
    }

    private void changeAvatar() {
        AlertDialog.Builder builder2 = new AlertDialog.Builder(requireContext());
        @SuppressLint("InflateParams") View view2 = getLayoutInflater().inflate(R.layout.custom_dialog_change_avt_group_chat, null);
        Button btn_cancel2 = view2.findViewById(R.id.btn_cancel_change_group_avt);
        Button btn_save2 = view2.findViewById(R.id.btn_save_group_avt);
        Button btn_choose = view2.findViewById(R.id.btn_choose_picture_group_chat);
        avt_group = view2.findViewById(R.id.avt_group_change);
        Glide.with(requireContext())
                .load(chatRoom.getChatRoomAvt())
                .error(R.drawable.icon_avatar)
                .into(avt_group);
        builder2.setTitle("Thay đổi avatar nhóm");
        builder2.setView(view2);
        dialog2 = builder2.create();
        Window window2 = dialog2.getWindow();
        if (window2 != null) {
            window2.setBackgroundDrawableResource(R.drawable.custom_dialog);
            dialog2.show();
        } else {
            Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
        }
        btn_cancel2.setOnClickListener(view21 -> dialog2.dismiss());
        btn_save2.setOnClickListener(view1 -> {
            Log.e("avatargroup", "changeAvatar: " + uri);
            if (uri != null) {
                fragmentChangeInfoChatViewModel.updateChatRoomAvatar(uri);
            }
        });
        btn_choose.setOnClickListener(view3 -> ImagePicker.with(FragmentChangeInfoChat.this)
                .compress(512)
                .galleryOnly()
                .cropSquare()
                .maxResultSize(512, 512)
                .createIntent(intent -> {
                    resultLauncher.launch(intent);
                    return null;
                }));
    }

    private void changeName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.custom_dialog_change_group_name, null);
        EditText editText = view1.findViewById(R.id.edt_change_group_name);
        Button btn_cancel = view1.findViewById(R.id.btn_cancel_change_group_name);
        Button btn_save = view1.findViewById(R.id.btn_save_group_name);
        builder.setTitle("Thay đổi tên nhóm");
        builder.setView(view1);
        dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.custom_dialog);
            dialog.show();
        } else {
            Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
        }
        btn_cancel.setOnClickListener(view2 -> dialog.dismiss());
        btn_save.setOnClickListener(view22 -> {
            String name = editText.getText().toString().trim();
            fragmentChangeInfoChatViewModel.updateChatNameGroup(name);
        });
    }

    private void searchUser(String username) {
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Nhập tên người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLiveData != null) {
            currentLiveData.removeObservers(this);
        }
        currentLiveData = fragmentChangeInfoChatViewModel.searchUser(username);
        currentLiveData.observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                if (users.isEmpty()) {
                    Toast.makeText(requireContext(), "Không tìm thấy người dùng", Toast.LENGTH_LONG).show();
                } else {
                    userSearchAddToGroupAdapter.setData(users);
                }
            }
        });

    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.list_choice_chat_group);
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