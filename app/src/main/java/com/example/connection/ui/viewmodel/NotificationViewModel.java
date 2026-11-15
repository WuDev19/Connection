package com.example.connection.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.User;
import com.example.connection.repository.base.UserRepository;
import com.example.connection.repository.impl.UserRepositoryImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationViewModel extends ViewModel {
    private final UserRepository repository = new UserRepositoryImpl();
    private final String currentUserId = repository.getUserId();
    private final SavedStateHandle savedState;
    private final List<String> requests = new ArrayList<>();
    private final Set<String> pending = new HashSet<>();
    private final Set<String> loadingUsers = new HashSet<>();
    private final Map<String, User> users = new HashMap<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<String>> items = new MutableLiveData<>();
    private final MutableLiveData<String> emptyText = new MutableLiveData<>();
    private final MutableLiveData<Map<String, User>> userData = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> pendingData = new MutableLiveData<>();

    public NotificationViewModel(SavedStateHandle handle) {
        savedState = handle;
        setRequests(handle.get("confirm_friend"));
    }

    public void getFriendCurrentUser() {
        repository.getNotificationListenerRealtime(this::setRequests);
    }

    private void setRequests(List<String> ids) {
        requests.clear();
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !id.isEmpty() && !id.equals(currentUserId) && !requests.contains(id)) {
                    requests.add(id);
                }
            }
        }
        publish();
        for (String id : new ArrayList<>(requests)) loadUser(id);
    }

    private void loadUser(String id) {
        if (users.containsKey(id) || !loadingUsers.add(id)) return;
        repository.getUser(id).addOnCompleteListener(task -> {
            loadingUsers.remove(id);
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null) users.put(id, user);
                userData.setValue(new HashMap<>(users));
            }
        });
    }

    public LiveData<String> onAccept(String id, int ignoredPosition) {
        MutableLiveData<String> success = new MutableLiveData<>();
        if (!begin(id)) return success;
        repository.acceptFriend(id, currentUserId, callback(id, success));
        return success;
    }

    public void onCancel(String id) {
        if (!begin(id)) return;
        repository.declineFriend(currentUserId, id, callback(id, null));
    }

    private boolean begin(String id) {
        if (currentUserId == null || id == null || !requests.contains(id) || !pending.add(id)) return false;
        pendingData.setValue(new HashSet<>(pending));
        return true;
    }

    private UserRepository.HandleFriendNotification callback(String id, MutableLiveData<String> success) {
        return new UserRepository.HandleFriendNotification() {
            @Override public void onSuccess(String message) {
                pending.remove(id);
                requests.remove(id);
                pendingData.setValue(new HashSet<>(pending));
                publish();
                if (success != null) success.setValue(message);
            }
            @Override public void onFailure(String message) {
                pending.remove(id);
                pendingData.setValue(new HashSet<>(pending));
                error.setValue(message);
            }
        };
    }

    private void publish() {
        ArrayList<String> copy = new ArrayList<>(requests);
        savedState.set("confirm_friend", copy);
        items.setValue(copy);
        numberOfNotification();
    }

    public void numberOfNotification() {
        emptyText.setValue(requests.isEmpty() ? "Hiện không có thông báo" : "");
    }

    public LiveData<String> getError() { return error; }
    public LiveData<List<String>> getSetAdapter() { return items; }
    public LiveData<String> getSetNotification() { return emptyText; }
    public LiveData<Map<String, User>> getUsers() { return userData; }
    public LiveData<Set<String>> getPending() { return pendingData; }

    @Override protected void onCleared() {
        repository.unregisterListeningRealtime();
    }
}
