package com.example.connection.ui.view.fragment;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.connection.R;
import com.example.connection.ui.viewmodel.ChangePasswordViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class FragmentChangePassword extends BottomSheetDialogFragment {

    private TextInputEditText tv_old_password, tv_new_password, tv_new_password_confirm;
    private Button bt_confirm;
    private ProgressBar progressBar;
    private ChangePasswordViewModel changePasswordViewModel;

    public FragmentChangePassword() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("cjeckk", "onCreate: ");
        changePasswordViewModel = new ViewModelProvider(this).get(ChangePasswordViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bt_confirm.setOnClickListener(view1 -> confirmToChange());

        changePasswordViewModel.getPasswordConfirmLiveData().observe(getViewLifecycleOwner(), newPasswordConfirm -> {
            if (newPasswordConfirm != null && !newPasswordConfirm.isEmpty()) {
                changePasswordViewModel.updatePassword(newPasswordConfirm);
            }
        });

        changePasswordViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            bt_confirm.setEnabled(true);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });

        changePasswordViewModel.getSuccess().observe(getViewLifecycleOwner(), newPassword -> {
            bt_confirm.setEnabled(true);
            Toast.makeText(getActivity(), "Cập nhật mật khẩu thành công", Toast.LENGTH_SHORT).show();
            tv_old_password.setText("");
            tv_new_password.setText("");
            tv_new_password_confirm.setText("");
            progressBar.setVisibility(View.GONE);
            if (newPassword != null && !newPassword.isEmpty()) {
                changePasswordViewModel.setPassword(getContext(), newPassword);
            }
        });

    }

    private void initView(View view) {
        tv_old_password = view.findViewById(R.id.edt_password_old);
        tv_new_password = view.findViewById(R.id.edt_password_new);
        tv_new_password_confirm = view.findViewById(R.id.edt_password_new_confirm);
        bt_confirm = view.findViewById(R.id.bt_confirm_change_password);
        progressBar = view.findViewById(R.id.progress_bar5);
    }

    private void confirmToChange() {
        bt_confirm.setEnabled(false);
        String oldPassword = Objects.requireNonNull(tv_old_password.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(tv_new_password.getText()).toString().trim();
        String newPasswordConfirm = Objects.requireNonNull(tv_new_password_confirm.getText()).toString().trim();
        progressBar.setVisibility(View.VISIBLE);
        changePasswordViewModel.checkPasswordConfirm(getContext(), oldPassword, newPassword, newPasswordConfirm);
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