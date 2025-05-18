package com.tanim.ccepedia;

public class FileItem {
    private String fileName;
    private String url;
    private String uploader;

    public FileItem() {} // Needed for Firestore

    public FileItem(String fileName, String url, String uploader) {
        this.fileName = fileName;
        this.url = url;
        this.uploader = uploader;
    }

    public String getFileName() { return fileName; }
    public String getUrl() { return url; }
    public String getUploader() { return uploader; }
}
