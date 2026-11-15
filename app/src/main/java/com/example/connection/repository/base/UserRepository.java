package com.example.connection.repository.base;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.connection.model.User;
import com.example.connection.model.WidgetModel;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;

public interface UserRepository {

    // Interfaces
    interface OnShowUser {
        void showUser(User user);
    }

    interface OnReauthCallback {
        void onSuccess();
    }

    interface HandleFriendNotification {
        void onSuccess(String success);
        void onFailure(String error);
    }

    interface OnGetListFriendConfirm {
        void onGetListFriendConfirm(List<String> list);
    }

    interface OnCountMessage {
        void onCountMessage(HashMap<String, Integer> countMessage);
    }

    interface OnUpdateWidget {
        void onUpdateWidget(WidgetModel widgetModel);
    }

    String getCurrentUserId();
    String getUserId();
    FirebaseUser getUser();
    Task<DocumentSnapshot> getCurrentUser();
    Task<DocumentSnapshot> getUserFromFirebase();
    Task<DocumentSnapshot> getUser(String id);

    // ==================== Username & Profile info ====================
    Task<DocumentSnapshot> getCurrentUserName();
    void setUsername(User user, OnStateListener listener);
    void showUserFromFirebase(OnShowUser onShowUser);
    void unregisterUserListening();
    void signOut();
    void updateNewEmailToFireStore(Context context);
    void deleteUser();
    void deleteUser(OnStateListener listener);
    void upload_avt(Uri uri, String username, User user);
    void upload_avt(Uri uri, String username, User user, OnStateListener listener);
    void shutdownService();

    // ==================== Security: Password & Email change ====================
    void reauthenticate(FirebaseUser user, String password, OnReauthCallback callback);
    void verifyChange(FirebaseUser user, String newEmail);
    void verifyChange(FirebaseUser user, String newEmail, OnStateListener listener);
    void updatePassword(FirebaseUser user, String oldPassword, String newPasswordConfirm);
    void updatePassword(FirebaseUser user, String oldPassword, String newPasswordConfirm, OnStateListener listener);

    // ==================== Social & Friend management ====================
    void addFriend(String otherUserId, User currentUser);
    void addFriend(String otherUserId, User currentUser, OnStateListener listener);
    void sendFriendRequest(String otherUserId);
    void sendFriendRequest(String otherUserId, OnStateListener listener);
    void delete(String friendId, List<String> friendCurrentUser);
    void delete(String friendId, List<String> friendCurrentUser, OnStateListener listener);
    Query getQueryFriend();
    Query getQuerySearch(String username);
    void acceptFriend(String otherUserId, String currentUserId, HandleFriendNotification handleFriend);
    void declineFriend(String currentUserId, String otherUserId, HandleFriendNotification handleFriendNotifacation);

    // ==================== Main & Widget logic ====================
    void setFirstUse(Activity activity);
    LiveData<Boolean> getFirstUse(Context context);
    void checkActive();
    void setActive();
    void getNotificationListenerRealtime(OnGetListFriendConfirm onGetListFriendConfirm);
    void getRealtimeUnseenMessage(OnCountMessage onCountMessage);
    void unregisterListeningRealtime();
    void scheduleJob(Context context, OnUpdateWidget onUpdateWidget);
}
