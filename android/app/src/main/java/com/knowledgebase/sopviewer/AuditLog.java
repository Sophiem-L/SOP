package com.knowledgebase.sopviewer;

public class AuditLog {
    private String userName;
    private String userRole;
    private String timestamp;
    private String actionTitle;
    private String attachmentName;
    private int avatarRes;

    public AuditLog(String userName, String userRole, String timestamp, String actionTitle, String attachmentName,
            int avatarRes) {
        this.userName = userName;
        this.userRole = userRole;
        this.timestamp = timestamp;
        this.actionTitle = actionTitle;
        this.attachmentName = attachmentName;
        this.avatarRes = avatarRes;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getActionTitle() {
        return actionTitle;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public int getAvatarRes() {
        return avatarRes;
    }
}
