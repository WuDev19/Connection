package com.example.connection.model;

public class ChoiceEditGroupModel {
    private int res;
    private String nameChoice;
    private String tag;

    public ChoiceEditGroupModel() {
    }

    public ChoiceEditGroupModel(int res, String nameChoice, String tag) {
        this.res = res;
        this.nameChoice = nameChoice;
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getRes() {
        return res;
    }

    public void setRes(int res) {
        this.res = res;
    }

    public String getNameChoice() {
        return nameChoice;
    }

    public void setNameChoice(String nameChoice) {
        this.nameChoice = nameChoice;
    }

}
