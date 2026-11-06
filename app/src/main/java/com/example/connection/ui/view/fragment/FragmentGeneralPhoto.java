package com.example.connection.ui.view.fragment;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.adapter.GeneralPhotoAdapter;
import com.example.connection.model.Image;
import com.example.connection.constants.MediaPlayerUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

public class FragmentGeneralPhoto extends Fragment {

    private RecyclerView recyclerView;
    private GeneralPhotoAdapter adapter;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
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
        setupBackPress();
    }

    private void setUpRecyclerView() {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Query query = db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100);

        FirestoreRecyclerOptions<Image> options = new FirestoreRecyclerOptions.Builder<Image>()
                .setQuery(query, Image.class)
                .build();

        adapter = new GeneralPhotoAdapter(options, getContext(), position -> {
            FragmentShowPhoto fragmentShowPhoto = new FragmentShowPhoto();
            Bundle bundle = new Bundle();
            bundle.putInt("position_key", position);
            bundle.putBoolean("isGeneralClick", true);
            Log.e("Position", position + "general");
            fragmentShowPhoto.setArguments(bundle);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragmentShowPhoto, "FragmentShowPhoto")
                    .addToBackStack("FragmentShowPhoto")
                    .commit();
        }) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                if (layoutEmpty != null) {
                    layoutEmpty.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Log.e(TAG, "onError: " + e.getMessage());
            }
        };

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setAdapter(adapter);
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_general_photo);
        progressBar = view.findViewById(R.id.progress_general_photo);
        layoutEmpty = view.findViewById(R.id.layout_empty_general);
        db = FirebaseFirestore.getInstance();
    }

    private void setupBackPress() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.setClick(false);
                FragmentManager manager = requireActivity().getSupportFragmentManager();
                fragmentHome = (FragmentHome) manager.findFragmentByTag("FragmentHome");
                if (fragmentHome == null) fragmentHome = new FragmentHome();
                manager.beginTransaction()
                        .replace(R.id.frame_layout, fragmentHome)
                        .commit();
                MediaPlayerUtil.getInstance().setWhenBackPressFromGeneral(true);
                Log.e("backpress", "handleOnBackPressed: ");
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e(TAG, "onDestroyView: ");
        MediaPlayerUtil.getInstance().setIsInGeneral(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
    }
}
