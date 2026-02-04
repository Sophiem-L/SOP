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

    public List<DocumentVersion> getVersions() {
        return versions;
    }
}
