package com.tanim.ccepedia;

public class DriveLink {
    private String title;
    private String url;

    public DriveLink() {} // Needed for Firebase

    public DriveLink(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
}
