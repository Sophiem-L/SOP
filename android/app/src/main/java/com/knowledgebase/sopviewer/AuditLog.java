package com.knowledgebase.sopviewer;

import com.google.gson.annotations.SerializedName;

public class AuditLog {

    @SerializedName("id")
    private int id;

    @SerializedName("action")
    private String action;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_role")
    private String userRole;

    @SerializedName("document_title")
    private String documentTitle;

    public AuditLog() {}

    public int getId() { return id; }
    public String getAction() { return action; }
    public String getCreatedAt() { return createdAt; }
    public String getUserName() { return userName; }
    public String getUserRole() { return userRole; }
    public String getDocumentTitle() { return documentTitle; }

    /** Human-readable action label shown in the UI. */
    public String getActionLabel() {
        if ("create".equals(action)) return "Created document";
        if ("update".equals(action)) return "Updated document";
        return action != null ? action : "";
    }
}
