package com.example.connection.data.worker;

import com.example.connection.model.Image;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Values that affect the widget; likes/comments do not trigger redundant image downloads. */
public final class WidgetContent {
    public final Image latest;
    public final int unread;

    private WidgetContent(Image latest, int unread) {
        this.latest = latest;
        this.unread = unread;
    }

    public static WidgetContent from(List<Image> newestFirst, String uid) {
        Image latest = null;
        int unread = 0;
        if (newestFirst != null) {
            for (Image image : newestFirst) {
                if (image == null) continue;
                if (latest == null) latest = image;
                if (uid != null && !uid.equals(image.getPosterId())
                        && (image.getSeen() == null || !image.getSeen().contains(uid))) {
                    unread = Math.min(10, unread + 1);
                }
            }
        }
        return new WidgetContent(latest, unread);
    }

    public String badge() { return unread == 0 ? "" : unread > 9 ? "9+" : String.valueOf(unread); }

    public static boolean isRefreshFor(java.util.Map<String, String> data, String uid) {
        return data != null && uid != null && !uid.isEmpty() && "WIDGET_REFRESH".equals(data.get("type"))
                && uid.equals(data.get("recipient_id"));
    }

    public String mediaUrl() {
        if (latest == null) return null;
        if (latest.getImageUrl() != null && !latest.getImageUrl().isEmpty()) return latest.getImageUrl();
        return videoThumbnail(latest.getVideoUrl());
    }

    public static String videoThumbnail(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            URI uri = new URI(url);
            if (!"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost())) return null;
            String path = uri.getPath();
            if (path == null || !path.contains("/video/upload/")) return null;
            path = path.replace("/video/upload/", "/video/upload/so_0,f_jpg,w_480/");
            int extension = path.lastIndexOf('.');
            if (extension > path.lastIndexOf('/')) path = path.substring(0, extension);
            return new URI("https", uri.getAuthority(), path + ".jpg", uri.getQuery(), null).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof WidgetContent)) return false;
        WidgetContent that = (WidgetContent) other;
        return unread == that.unread && Objects.equals(key(), that.key());
    }

    private List<String> key() {
        return latest == null ? java.util.Collections.emptyList() : java.util.Arrays.asList(
                latest.getPostId(), latest.getImageUrl(), latest.getVideoUrl(), latest.getStatus(),
                latest.getPosterId(), latest.getAvatarPoster(), latest.getUserNamePoster());
    }

    @Override public int hashCode() { return Objects.hash(unread, key()); }
}
