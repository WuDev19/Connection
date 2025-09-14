package com.example.connection.model;

import java.util.List;

public class Music {
    private List<VideoItem> items;

    public Music() {
    }

    public Music(List<VideoItem> items) {
        this.items = items;
    }

    public List<VideoItem> getItems() {
        return items;
    }

    public static class VideoItem{
        private Snippet snippet;

        public VideoItem() {
        }

        public VideoItem(Snippet snippet) {
            this.snippet = snippet;
        }

        public Snippet getSnippet() {
            return snippet;
        }

        public static class Snippet{
            private String title;
            private Thumbnails thumbnails;
            private ResourceId resourceId;

            public Snippet() {
            }

            public Snippet(String title, Thumbnails thumbnails, ResourceId resourceId) {
                this.title = title;
                this.thumbnails = thumbnails;
                this.resourceId = resourceId;
            }

            public String getTitle() {
                return title;
            }

            public Thumbnails getThumbnails() {
                return thumbnails;
            }

            public ResourceId getResourceId() {
                return resourceId;
            }

            public static class Thumbnails{
                private Medium medium;

                public Thumbnails() {
                }

                public Thumbnails(Medium medium) {
                    this.medium = medium;
                }

                public Medium getMedium() {
                    return medium;
                }

                public static class Medium{
                    private String url;

                    public Medium() {
                    }

                    public Medium(String url) {
                        this.url = url;
                    }

                    public String getUrl() {
                        return url;
                    }
                }
            }

            public static class ResourceId{
                private String videoId;

                public ResourceId() {
                }

                public ResourceId(String videoId) {
                    this.videoId = videoId;
                }

                public String getVideoId() {
                    return videoId;
                }
            }
        }
    }
}
