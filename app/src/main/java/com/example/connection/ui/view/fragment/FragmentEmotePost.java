package com.example.connection.ui.view.fragment;

import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.connection.R;
import com.example.connection.ui.adapter.EmotePostAdapter;
import com.example.connection.model.EmoteModel;
import com.example.connection.ui.viewmodel.FragmentEmotePostViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.Query;

public class FragmentEmotePost extends BottomSheetDialogFragment {

    private FragmentEmotePostViewModel fragmentEmotePostViewModel;
    private String postId;
    private EmotePostAdapter adapter;

    public FragmentEmotePost(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentEmotePostViewModel = new ViewModelProvider(this).get(FragmentEmotePostViewModel.class);
        if(getArguments() != null){
            postId = getArguments().getString("postId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emote_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_emote_show_in_post);
        Query query = fragmentEmotePostViewModel.getQuery(postId);
        FirestoreRecyclerOptions<EmoteModel> options = new FirestoreRecyclerOptions.Builder<EmoteModel>()
                .setQuery(query, EmoteModel.class)
                .build();
        adapter = new EmotePostAdapter(options, requireContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setNestedScrollingEnabled(true);
        adapter.startListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(adapter != null){
            adapter.stopListening();
        }
    }
}