package com.example.connection.repository;

import android.util.Log;
import com.example.connection.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignUpRepository {

    public interface OnSignUpListener{
        void onSuccess(boolean success);
        void onEmailAndPassword(String emailAndPassword);
    }

    public void signUp(String email, String passwordConfirm, String username, OnSignUpListener onSignUpListener){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(email, passwordConfirm)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        String uID = mAuth.getUid();
                        if(uID == null) {
                            onSignUpListener.onSuccess(false);
                            return;
                        }
                        List<String> mList = new ArrayList<>();
                        mList.add(uID);
                        User user = new User(uID, null, username, email, null, mList, mList, false);
                        FirebaseFirestore.getInstance().collection("users").document(Objects.requireNonNull(uID)).set(user).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.e("SignUp", "Save successful");
                                onSignUpListener.onSuccess(true);
                                onSignUpListener.onEmailAndPassword(email + "_" + passwordConfirm);
                            } else {
                                FirebaseUser user1 = mAuth.getCurrentUser();
                                if(user1 != null){
                                    user1.delete()
                                            .addOnCompleteListener(deleteTask -> {
                                                if (!deleteTask.isSuccessful()) {
                                                    Log.e("SignUp", "Rollback Auth fail", deleteTask.getException());
                                                }
                                                onSignUpListener.onSuccess(false);
                                            });
                                }
                            }
                        });
                    }
                    else{
                        onSignUpListener.onSuccess(false);
                    }
                });
    }

}
