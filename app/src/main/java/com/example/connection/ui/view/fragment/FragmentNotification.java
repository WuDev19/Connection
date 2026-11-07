package com.example.connection.ui.view.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.R;
import com.example.connection.ui.adapter.NotificationAdapter;
import com.example.connection.ui.viewmodel.MainViewModel;
import com.example.connection.ui.viewmodel.NotificationViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class FragmentNotification extends Fragment {

    private RecyclerView recyclerView;
    private List<String> listFriendConfirm;
    private NotificationAdapter adapter;
    private final String TAG = getClass().getName();
    private TextView tv_no_notification;
    private int numberOfNotification;
    private FragmentHome fragmentHome;
    private NotificationViewModel notificationViewModel;

    public FragmentNotification() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            Bundle bundle = getArguments();
            listFriendConfirm = bundle.getStringArrayList("confirm_friend");
            numberOfNotification = bundle.getInt("number");
        }

        notificationViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(
                        requireActivity().getApplication(),
                        this,
                        getArguments()))
                .get(NotificationViewModel.class);
        notificationViewModel.getFriendCurrentUser();

        backPress();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);
        initView(view);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (numberOfNotification < 1) tv_no_notification.setText("Hiện không có thông báo");

        setUpRecyclerView();

        notificationViewModel.getError().observe(getViewLifecycleOwner(), s ->
                Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show());

        notificationViewModel.getSetAdapter().observe(getViewLifecycleOwner(), listFriendConfirm ->
                adapter.setData(listFriendConfirm));

        notificationViewModel.getSetNotification().observe(getViewLifecycleOwner(), notification ->
                tv_no_notification.setText(notification));

    }

    private void setUpRecyclerView() {
        adapter = new NotificationAdapter(requireContext(), listFriendConfirm, new NotificationAdapter.onItemClick() {
            @Override
            public void onAccept(String otherUserId, int position) {
                notificationViewModel.onAccept(otherUserId, position).observe(getViewLifecycleOwner(), success ->
                        Toast.makeText(FragmentNotification.this.requireContext(), success, Toast.LENGTH_SHORT).show());
                notificationViewModel.numberOfNotification();
            }

            @Override
            public void onCancel(String otherUserId, int position) {
                notificationViewModel.onCancel(position);
                notificationViewModel.numberOfNotification();
            }
        }, notificationViewModel, getViewLifecycleOwner());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_notification);
        tv_no_notification = view.findViewById(R.id.tv_no_notification);
    }

    private void backPress() {
        FragmentManager manager = requireActivity().getSupportFragmentManager();
        fragmentHome = (FragmentHome) manager.findFragmentByTag("FragmentHome");
        if (fragmentHome == null) fragmentHome = new FragmentHome();
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FloatingActionButton floatingActionButton = requireActivity().findViewById(R.id.floating_action_button);
                TextView tv_notification = requireActivity().findViewById(R.id.tv_notification);
                FrameLayout frameLayout = requireActivity().findViewById(R.id.frame_layout_notification);
                floatingActionButton.show();
                frameLayout.setVisibility(View.VISIBLE);
                tv_notification.setVisibility(View.VISIBLE);
                manager.beginTransaction()
                        .replace(R.id.frame_layout, fragmentHome)
                        .commit();
                MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
                mainViewModel.setCameraMusicPlaying(true);
            }
        });
    }

}