package com.knowledgebase.sopviewer;

import java.util.List;

public class Document {
    private int id;
    private String title;
    private Category category;
    private List<DocumentVersion> versions;

    private String description;
    private String updated_at;
    private int is_favorite;
    private String status; // "pending", "approved", "rejected"

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUpdatedAt() {
        return updated_at;
    }

    public Category getCategory() {
        return category;
    }

    public void setVersions(List<DocumentVersion> versions) {
        this.versions = versions;
    }

    public int getIsFavorite() {
        return is_favorite;
    }

    public void setIsFavorite(int is_favorite) {
        this.is_favorite = is_favorite;
    }

    // Add a method to handle string conversion from JSON
    public void setIsFavoriteFromString(String is_favorite_str) {
        this.is_favorite = is_favorite_str != null && !is_favorite_str.equals("0") ? 1 : 0;
    }

    public List<DocumentVersion> getVersions() {
        return versions;
    }

    public String getStatus() {
        return status != null ? status : "pending";
    }
}
