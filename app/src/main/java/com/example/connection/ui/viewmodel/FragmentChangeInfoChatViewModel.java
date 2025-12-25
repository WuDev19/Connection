package com.example.connection.ui.viewmodel;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.FragmentChangeInfoChatRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;
import java.util.Map;

public class FragmentChangeInfoChatViewModel extends ViewModel implements OnStateListener {
    private ChatRoom chatRoom;
    private final FragmentChangeInfoChatRepository fragmentChangeInfoChatRepository;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> notifyAlreadyExistInGroup = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> chatRoomListeningLiveData = new MutableLiveData<>();

    public FragmentChangeInfoChatViewModel(){
        fragmentChangeInfoChatRepository = new FragmentChangeInfoChatRepository(this);
    }

    public void setChatRoom(ChatRoom chatRoom){
        this.chatRoom = chatRoom;
    }

    public void updateChatNameGroup(String name){
        fragmentChangeInfoChatRepository.updateChatGroupName(chatRoom.getChatRoomId(), name).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                successLiveData.setValue("Cập nhật tên nhóm thành công");
            }
            else {
                Exception exception = task.getException();
                if(exception != null){
                    errorLiveData.setValue(exception.getMessage());
                }
            }
        });
    }

    public void updateChatRoomAvatar(Uri uri){
        fragmentChangeInfoChatRepository.upload_avt(uri, chatRoom);
    }

    public void addMemberToGroup(List<String> userIds, List<User> avtList){
        if(userIds.isEmpty()){
            notifyAlreadyExistInGroup.setValue("Hãy thêm ít nhất một người dùng");
            return;
        }
        boolean check = false;
        List<String> userId = chatRoom.getUserId();
        for(String id : userIds){
            if(!userId.contains(id)){
                userId.add(id);
                check = true;
            }
        }
        if(!check){
            StringBuilder thongBao = new StringBuilder("Người dùng ");
            for(int i = 0; i < avtList.size(); i++){
                if(i == avtList.size() - 1){
                    thongBao.append(avtList.get(i).getUserName()).append(" ");
                }
                else thongBao.append(avtList.get(i).getUserName()).append(", ");
            }
            notifyAlreadyExistInGroup.setValue(thongBao.append("đã ở trong nhóm").toString());
            return;
        }
        Map<String, Long> mapDelete = chatRoom.getDeleteChat();
        for(String id : userIds){
            if(!mapDelete.containsKey(id)){
                mapDelete.put(id, Timestamp.now().toDate().getTime());
            }
        }
        Map<String, String> avtMap = chatRoom.getAvtSender();
        for(User user : avtList){
            if(!avtMap.containsKey(user.getId())){
                avtMap.put(user.getId(), user.getAvatarUser());
            }
        }
        Map<String, String> aliasMap = chatRoom.getAlias();
        for(User user : avtList){
            if(!aliasMap.containsKey(user.getId())){
                aliasMap.put(user.getId(), user.getUserName());
            }
        }
        chatRoom.setUserId(userId);
        chatRoom.setAvtSender(avtMap);
        chatRoom.setDeleteChat(mapDelete);
        chatRoom.setAlias(aliasMap);
        fragmentChangeInfoChatRepository.addMemberToGroup(chatRoom).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                successLiveData.setValue("Thêm thành viên nhóm thành công");
            }
            else {
                Exception exception = task.getException();
                if(exception != null){
                    errorLiveData.setValue(exception.getMessage());
                }
            }
        });
    }

    public void leaveChat(){
        String currentId = FirebaseAuth.getInstance().getUid();
        if(currentId == null) return;
        int check = 0;
        List<String> userId = chatRoom.getUserId();
        Map<String, Long> mapDelete = chatRoom.getDeleteChat();
        Map<String, String> avtMap = chatRoom.getAvtSender();
        Map<String, String> aliasMap = chatRoom.getAlias();
        if(userId.remove(currentId)) {
            check++;
        }
        if (mapDelete.remove(currentId) != null) {
            check++;
        }
        if (avtMap.containsKey(currentId)) {
            avtMap.remove(currentId);
            check++;
        }
        if(aliasMap.remove(currentId) != null) {
            check++;
        }
        if(check == 4){
            chatRoom.setUserId(userId);
            chatRoom.setAvtSender(avtMap);
            chatRoom.setDeleteChat(mapDelete);
            chatRoom.setAlias(aliasMap);
            fragmentChangeInfoChatRepository.leaveChat(chatRoom).addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    successLiveData.setValue("Đã rời khỏi nhóm");
                }
                else{
                    Exception exception = task.getException();
                    if(exception != null){
                        errorLiveData.setValue(exception.getMessage());
                    }
                }
            });
        }
        else {
            errorLiveData.setValue("Lỗi không thể rời đoạn chat");
        }
    }

    public void getDelete(){
        List<String> userIds = chatRoom.getUserId();
        if(userIds == null) return;
        int count = 0;
        for(String id : userIds){
            if(id.contains("_delete")){
                count++;
            }
        }
        if ((count == (userIds.size() - 1)) || userIds.get(0).equals(userIds.get(1))) {
            fragmentChangeInfoChatRepository.deleteTwoSides(chatRoom);
        }
        else {
            fragmentChangeInfoChatRepository.deleteOneSide(chatRoom, userIds);
        }
    }

    public void listeningChatRoom(){
        fragmentChangeInfoChatRepository.listeningChatRoom(chatRoom, value -> {
            if(value != null) chatRoom = value;
            chatRoomListeningLiveData.setValue(value);
        });
    }

    public void unregisterListeningChatRoom(){
        fragmentChangeInfoChatRepository.unregisterListeningChatRoom();
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<List<User>> searchUser(String username){
        return fragmentChangeInfoChatRepository.searchUser(username);
    }

    public LiveData<String> getNotifyAlreadyExistInGroup() {
        return notifyAlreadyExistInGroup;
    }

    public LiveData<ChatRoom> getChatRoomListeningLiveData() {
        return chatRoomListeningLiveData;
    }

    @Override
    public void onSuccess(String success) {
        successLiveData.setValue(success);
    }

    @Override
    public void onFailure(String error) {
        errorLiveData.setValue(error);
    }

    @Override
    public void onLoading(boolean isLoading) {

    }

}
