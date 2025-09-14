package com.example.connection.model;

import java.util.List;

public class WidgetModel {
    private List<Image> list;
    private User user;
    private Image image;

    public WidgetModel() {
    }

    public WidgetModel(List<Image> list, User user, Image image) {
        this.list = list;
        this.user = user;
        this.image = image;
    }

    public List<Image> getList() {
        return list;
    }

    public void setList(List<Image> list) {
        this.list = list;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }
}
