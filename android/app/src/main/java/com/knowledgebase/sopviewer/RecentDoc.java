package com.knowledgebase.sopviewer;

public class RecentDoc {
    private int id;
    private String title;
    private String description;
    private String date;
    private int imageResId;
    private boolean isFavorite;
    private String fileUrl;
    private String fileType;
    private String category;
    private String version;
    private String status = ""; // "pending", "approved", "rejected", "" for articles/SOPs

    public RecentDoc(int id, String title, String description, String date, int imageResId,
                     boolean isFavorite, String fileUrl, String fileType, String category, String version) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageResId = imageResId;
        this.isFavorite = isFavorite;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.category = category;
        this.version = version;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public int getImageResId() { return imageResId; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public String getFileUrl() { return fileUrl; }
    public String getFileType() { return fileType; }
    public String getCategory() { return category; }
    public String getVersion() { return version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status != null ? status : ""; }
}
