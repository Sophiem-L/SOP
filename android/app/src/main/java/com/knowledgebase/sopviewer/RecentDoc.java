package com.knowledgebase.sopviewer;

public class RecentDoc {
    private int id;
    private String title;
    private String description;
    private String date;
    private int imageResId;
    private boolean isFavorite;

    public RecentDoc(int id, String title, String description, String date, int imageResId, boolean isFavorite) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.imageResId = imageResId;
        this.isFavorite = isFavorite;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public int getImageResId() {
        return imageResId;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
