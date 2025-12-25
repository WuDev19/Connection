package com.example.connection.ui.view.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.ui.viewmodel.InfoViewModel;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class FragmentInfo extends BottomSheetDialogFragment {

    private ImageView img_choose_photo, img_avatar_update;
    private TextInputEditText edt_username;
    private Button btn_confirm_update;
    private Uri uri;
    private ActivityResultLauncher<Intent> resultLauncher;
    private final String TAG = "FragmentInfo";
    private ProgressBar progressBar;
    private InfoViewModel infoViewModel;
    private String userName;

    public FragmentInfo() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        infoViewModel = new ViewModelProvider(this).get(InfoViewModel.class);
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null && intent.getData() != null) {
                    uri = intent.getData();
                    img_avatar_update.setImageURI(uri);
                }
            }
        });
        infoViewModel.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        onClick();

        infoViewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                userName = user.getUserName();
                edt_username.setText(user.getUserName());
                edt_username.setSelection(Objects.requireNonNull(edt_username.getText()).length());
                Glide.with(requireContext())
                        .load(user.getAvatarUser())
                        .error(R.drawable.icon_avatar)
                        .into(img_avatar_update);
            }
        });

        infoViewModel.getSuccess().observe(getViewLifecycleOwner(), success -> {
            Toast.makeText(FragmentInfo.this.getContext(), success, Toast.LENGTH_SHORT).show();
            edt_username.setText("");
            uri = null;
        });

        infoViewModel.getError().observe(getViewLifecycleOwner(), error -> Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());

        infoViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

    }

    private void initView(View view) {
        img_choose_photo = view.findViewById(R.id.img_choose_photo);
        edt_username = view.findViewById(R.id.edt_username);
        btn_confirm_update = view.findViewById(R.id.btn_confirm_update);
        img_avatar_update = view.findViewById(R.id.img_avatar_update);
        progressBar = view.findViewById(R.id.progress_bar7);
    }

    private void onClick() {
        img_choose_photo.setOnClickListener(view -> ImagePicker.with(FragmentInfo.this)
                .compress(512)
                .galleryOnly()
                .cropSquare()
                .maxResultSize(512, 512)
                .createIntent(intent -> {
                    resultLauncher.launch(intent);
                    return null;
                }));

        btn_confirm_update.setOnClickListener(view -> {
            String username = Objects.requireNonNull(edt_username.getText()).toString().trim();
            if ((username.equals(userName) && uri == null) || (username.isEmpty() && uri == null)) {
                Toast.makeText(getContext(), "Vui lòng nhập ít nhất một thông tin", Toast.LENGTH_LONG).show();
                return;
            }
            Log.e(TAG, "onClick: " + uri);
            infoViewModel.upload_avt(uri, username);
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