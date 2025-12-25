package com.example.connection.ui.view.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.connection.R;
import com.example.connection.ui.adapter.CommentPostAdapter;
import com.example.connection.model.Comment;
import com.example.connection.ui.viewmodel.CommentViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.Query;

public class FragmentComment extends BottomSheetDialogFragment {

    private String postId;
    private CommentViewModel commentViewModel;
    private RecyclerView recyclerView;
    private EditText edt_comment;
    private ImageButton img_send;
    private CommentPostAdapter adapter;

    public FragmentComment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);
        if (getArguments() != null) {
            postId = getArguments().getString("postIdComment");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        Query query = commentViewModel.getQuery(postId);
        FirestoreRecyclerOptions<Comment> options = new FirestoreRecyclerOptions.Builder<Comment>()
                .setQuery(query, Comment.class)
                .build();
        adapter = new CommentPostAdapter(options, getContext(), commentId -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Xóa bình luận của bạn");
            builder.setMessage("Khi xóa bình luận bạn sẽ không thể khôi phục");
            builder.setPositiveButton("Xóa", (dialogInterface, i) -> commentViewModel.deleteComment(postId, commentId));
            builder.setNegativeButton("Hủy", null);
            AlertDialog dialog = builder.create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                dialog.show();
            } else {
                Toast.makeText(requireContext(), "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter.startListening();

        commentViewModel.getError().observe(getViewLifecycleOwner(), error ->
                Toast.makeText(getContext(),
                                error,
                                Toast.LENGTH_SHORT)
                        .show());

        img_send.setOnClickListener(view1 -> {
            String comment = edt_comment.getText().toString().trim();
            if (comment.isEmpty()) return;
            commentViewModel.setComment(postId, comment);
            edt_comment.setText("");
        });

    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_comment);
        edt_comment = view.findViewById(R.id.edt_comment);
        img_send = view.findViewById(R.id.btn_send_comment);
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
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}