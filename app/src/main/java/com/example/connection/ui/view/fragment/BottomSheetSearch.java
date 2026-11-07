package com.example.connection.ui.view.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.ui.viewmodel.MainViewModel;
import com.example.connection.ui.view.activity.chat.ChatActivity;
import com.example.connection.ui.adapter.ShowFriendAdapter;
import com.example.connection.ui.adapter.UserSearchAdapter;
import com.example.connection.model.User;
import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.ui.viewmodel.BottomSheetViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class BottomSheetSearch extends BottomSheetDialogFragment {

    private TextInputEditText edt_search;
    private TextView tv_ok, tv_exist;
    private RecyclerView recyclerView, recyclerViewFriend;
    private final String TAG = getClass().getName();
    private ImageView img_search;
    private BottomSheetViewModel bottomSheetViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bottomSheetViewModel = new ViewModelProvider(this).get(BottomSheetViewModel.class);
        bottomSheetViewModel.getUserFromFirebase();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottomsheet, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpRecyclerViewFriend();

        edt_search.setOnFocusChangeListener((view13, b) -> {
            if (b) {
                tv_ok.setVisibility(View.VISIBLE);
            }
        });

        tv_ok.setOnClickListener(view1 -> {
            edt_search.setText("");
            tv_ok.setVisibility(View.INVISIBLE);
        });

        edt_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tv_ok.setVisibility(View.VISIBLE);
                tv_ok.setText("Hủy");
                tv_exist.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().isEmpty()) {
                    tv_ok.setVisibility(View.INVISIBLE);
                }
            }
        });

        img_search.setOnClickListener(view12 -> searchUsername());

        edt_search.setOnEditorActionListener((textView, id, keyEvent) -> {
            if(id == EditorInfo.IME_ACTION_SEARCH ||
                    id == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)){
                searchUsername();
                return true;
            }
            return false;
        });

        bottomSheetViewModel.getSuccessLiveData().observe(getViewLifecycleOwner(), success -> Toast.makeText(requireContext(), success, Toast.LENGTH_SHORT).show());

        bottomSheetViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show());

        bottomSheetViewModel.getQuerySearchLiveData().observe(getViewLifecycleOwner(), query -> {
            if (query != null) {
                FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .setLifecycleOwner(getViewLifecycleOwner())
                        .build();
                UserSearchAdapter adapter = new UserSearchAdapter(options, requireContext(), new UserSearchAdapter.onItemClick() {
                    @Override
                    public void onChatListener(User user) {
                        chat(user);
                    }

                    @Override
                    public void onAddListener(String otherUserId) {
                        bottomSheetViewModel.addFriend(otherUserId);
                    }
                }, tv_exist);
                recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                recyclerView.setAdapter(adapter);
            }
        });

    }

    private void setUpRecyclerViewFriend() {
        bottomSheetViewModel.getQueryFriend();
        bottomSheetViewModel.getQueryFriendLiveData().observe(getViewLifecycleOwner(), query -> {
            if (query != null) {
                FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(query, User.class)
                        .setLifecycleOwner(getViewLifecycleOwner())
                        .build();
                ShowFriendAdapter showFriendAdapter = new ShowFriendAdapter(options, requireContext(), new ShowFriendAdapter.onItemClickListener() {
                    @Override
                    public void deleteFriend(String friendId) {
                        bottomSheetViewModel.delete(friendId);
                    }

                    @Override
                    public void chatFriend(User friend) {
                        chat(friend);
                    }
                });
                recyclerViewFriend.setLayoutManager(new LinearLayoutManager(requireContext()));
                recyclerViewFriend.setAdapter(showFriendAdapter);
            }
        });
    }

    private void searchUsername() {
        String username = Objects.requireNonNull(edt_search.getText()).toString().trim();
        if (username.isEmpty()) return;
        bottomSheetViewModel.getQuerySearch(username);
    }

    private void initView(View view) {
        edt_search = view.findViewById(R.id.edt_search);
        tv_ok = view.findViewById(R.id.tv_ok);
        tv_exist = view.findViewById(R.id.tv_exist);
        recyclerView = view.findViewById(R.id.recycler_view_search);
        recyclerViewFriend = view.findViewById(R.id.recycler_view_show_friend);
        img_search = view.findViewById(R.id.img_search);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        if (!MediaPlayerUtil.getInstance().isCLickToPause()) {
            MediaPlayerUtil.getInstance().resumeMusic();
        }
    }

    private void chat(User friend) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("username_chat", friend.getUserName());
        intent.putExtra("avatar_chat", friend.getAvatarUser());
        intent.putExtra("user_id", friend.getId());
        if (friend.getSdt() == null) {
            intent.putExtra("check", "email");
            intent.putExtra("email_chat", friend.getEmail());
        } else {
            intent.putExtra("check", "sdt");
            intent.putExtra("phone_chat", friend.getSdt());
        }
        requireActivity().startActivity(intent);
        requireDialog().dismiss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
            mainViewModel.setCameraMusicPlaying(true);
        }
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
