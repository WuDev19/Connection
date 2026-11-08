package com.example.connection.repository.impl;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.constants.SharePreUtil;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.example.connection.model.WidgetModel;
import com.example.connection.repository.base.UserRepository;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.data.api.NotificationHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepositoryImpl implements UserRepository {

    private final String TAG = getClass().getName();
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final CollectionReference collectionUser;
    private final String currentUserId;
    private OnStateListener onStateListener;

    // Listeners
    private ListenerRegistration userListening;
    private ListenerRegistration chatroomListenerRealTime, notificationListenerRealtime;
    private final List<ListenerRegistration> messageListenerRealTime = new ArrayList<>();
    private final HashMap<String, Integer> countUnseenMessage = new HashMap<>();

    // Thread pool
    private final ExecutorService updateEmoteAndCommentService = Executors.newSingleThreadExecutor();

    // Constructors
    public UserRepositoryImpl() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.collectionUser = db.collection("users");
        this.currentUserId = mAuth.getUid();
    }

    public UserRepositoryImpl(OnStateListener onStateListener) {
        this();
        this.onStateListener = onStateListener;
    }

    @Override
    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    public String getUserId() {
        return currentUserId;
    }

    @Override
    public FirebaseUser getUser() {
        return mAuth.getCurrentUser();
    }

    @Override
    public Task<DocumentSnapshot> getCurrentUser() {
        if (currentUserId == null) return null;
        return collectionUser.document(currentUserId).get();
    }

    @Override
    public Task<DocumentSnapshot> getUserFromFirebase() {
        return getCurrentUser();
    }

    @Override
    public Task<DocumentSnapshot> getUser(String id) {
        if (id == null) return null;
        return collectionUser.document(id).get();
    }

    // ==================== Username & Profile info ====================

    @Override
    public Task<DocumentSnapshot> getCurrentUserName() {
        return getCurrentUser();
    }

    @Override
    public void setUsername(User user, OnStateListener listener) {
        if (currentUserId == null) return;
        collectionUser.document(currentUserId).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (listener != null) listener.onSuccess("setUserName");
            } else {
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    listener.onFailure(exception.getMessage());
                }
            }
        });
    }

    @Override
    public void showUserFromFirebase(OnShowUser onShowUser) {
        if (currentUserId == null || onShowUser == null) return;

        if (userListening != null) {
            userListening.remove();
        }
        userListening = collectionUser.document(currentUserId).addSnapshotListener((value, error) -> {
            if (value == null || error != null) return;
            User user = value.toObject(User.class);
            if (user != null) {
                onShowUser.showUser(user);
            }
        });
    }

    @Override
    public void unregisterUserListening() {
        if (userListening != null) {
            userListening.remove();
            userListening = null;
        }
    }

    @Override
    public void signOut() {
        mAuth.signOut();
    }

    @Override
    public void updateNewEmailToFireStore(Context context) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String newEmail = currentUser.getEmail();
        if (currentUserId == null || newEmail == null) return;
        collectionUser.document(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user1 = task.getResult().toObject(User.class);
                if (user1 == null) return;
                String oldEmail = user1.getEmail();
                if (oldEmail == null) return;
                if (!oldEmail.equals(newEmail)) {
                    collectionUser.document(currentUserId)
                            .update("email", newEmail)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.e(TAG, "Cập nhật email lên firestore thành công");
                                }
                            });
                }
            }
        });
    }

    @Override
    public void deleteUser() {
        deleteUser(this.onStateListener);
    }

    @Override
    public void deleteUser(OnStateListener listener) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || currentUserId == null) return;
        DocumentReference userReference = collectionUser.document(currentUserId);
        userReference.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User userFireStore = task.getResult().toObject(User.class);
                if (userFireStore != null) {
                    userReference.delete().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            user.delete().addOnCompleteListener(task11 -> {
                                if (task11.isSuccessful()) {
                                    if (listener != null) listener.onSuccess("Đã xóa tài khoản");
                                } else {
                                    userReference.set(userFireStore).addOnCompleteListener(task111 -> {
                                        if (task111.isSuccessful()) {
                                            Log.e(TAG, "onComplete: rollback thanh cong");
                                        } else {
                                            Exception exception = task111.getException();
                                            if (exception != null && listener != null) {
                                                listener.onFailure(exception.getMessage());
                                            }
                                        }
                                    });
                                    if (task11.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                        if (listener != null) listener.onFailure("Dang nhap lai");
                                    } else {
                                        Exception exception = task11.getException();
                                        if (exception != null && listener != null) {
                                            listener.onFailure(exception.getMessage());
                                        }
                                    }
                                }
                            });
                        } else {
                            Exception exception = task1.getException();
                            if (exception != null && listener != null) {
                                listener.onFailure(exception.getMessage());
                            }
                        }
                    });
                }
            } else {
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    listener.onFailure(exception.getMessage());
                }
            }
        });
    }

    @Override
    public void upload_avt(Uri uri, String username, User user) {
        upload_avt(uri, username, user, this.onStateListener);
    }

    @Override
    public void upload_avt(Uri uri, String username, User user, OnStateListener listener) {
        if (uri == null) {
            if (listener != null) listener.onLoading(true);
            if (!username.isEmpty()) {
                user.setUserName(username);
                user.setUserNameLowerCase(username.toLowerCase());
            }
            updateUserToFirebase(user, listener);
            return;
        }
        Log.e(TAG, "upload_avt: " + uri);
        MediaManager.get().upload(uri).unsigned("post_upload").option("folder", "avatar/").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                if (listener != null) listener.onLoading(true);
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                if (imageUrl != null) user.setAvatarUser(imageUrl);
                if (!username.isEmpty()) {
                    user.setUserName(username);
                    user.setUserNameLowerCase(username.toLowerCase());
                }
                updateUserToFirebase(user, listener);
            }

            @Override
            public void onError(String requestId, ErrorInfo errorInfo) {
                if (listener != null) listener.onFailure(errorInfo.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void updateUserToFirebase(User user, OnStateListener listener) {
        if (currentUserId == null) return;
        collectionUser.document(currentUserId)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateRelativeInfo(currentUserId, user);
                        updateEmoteAndComment(currentUserId, user);
                        if (listener != null) {
                            listener.onSuccess("Cập nhật thông tin thành công");
                            listener.onLoading(false);
                        }
                    } else {
                        Exception exception = task.getException();
                        if (exception != null && listener != null) {
                            listener.onFailure(exception.getMessage());
                        }
                    }
                });
    }

    private void updateRelativeInfo(String id, User user) {
        Task<QuerySnapshot> taskChat = db.collection("chatroom")
                .whereArrayContains("userId", id)
                .get();

        Task<QuerySnapshot> taskPost = db.collection("post_photo")
                .whereEqualTo("posterId", id)
                .get();

        Tasks.whenAllSuccess(taskChat, taskPost)
                .addOnSuccessListener(results -> {
                    WriteBatch batchChatAndPost = db.batch();

                    QuerySnapshot chatResult = (QuerySnapshot) results.get(0);
                    QuerySnapshot postResult = (QuerySnapshot) results.get(1);

                    for (DocumentSnapshot documentSnapshot : chatResult) {
                        Log.e(TAG, "updateRelativeInfo: chat");
                        ChatRoom room = documentSnapshot.toObject(ChatRoom.class);
                        if (room != null) {
                            Map<String, String> avtMap = room.getAvtSender();
                            if (avtMap == null) avtMap = new HashMap<>();
                            avtMap.put(user.getId(), user.getAvatarUser());
                            Map<String, String> aliasMap = room.getAlias();
                            if (aliasMap != null) {
                                String alias = aliasMap.get(user.getId());
                                if (alias != null) {
                                    if (alias.contains("_")) {
                                        alias = user.getUserName() + "_" + alias.split("_")[1];
                                    } else {
                                        alias = user.getUserName();
                                    }
                                    aliasMap.put(user.getId(), alias);
                                    batchChatAndPost.update(documentSnapshot.getReference(), "alias", aliasMap);
                                }
                            }
                            batchChatAndPost.update(documentSnapshot.getReference(), "avtSender", avtMap);
                        }
                    }
                    for (DocumentSnapshot documentSnapshot : postResult) {
                        Log.e(TAG, "updateRelativeInfo: post");
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image != null) {
                            batchChatAndPost.update(documentSnapshot.getReference(), "avatarPoster", user.getAvatarUser());
                            batchChatAndPost.update(documentSnapshot.getReference(), "userNamePoster", user.getUserName());
                        }
                    }
                    batchChatAndPost.commit().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Log.e(TAG, "Update avatar thành công cho cả chatroom và post_photo");
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    private void updateEmoteAndComment(String id, User user) {
        updateEmoteAndCommentService.execute(() -> db.collection("post_photo")
                .whereArrayContains("friendId", id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task<QuerySnapshot>> taskList = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        taskList.add(documentSnapshot.getReference().collection("emotes")
                                .whereEqualTo("idUser", id)
                                .get());
                        taskList.add(documentSnapshot.getReference().collection("comments")
                                .whereEqualTo("idUser", id)
                                .get());
                    }
                    if (taskList.isEmpty()) return;
                    Tasks.whenAllSuccess(taskList)
                            .addOnSuccessListener(objects1 -> {
                                WriteBatch batchEmoteAndComment = db.batch();
                                for (Object object : objects1) {
                                    if (object instanceof QuerySnapshot) {
                                        QuerySnapshot querySnapshot = (QuerySnapshot) object;
                                        for (DocumentSnapshot docSnap : querySnapshot) {
                                            if (docSnap.exists()) {
                                                String path = docSnap.getReference().getPath();
                                                Log.e(TAG, "onSuccess: " + path);
                                                if (path.contains("emotes")) {
                                                    batchEmoteAndComment.update(docSnap.getReference(), "urlAvt", user.getAvatarUser());
                                                    batchEmoteAndComment.update(docSnap.getReference(), "username", user.getUserName());
                                                } else if (path.contains("comments")) {
                                                    batchEmoteAndComment.update(docSnap.getReference(), "avtComment", user.getAvatarUser());
                                                    batchEmoteAndComment.update(docSnap.getReference(), "userNameComment", user.getUserName());
                                                }
                                            }
                                        }
                                    }
                                }
                                batchEmoteAndComment.commit().addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.e(TAG, "Update avatar thành công cho cả emote và comment");
                                    } else {
                                        Log.e(TAG, "updateRelativeInfo: " + Objects.requireNonNull(task1.getException()).getMessage());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage())));
    }

    @Override
    public void shutdownService() {
        Log.e(TAG, "shutdownService: ");
        updateEmoteAndCommentService.shutdown();
    }

    // ==================== Security: Password & Email change ====================

    @Override
    public void reauthenticate(FirebaseUser user, String password, OnReauthCallback callback) {
        if (user == null || user.getEmail() == null) {
            if (onStateListener != null) onStateListener.onFailure("Không tìm thấy thông tin tài khoản");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onSuccess();
            } else {
                if (onStateListener != null) onStateListener.onFailure("Mật khẩu không chính xác");
            }
        });
    }

    @Override
    public void verifyChange(FirebaseUser user, String newEmail) {
        verifyChange(user, newEmail, this.onStateListener);
    }

    @Override
    public void verifyChange(FirebaseUser user, String newEmail, OnStateListener listener) {
        if (user == null) return;
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (listener != null) listener.onSuccess("Vui lòng kiểm tra email và đăng nhập lại để xác nhận thay đổi");
            } else {
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    listener.onFailure(exception.getMessage());
                }
            }
        });
    }

    @Override
    public void updatePassword(FirebaseUser user, String oldPassword, String newPasswordConfirm) {
        updatePassword(user, oldPassword, newPasswordConfirm, this.onStateListener);
    }

    @Override
    public void updatePassword(FirebaseUser user, String oldPassword, String newPasswordConfirm, OnStateListener listener) {
        if (user == null || user.getEmail() == null) {
            if (listener != null) listener.onFailure("Không tìm thấy thông tin tài khoản");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                user.updatePassword(newPasswordConfirm).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (listener != null) listener.onSuccess(newPasswordConfirm);
                    } else {
                        Exception exception = task.getException();
                        if (exception != null && listener != null) {
                            listener.onFailure(exception.getMessage());
                        }
                    }
                });
            } else {
                if (listener != null) listener.onFailure("Mật khẩu cũ không chính xác");
            }
        });
    }

    // ==================== Social & Friend management (BottomSheet & Notification) ====================

    @Override
    public void addFriend(String otherUserId, User currentUser) {
        addFriend(otherUserId, currentUser, this.onStateListener);
    }

    @Override
    public void addFriend(String otherUserId, User currentUser, OnStateListener listener) {
        if (currentUserId == null || otherUserId == null) return;
        WriteBatch writeBatch = db.batch();
        writeBatch.set(collectionUser.document(currentUserId), currentUser);
        collectionUser.document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User otherUser = documentSnapshot.toObject(User.class);
                if (otherUser != null) {
                    List<String> friendOtherUser = otherUser.getFriend();
                    if (friendOtherUser == null) friendOtherUser = new ArrayList<>();
                    friendOtherUser.add(currentUserId);
                    writeBatch.update(collectionUser.document(otherUserId), "friend", friendOtherUser);
                    writeBatch.commit().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        } else {
                            if (listener != null) listener.onSuccess("Kết bạn thành công");
                        }
                    });
                }
            }
        });
    }

    @Override
    public void sendFriendRequest(String otherUserId) {
        sendFriendRequest(otherUserId, this.onStateListener);
    }

    @Override
    public void sendFriendRequest(String otherUserId, OnStateListener listener) {
        if (currentUserId == null || otherUserId == null) return;
        collectionUser.document(otherUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User otherUser = task.getResult().toObject(User.class);
                if (otherUser != null) {
                    List<String> friendConfirmOtherUser = otherUser.getFriendConfirm();
                    List<String> friendOtherUser = otherUser.getFriend();
                    if (friendOtherUser == null || friendConfirmOtherUser == null) {
                        Log.e(TAG, "sendFriendRequest: ko tim thay danh sach ban be cua otheruser");
                        return;
                    }
                    if (friendConfirmOtherUser.contains(currentUserId)) {
                        if (listener != null) listener.onSuccess("Bạn đã gửi kết bạn đến người dùng này");
                        return;
                    } else if (friendOtherUser.contains(currentUserId)) {
                        if (listener != null) listener.onSuccess("Hai người đã là bạn bè");
                        return;
                    }
                    friendConfirmOtherUser.add(currentUserId);
                    otherUser.setFriendConfirm(friendConfirmOtherUser);
                    collectionUser.document(otherUserId)
                            .set(otherUser).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    if (listener != null) listener.onSuccess("Đã gửi lời mời kết bạn thành công");
                                    Map<String, String> data = new HashMap<>();
                                    data.put("type", "FRIEND_REQUEST");
                                    collectionUser.document(currentUserId).get().addOnSuccessListener(currentSnap -> {
                                        String senderName = "Ai đó";
                                        if (currentSnap.exists()) {
                                            User cur = currentSnap.toObject(User.class);
                                            if (cur != null && cur.getUserName() != null) {
                                                senderName = cur.getUserName();
                                            }
                                        }
                                        NotificationHelper.sendNotificationToUser(otherUserId, "Lời mời kết bạn", senderName + " đã gửi cho bạn lời mời kết bạn", data);
                                    }).addOnFailureListener(e -> {
                                        NotificationHelper.sendNotificationToUser(otherUserId, "Lời mời kết bạn", "Bạn có lời mời kết bạn mới", data);
                                    });
                                } else {
                                    Exception exception = task1.getException();
                                    if (exception != null && listener != null) {
                                        listener.onFailure(exception.getMessage());
                                    }
                                }
                            });
                }
            } else {
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    listener.onFailure(exception.getMessage());
                }
            }
        });
    }

    @Override
    public void delete(String friendId, List<String> friendCurrentUser) {
        delete(friendId, friendCurrentUser, this.onStateListener);
    }

    @Override
    public void delete(String friendId, List<String> friendCurrentUser, OnStateListener listener) {
        if (friendId == null || currentUserId == null || friendCurrentUser == null) return;
        friendCurrentUser.remove(friendId);
        WriteBatch writeBatch = db.batch();
        writeBatch.update(collectionUser.document(currentUserId), "friend", friendCurrentUser);
        collectionUser.document(friendId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User userOther = task.getResult().toObject(User.class);
                if (userOther == null) return;
                List<String> listFriendOtherUser = userOther.getFriend();
                if (listFriendOtherUser != null) {
                    listFriendOtherUser.remove(currentUserId);
                    writeBatch.update(collectionUser.document(friendId), "friend", listFriendOtherUser);
                }
                writeBatch.commit().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        if (listener != null) listener.onSuccess("Xóa thành công");
                    } else {
                        Exception exception = task1.getException();
                        if (exception != null && listener != null) {
                            listener.onFailure(exception.getMessage());
                        }
                    }
                });
            }
        });
    }

    @Override
    public Query getQueryFriend() {
        if (currentUserId != null) {
            return collectionUser
                    .whereArrayContains("friend", currentUserId)
                    .orderBy("userName", Query.Direction.ASCENDING);
        }
        return null;
    }

    @Override
    public Query getQuerySearch(String username) {
        if (currentUserId != null) {
            return collectionUser
                    .whereGreaterThanOrEqualTo("userNameLowerCase", username.toLowerCase())
                    .whereLessThanOrEqualTo("userNameLowerCase", username.toLowerCase() + "\uf8ff")
                    .orderBy("userNameLowerCase");
        }
        return null;
    }

    @Override
    public void acceptFriend(String otherUserId, String currentUserId, User user, HandleFriendNotification handleFriend) {
        WriteBatch writeBatch = db.batch();
        writeBatch.set(collectionUser.document(currentUserId), user);
        collectionUser.document(otherUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User otherUser = task.getResult().toObject(User.class);
                if (otherUser == null) return;
                List<String> friend = otherUser.getFriend();
                if (friend == null) friend = new ArrayList<>();
                friend.add(currentUserId);
                otherUser.setFriend(friend);
                writeBatch.set(collectionUser.document(otherUserId), otherUser);
                writeBatch.commit().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Log.e(TAG, "onComplete: accept thanh cong");
                        if (handleFriend != null) handleFriend.onSuccess("Kết bạn thành công");
                        Map<String, String> data = new HashMap<>();
                        data.put("type", "FRIEND_ACCEPT");
                        String senderName = (user != null && user.getUserName() != null) ? user.getUserName() : "Ai đó";
                        NotificationHelper.sendNotificationToUser(otherUserId, "Kết bạn thành công", senderName + " đã đồng ý lời mời kết bạn của bạn", data);
                    } else {
                        Exception exception = task1.getException();
                        if (exception != null && handleFriend != null) {
                            handleFriend.onFailure(exception.getMessage());
                        }
                    }
                });
            } else {
                Exception exception = task.getException();
                if (exception != null && handleFriend != null) {
                    handleFriend.onFailure(exception.getMessage());
                }
            }
        });
    }

    @Override
    public void declineFriend(String currentUserId, User user, HandleFriendNotification handleFriendNotifacation) {
        collectionUser.document(currentUserId).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.e(TAG, user.toString());
            } else {
                Exception exception = task.getException();
                if (exception != null && handleFriendNotifacation != null) {
                    handleFriendNotifacation.onFailure(exception.getMessage());
                }
            }
        });
    }

    // ==================== Main & Widget logic (MainRepository) ====================

    @Override
    public void setFirstUse(Activity activity) {
        SharePreUtil.getInstance(activity.getApplicationContext()).setFirstUseApp();
    }

    @Override
    public LiveData<Boolean> getFirstUse(Context context) {
        MutableLiveData<Boolean> firstUse = new MutableLiveData<>();
        firstUse.setValue(SharePreUtil.getInstance(context.getApplicationContext()).firstUseApp());
        return firstUse;
    }

    @Override
    public void checkActive() {
        if (currentUserId == null) return;
        collectionUser.document(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user1 = task.getResult().toObject(User.class);
                if (user1 == null) return;
                if (!user1.isActive()) {
                    collectionUser.document(currentUserId)
                            .update("active", true).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.e(TAG, "Đang hoạt động");
                                } else {
                                    Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task1.getException()).getMessage()));
                                }
                            });
                }
            } else if (task.getException() != null) {
                Log.e(TAG, Objects.requireNonNull(task.getException().getMessage()));
            }
        });
    }

    @Override
    public void setActive() {
        if (currentUserId == null) return;
        collectionUser.document(currentUserId).update("active", false)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        Log.e(TAG, Objects.requireNonNull(task.getException().getMessage()));
                    }
                });
    }

    @Override
    public void getNotificationListenerRealtime(OnGetListFriendConfirm onGetListFriendConfirm) {
        if (currentUserId == null || onGetListFriendConfirm == null) return;
        if (notificationListenerRealtime != null) {
            notificationListenerRealtime.remove();
        }
        notificationListenerRealtime = collectionUser.document(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    User user = value.toObject(User.class);
                    if (user != null) {
                        onGetListFriendConfirm.onGetListFriendConfirm(user.getFriendConfirm());
                    }
                });
    }

    @Override
    public void getRealtimeUnseenMessage(OnCountMessage onCountMessage) {
        if (currentUserId == null || onCountMessage == null) return;

        if (chatroomListenerRealTime != null) {
            chatroomListenerRealTime.remove();
        }

        chatroomListenerRealTime = db.collection("chatroom")
                .whereArrayContains("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    countUnseenMessage.clear();
                    for (DocumentSnapshot documentSnapshot : value) {
                        String chatRoomId = documentSnapshot.getId();
                        ChatRoom room = documentSnapshot.toObject(ChatRoom.class);
                        long unread = 0;
                        if (room != null && room.getUnreadCounts() != null && room.getUnreadCounts().containsKey(currentUserId)) {
                            Long count = room.getUnreadCounts().get(currentUserId);
                            if (count != null) {
                                unread = count;
                            }
                        }
                        countUnseenMessage.put(chatRoomId, (int) unread);
                    }
                    onCountMessage.onCountMessage(countUnseenMessage);
                });
    }

    @Override
    public void unregisterListeningRealtime() {
        if (chatroomListenerRealTime != null) {
            chatroomListenerRealTime.remove();
            chatroomListenerRealTime = null;
        }
        for (ListenerRegistration listenerRegistration : messageListenerRealTime) {
            listenerRegistration.remove();
        }
        messageListenerRealTime.clear();
        countUnseenMessage.clear();
        if (notificationListenerRealtime != null) {
            notificationListenerRealtime.remove();
            notificationListenerRealtime = null;
        }
    }

    @Override
    public void scheduleJob(Context context, OnUpdateWidget onUpdateWidget) {
        if (!SharePreUtil.getInstance(context.getApplicationContext()).enableWidget()) return;
        Log.e(TAG, "scheduleJob: ");
        if (currentUserId == null || onUpdateWidget == null) return;
        db.collection("post_photo")
                .whereArrayContains("friendId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Image> list = new ArrayList<>();
                    Image imageFirst = null;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            Image image = doc.toObject(Image.class);
                            if (image != null) {
                                if (imageFirst == null) imageFirst = image;
                                List<String> seen = image.getSeen();
                                if (seen != null && !seen.contains(currentUserId)) {
                                    list.add(image);
                                }
                            }
                        }
                    }
                    if (imageFirst == null) {
                        Log.e(TAG, "Ko có dữ liệu");
                        return;
                    }
                    final Image finalImageFirst = imageFirst;
                    collectionUser.document(imageFirst.getPosterId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);
                                    if (user != null) {
                                        onUpdateWidget.onUpdateWidget(new WidgetModel(list, user, finalImageFirst));
                                    }
                                }
                            });
                });
    }
}
