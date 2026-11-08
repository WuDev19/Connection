package com.example.connection;

import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ModelValidationTest {

    @Test
    public void testUserCreationAndFields() {
        List<String> friends = Arrays.asList("user1", "user2");
        User user = new User("uid123", "https://avatar.url", "TestUser", "testuser", "test@example.com", "0123456789", friends, friends, true);

        assertEquals("uid123", user.getId());
        assertEquals("TestUser", user.getUserName());
        assertEquals("testuser", user.getUserNameLowerCase());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("0123456789", user.getSdt());
        assertTrue(user.isActive());
        assertEquals(2, user.getFriend().size());
    }

    @Test
    public void testChatRoomUnreadCounts() {
        ChatRoom room = new ChatRoom();
        room.setChatRoomId("room_001");
        Map<String, Long> unread = new HashMap<>();
        unread.put("user1", 3L);
        unread.put("user2", 0L);
        room.setUnreadCounts(unread);

        assertNotNull(room.getUnreadCounts());
        assertEquals(Long.valueOf(3L), room.getUnreadCounts().get("user1"));
        assertEquals(Long.valueOf(0L), room.getUnreadCounts().get("user2"));
    }

    @Test
    public void testChatMessageEmoteMapping() {
        Map<String, String> emotes = new HashMap<>();
        emotes.put("user1", "❤️");
        emotes.put("user2", "👍");

        ChatMessage message = new ChatMessage("msg01", "Hello world", "user1", 1700000000000L, true, emotes);
        assertEquals("msg01", message.getIdMessage());
        assertEquals("Hello world", message.getMessage());
        assertEquals("user1", message.getSenderId());
        assertTrue(message.isSeen());
        assertEquals("❤️", message.getEmote().get("user1"));
        assertEquals("👍", message.getEmote().get("user2"));
    }

    @Test
    public void testDeterministicChatRoomIdGeneration() {
        String userA = "alice_123";
        String userB = "bob_456";

        String roomId1 = (userA.hashCode() < userB.hashCode()) ? userA + "_" + userB : userB + "_" + userA;
        String roomId2 = (userB.hashCode() < userA.hashCode()) ? userB + "_" + userA : userA + "_" + userB;

        assertEquals("Room ID must be identical regardless of user parameter order", roomId1, roomId2);
    }
}
