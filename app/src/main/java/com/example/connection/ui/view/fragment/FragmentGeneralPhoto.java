package com.example.connection.ui.view.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.adapter.GeneralPhotoAdapter;
import com.example.connection.model.Image;
import com.example.connection.constants.MediaPlayerUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FragmentGeneralPhoto extends Fragment {

    private RecyclerView recyclerView;
    private GeneralPhotoAdapter adapter;
    private FirebaseFirestore db;
    private final String TAG = "FragmentGeneralPhoto";
    private FragmentHome fragmentHome;

    public FragmentGeneralPhoto() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_general_photo, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        String myId = FirebaseAuth.getInstance().getUid();
        if(myId == null) return;
        Query query = db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<Image> options = new FirestoreRecyclerOptions.Builder<Image>()
                .setQuery(query, Image.class)
                .build();
        adapter = new GeneralPhotoAdapter(options, getContext(), position -> {
            FragmentShowPhoto fragmentShowPhoto = new FragmentShowPhoto();
            Bundle bundle = new Bundle();
            bundle.putInt("position_key", position);
            bundle.putBoolean("isGeneralClick", true);
            Log.e("Position", position+"general" );
            fragmentShowPhoto.setArguments(bundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragmentShowPhoto, "FragmentShowPhoto")
                    .addToBackStack("FragmentShowPhoto")
                    .commit();
        });
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_general_photo);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: " );
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: " );
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }
        Log.e(TAG, "onResume: " );
        onBackPress();
    }

    private void onBackPress(){
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.setClick(false);
                FragmentManager manager = requireActivity().getSupportFragmentManager();
                fragmentHome = (FragmentHome) manager.findFragmentByTag("FragmentHome");
                if(fragmentHome == null) fragmentHome = new FragmentHome();
                manager.beginTransaction()
                        .replace(R.id.frame_layout, fragmentHome)
                        .commit();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Fragment fragment = manager.findFragmentById(R.id.frame_layout);
                    if (fragment instanceof FragmentHome) {
                        ((FragmentHome) fragment).setCurrentPage(0);
                    }
                }, 100);
                MediaPlayerUtil.getInstance().setWhenBackPressFromGeneral(true);
                Log.e("backpress", "handleOnBackPressed: ");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        if (adapter != null){
            adapter.stopListening();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView: ");
        MediaPlayerUtil.getInstance().setIsInGeneral(false);
    }

}