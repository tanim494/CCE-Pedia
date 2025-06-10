package com.tanim.ccepedia;

public class DriveLinkItem {
    private String id;
    private String title;
    private String url;

    // ✅ Required by Firestore: no-argument constructor
    public DriveLinkItem() {
        // Needed for Firebase Firestore to deserialize documents
    }

    // Optional: Constructor for creating new objects manually
    public DriveLinkItem(String title, String url) {
        this.title = title;
        this.url = url;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
