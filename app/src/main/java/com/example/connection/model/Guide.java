package com.example.connection.model;

public class Guide {

    private int res;
    private String guide;

    public Guide() {
    }

    public Guide(int res, String guide) {
        this.res = res;
        this.guide = guide;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public String getGuide() {
        return guide;
    }

    public void setGuide(String guide) {
        this.guide = guide;
    }
}
