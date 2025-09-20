package com.example.connection.view.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.example.connection.R;
import com.example.connection.view.activity.authentication.LoginActivity;
import com.example.connection.viewmodel.ChangeEmailViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class FragmentChangeEmail extends BottomSheetDialogFragment {

    private TextInputEditText edt_email_change, edt_password_confirm;
    private Button btn_confirm;
    private ChangeEmailViewModel changeEmailViewModel;

    public FragmentChangeEmail() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changeEmailViewModel = new ViewModelProvider(this).get(ChangeEmailViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_email, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btn_confirm.setOnClickListener(view1 -> onClickChange());

        changeEmailViewModel.getCheckPassWordLiveData().observe(getViewLifecycleOwner(), newEmail -> {
            if (newEmail != null && !newEmail.isEmpty()) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(FragmentChangeEmail.this.getActivity())
                        .setTitle("Connection")
                        .setMessage("Bạn chắc chắn muốn thay đổi?")
                        .setPositiveButton("Có", (dialogInterface, i) -> changeEmailViewModel.verifyChange(newEmail))
                        .setNegativeButton("Không", (dialogInterface, i) -> dialogInterface.cancel());
                dialog.create().show();
            }
        });

        changeEmailViewModel.getError().observe(getViewLifecycleOwner(), error -> Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show());

        changeEmailViewModel.getVerifySuccess().observe(getViewLifecycleOwner(), success -> {
            Toast.makeText(FragmentChangeEmail.this.getActivity(), success, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(FragmentChangeEmail.this.requireActivity(), LoginActivity.class);
            FragmentChangeEmail.this.startActivity(intent);
            requireActivity().finishAffinity();
        });

    }

    private void initView(View view) {
        edt_email_change = view.findViewById(R.id.edt_email_change);
        edt_password_confirm = view.findViewById(R.id.edt_password_confirm1);
        btn_confirm = view.findViewById(R.id.btn_confirm_update_email);
    }

    private void onClickChange() {
        String newEmail = Objects.requireNonNull(edt_email_change.getText()).toString().trim();
        String password = Objects.requireNonNull(edt_password_confirm.getText()).toString().trim();
        changeEmailViewModel.checkPassword(newEmail, password, getContext());
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