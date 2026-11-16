package com.example.connection;

import com.example.connection.data.worker.WidgetContent;
import com.example.connection.model.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetContentTest {
    private Image post(String id, String author, List<String> seen) {
        Image image = new Image();
        image.setPostId(id);
        image.setPosterId(author);
        image.setSeen(seen);
        return image;
    }

    @Test public void emptyFeedClearsBadgeAndContent() {
        WidgetContent content = WidgetContent.from(Collections.emptyList(), "me");
        assertNull(content.latest);
        assertEquals("", content.badge());
        assertNull(content.mediaUrl());
    }

    @Test public void missingSeenMeansUnreadButOwnPostsAreExcluded() {
        Image latest = post("a", "friend", null);
        WidgetContent content = WidgetContent.from(Arrays.asList(latest,
                post("b", "friend", Collections.singletonList("me")), post("c", "me", null)), "me");
        assertSame(latest, content.latest);
        assertEquals(1, content.unread);
        assertEquals("1", content.badge());
    }

    @Test public void unreadBadgeSaturatesToAvoidUnnecessaryUpdates() {
        List<Image> posts = new ArrayList<>();
        for (int i = 0; i < 30; i++) posts.add(post("post" + i, "friend", null));
        WidgetContent content = WidgetContent.from(posts, "me");
        assertEquals(10, content.unread);
        assertEquals("9+", content.badge());
    }

    @Test public void likesDoNotTriggerWidgetUpdatesButCaptionAndSeenDo() {
        Image before = post("a", "friend", null);
        Image after = post("a", "friend", null);
        after.setNumberEmote(10);
        WidgetContent previous = WidgetContent.from(Collections.singletonList(before), "me");
        assertEquals(previous, WidgetContent.from(Collections.singletonList(after), "me"));
        after.setStatus("new caption");
        assertNotEquals(previous, WidgetContent.from(Collections.singletonList(after), "me"));
        after.setStatus(null);
        after.setSeen(Collections.singletonList("me"));
        assertNotEquals(previous, WidgetContent.from(Collections.singletonList(after), "me"));
    }

    @Test public void cloudinaryVideoUsesAnImageThumbnail() {
        assertEquals("https://res.cloudinary.com/demo/video/upload/so_0,f_jpg,w_480/v123/post/clip.jpg",
                WidgetContent.videoThumbnail("https://res.cloudinary.com/demo/video/upload/v123/post/clip.mp4"));
        assertNull(WidgetContent.videoThumbnail("https://other.example/clip.mp4"));
        assertNull(WidgetContent.videoThumbnail("not a url"));
        assertNull(WidgetContent.videoThumbnail(null));
    }

    @Test public void imageIsPreferredOverVideo() {
        Image image = post("a", "friend", null);
        image.setImageUrl("https://example.com/photo.jpg");
        image.setVideoUrl("https://res.cloudinary.com/demo/video/upload/clip.mp4");
        assertEquals(image.getImageUrl(), WidgetContent.from(Collections.singletonList(image), "me").mediaUrl());
    }

    @Test public void oldAccountsPushCannotRefreshNewAccountsWidget() {
        Map<String, String> event = new HashMap<>();
        event.put("type", "WIDGET_REFRESH");
        event.put("recipient_id", "account-a");
        assertTrue(WidgetContent.isRefreshFor(event, "account-a"));
        assertFalse(WidgetContent.isRefreshFor(event, "account-b"));
        assertFalse(WidgetContent.isRefreshFor(event, null));
        event.remove("recipient_id");
        assertFalse(WidgetContent.isRefreshFor(event, "account-a"));
    }

    @Test public void chatPushIsNotHandledAsWidgetRefresh() {
        Map<String, String> event = new HashMap<>();
        event.put("type", "CHAT");
        event.put("recipient_id", "me");
        assertFalse(WidgetContent.isRefreshFor(event, "me"));
        assertFalse(WidgetContent.isRefreshFor(null, "me"));
    }
}
