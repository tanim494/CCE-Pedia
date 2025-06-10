package com.tanim.ccepedia;

public class Notice {
    private String id;
    private String text;
    private String link;

    public Notice() { }

    public Notice(String id, String text, String link) {
        this.id = id;
        this.text = text;
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) { this.id = id; }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
