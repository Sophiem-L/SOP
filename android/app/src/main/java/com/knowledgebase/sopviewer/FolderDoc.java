package com.knowledgebase.sopviewer;

public class FolderDoc {
    private String title;
    private int docCount;
    private String lastEdited;
    private int colorResId;

    public FolderDoc(String title, int docCount, String lastEdited, int colorResId) {
        this.title = title;
        this.docCount = docCount;
        this.lastEdited = lastEdited;
        this.colorResId = colorResId;
    }

    public String getTitle() {
        return title;
    }

    public int getDocCount() {
        return docCount;
    }

    public String getLastEdited() {
        return lastEdited;
    }

    public int getColorResId() {
        return colorResId;
    }
}
