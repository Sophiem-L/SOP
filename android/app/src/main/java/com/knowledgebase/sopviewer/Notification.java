package com.knowledgebase.sopviewer;

public class Notification {
    private String title;
    private String content;
    private String time;
    private String status; // "New" or "Urgent"

    public Notification(String title, String content, String time, String status) {
        this.title = title;
        this.content = content;
        this.time = time;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }
}
