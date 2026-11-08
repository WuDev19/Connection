package com.example.connection.ui.view.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connection.R;
import com.example.connection.ui.adapter.UserSearchAddToGroupAdapter;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.ui.viewmodel.CreateGroupChatViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentCreateGroupChat extends BottomSheetDialogFragment {

    private TextView tv_create_done;
    private EditText edt_group_name;
    private TextInputLayout txt_layout_search_member;
    private TextInputEditText edt_search_user_to_add;
    private RecyclerView recycler_view_friend_to_create_group;
    private UserSearchAddToGroupAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private CreateGroupChatViewModel createGroupChatViewModel;
    private LiveData<List<User>> currentLiveData;
    private String oldGroupName = "";
    private List<String> userInGroup = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createGroupChatViewModel = new ViewModelProvider(this).get(CreateGroupChatViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_group_chat, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        tv_create_done = view.findViewById(R.id.tv_create_done);
        edt_group_name = view.findViewById(R.id.edt_group_name);
        txt_layout_search_member = view.findViewById(R.id.txt_layout_search_member);
        edt_search_user_to_add = view.findViewById(R.id.edt_search_user_to_add);
        recycler_view_friend_to_create_group = view.findViewById(R.id.recycler_view_friend_to_create_group);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpRecyclerView();

        createGroupChatViewModel.getListFriend();

        createGroupChatViewModel.getCurrentUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                adapter.setUser(user);
            }
        });

        createGroupChatViewModel.getListFriendLiveData().observe(getViewLifecycleOwner(), list -> {
            if (list != null) {
                for (String id : list) {
                    createGroupChatViewModel.getUserFriend(id);
                }
            }
        });

        createGroupChatViewModel.getUserFriendLiveData().observe(getViewLifecycleOwner(), userFriend -> {
            if (userFriend != null) {
                userList.add(userFriend);
                adapter.setData(userList);
            }
        });

        createGroupChatViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success -> {
            Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        createGroupChatViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        tv_create_done.setOnClickListener(view12 -> {
            String groupName = edt_group_name.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên group trước khi tạo", Toast.LENGTH_SHORT).show();
                return;
            }
            if (adapter.getUserInGroup() != null && adapter.getUserInGroup().size() <= 2) {
                if (adapter.getUserInGroup().size() == 1) {
                    Toast.makeText(requireContext(), "Hãy thêm ít nhất 2 người nữa", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Hãy thêm ít nhất 1 người nữa", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (oldGroupName.equals(groupName) && userInGroup.equals(adapter.getUserInGroup())) {
                Log.e("checkti", "onViewCreated: " + userInGroup + "\n" + adapter.getUserInGroup());
                Toast.makeText(requireContext(), "Trùng tên nhóm vừa đặt", Toast.LENGTH_SHORT).show();
                return;
            }
            oldGroupName = groupName;
            userInGroup = new ArrayList<>(adapter.getUserInGroup());
            Map<String, Long> mapDelete = new HashMap<>();
            Map<String, String> avtMap = new HashMap<>();
            Map<String, String> aliasMap = new HashMap<>();
            List<String> searchChat = new ArrayList<>();
            for (String id : adapter.getUserInGroup()) {
                mapDelete.put(id, Timestamp.now().toDate().getTime());
            }
            for (User user : adapter.getAvtUserInGroup()) {
                avtMap.put(user.getId(), user.getAvatarUser());
                aliasMap.put(user.getId(), user.getUserName());
                searchChat.add(user.getUserName().toLowerCase());
            }
            searchChat.add(groupName.toLowerCase());
            String myId = FirebaseAuth.getInstance().getUid();
            Timestamp now = Timestamp.now();
            ChatRoom chatRoom = new ChatRoom(null,
                    groupName,
                    null,
                    adapter.getUserInGroup(),
                    now,
                    myId,
                    "Đã tạo nhóm " + groupName,
                    mapDelete,
                    avtMap,
                    aliasMap,
                    searchChat,
                    "",
                    0);
            chatRoom.setUnreadCounts(new HashMap<>());
            createGroupChatViewModel.setChatRoomToFireStore(chatRoom);

            // Bắn thông báo mời vào nhóm cho các thành viên qua Backend Go
            Map<String, String> data = new HashMap<>();
            data.put("type", "GROUP_INVITE");
            String creatorName = aliasMap.get(myId);
            if (creatorName == null) creatorName = "Ai đó";
            com.example.connection.data.api.NotificationHelper.sendNotificationToUsers(
                    adapter.getUserInGroup(),
                    "Nhóm chat mới",
                    creatorName + " đã thêm bạn vào nhóm " + groupName,
                    data
            );
        });

        edt_search_user_to_add.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().isEmpty()) {
                    adapter.setData(userList);
                }
            }
        });

        txt_layout_search_member.setStartIconOnClickListener(view1 -> searchUser());

        edt_search_user_to_add.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH ||
                    id == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchUser();
                return true;
            }
            return false;
        });

    }

    private void setUpRecyclerView() {
        adapter = new UserSearchAddToGroupAdapter(requireContext(), userList);
        recycler_view_friend_to_create_group.setAdapter(adapter);
        recycler_view_friend_to_create_group.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void searchUser() {
        String username = Objects.requireNonNull(edt_search_user_to_add.getText()).toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Nhập tên người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentLiveData != null) {
            currentLiveData.removeObservers(this);
        }
        currentLiveData = createGroupChatViewModel.searchUser(username);
        currentLiveData.observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                if (users.isEmpty()) {
                    Toast.makeText(requireContext(), "Không tìm thấy người dùng", Toast.LENGTH_LONG).show();
                } else {
                    adapter.setData(users);
                }
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
