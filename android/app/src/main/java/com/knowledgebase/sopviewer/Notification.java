package com.knowledgebase.sopviewer;

/**
 * Maps the JSON response from GET /api/notifications.
 *
 * JSON shape:
 * {
 *   "id": 1,
 *   "title": "Document Pending Review",
 *   "message": "Alice submitted \"SOP\" for approval.",
 *   "type": "document_review",
 *   "document_id": 5,        // nullable
 *   "created_at": "2026-02-27T10:00:00.000000Z",
 *   "pivot": { "is_read": false }
 * }
 */
public class Notification {
    private int id;
    private String title;
    private String message;
    private String type;
    private Integer document_id;   // nullable
    private String created_at;
    private Pivot pivot;

    // ── Gson-friendly no-arg constructor ──────────────────────────────────
    public Notification() {}

    // ── Getters ───────────────────────────────────────────────────────────
    public int getId()            { return id; }
    public String getTitle()      { return title != null ? title : ""; }
    public String getMessage()    { return message != null ? message : ""; }
    public String getType()       { return type != null ? type : "info"; }
    public Integer getDocumentId(){ return document_id; }
    public String getCreatedAt()  { return created_at != null ? created_at : ""; }
    public boolean isRead()       { return pivot != null && pivot.is_read; }

    // ── Adapter-facing helpers ─────────────────────────────────────────────
    /** Content text shown in the notification card body. */
    public String getContent()    { return getMessage(); }

    /** Human-readable date (first 10 chars of ISO timestamp). */
    public String getTime() {
        if (created_at != null && created_at.length() >= 10) {
            return created_at.substring(0, 10);
        }
        return "";
    }

    /** Badge label: "New" for unread document-review alerts, blank otherwise. */
    public String getStatus() {
        if (!isRead() && "document_review".equals(type)) return "Review";
        if (!isRead()) return "New";
        return "";
    }

    // ── Pivot sub-object ──────────────────────────────────────────────────
    public static class Pivot {
        public boolean is_read;
    }
}
