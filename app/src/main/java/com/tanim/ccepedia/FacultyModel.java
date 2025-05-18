package com.tanim.ccepedia;

public class FacultyModel {
    private String name;
    private String designation;
    private String phone;
    private String imageUrl;

    public FacultyModel() {
        // Needed for Firestore deserialization
    }

    public FacultyModel(String name, String designation, String phone, String imageUrl) {
        this.name = name;
        this.designation = designation;
        this.phone = phone;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public String getDesignation() { return designation; }
    public String getPhone() { return phone; }
    public String getImageUrl() { return imageUrl; }
}
