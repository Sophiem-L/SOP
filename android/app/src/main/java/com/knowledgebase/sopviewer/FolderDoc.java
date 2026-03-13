package com.knowledgebase.sopviewer;

public class FolderDoc {
    private String title;
    private int docCount;
    private String lastEdited;
    private int colorResId;
    private int categoryId;

    public FolderDoc(String title, int docCount, String lastEdited, int colorResId, int categoryId) {
        this.title = title;
        this.docCount = docCount;
        this.lastEdited = lastEdited;
        this.colorResId = colorResId;
        this.categoryId = categoryId;
    }

    public String getTitle() { return title; }
    public int getDocCount() { return docCount; }
    public String getLastEdited() { return lastEdited; }
    public int getColorResId() { return colorResId; }
    public int getCategoryId() { return categoryId; }
}
