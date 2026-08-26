package com.tanim.ccepedia;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class UserListModel {
    private String name;
    private String studentId;
    private String email;
    private String gender;
    private String phone;
    private String semester;
    private String role;
    private boolean verified;
    private Date lastLoggedIn;
    private String departmentName;
    private long viewCount;
    private String photoUrl;
    private String appVersion;

    public UserListModel() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @PropertyName("id")
    public String getStudentId() { return studentId; }
    @PropertyName("id")
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Date getLastLoggedIn() {
        return lastLoggedIn;
    }

    public void setLastLoggedIn(Date lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    @PropertyName("department")
    public String getDepartmentName() { return departmentName; }
    @PropertyName("department")
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
}