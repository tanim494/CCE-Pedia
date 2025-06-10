package com.tanim.ccepedia;

public class FileItem {
    private String id;         // Firestore document ID
    private String fileName;
    private String url;
    private String uploader;

    public FileItem(String id, String fileName, String url, String uploader) {
        this.id = id;
        this.fileName = fileName;
        this.url = url;
        this.uploader = uploader;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileName() { return fileName; }
    public String getUrl() { return url; }
    public String getUploader() { return uploader; }
}
