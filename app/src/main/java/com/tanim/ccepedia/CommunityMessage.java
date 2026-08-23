package com.tanim.ccepedia;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class CommunityMessage {
    private String userEmail;
    private String userStudentId;
    private String userName;
    private String messageText;
    private Date timestamp;
    private String userDepartment;
    // Student IDs of mentioned users (drives the unread-mention cue query).
    private List<String> mentions;
    // Display names of mentioned users (drives @Name highlight rendering).
    private List<String> mentionNames;
    // Denormalized snapshot of the message this one replies to. Stored on the reply itself so the
    // quote survives deletion of the original and needs no extra fetch. Null on non-reply messages.
    private String replyToName;
    private String replyToText;
    private String replyToStudentId;
    // Shared-resource attachment. All null on ordinary messages (and on docs written before this
    // feature), so the chat renders them unchanged. attachmentType is "pdf" or "link"; the URL is a
    // Firebase Storage download URL (pdf) or a web URL (link); the title is shown on the chat card.
    private String attachmentType;
    private String attachmentUrl;
    private String attachmentTitle;

    public CommunityMessage() {
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserStudentId() {
        return userStudentId;
    }

    public String getUserName() {
        return userName;
    }

    public String getMessageText() {
        return messageText;
    }

    @ServerTimestamp
    public Date getTimestamp() { return timestamp; }

    public void setUserStudentId(String userStudentId) {
        this.userStudentId = userStudentId;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getUserDepartment() {
        return userDepartment;
    }

    public void setUserDepartment(String userDepartment) {
        this.userDepartment = userDepartment;
    }

    public List<String> getMentions() {
        return mentions;
    }

    public void setMentions(List<String> mentions) {
        this.mentions = mentions;
    }

    public List<String> getMentionNames() {
        return mentionNames;
    }

    public void setMentionNames(List<String> mentionNames) {
        this.mentionNames = mentionNames;
    }

    public String getReplyToName() {
        return replyToName;
    }

    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public String getReplyToStudentId() {
        return replyToStudentId;
    }

    public void setReplyToStudentId(String replyToStudentId) {
        this.replyToStudentId = replyToStudentId;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentTitle() {
        return attachmentTitle;
    }

    public void setAttachmentTitle(String attachmentTitle) {
        this.attachmentTitle = attachmentTitle;
    }
}
