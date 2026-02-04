package com.knowledgebase.sopviewer;

public class Category {
    private int id;
    private String name;

    private int documents_count;
    private String updated_at;

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDocumentsCount() {
        return documents_count;
    }

    public String getUpdatedAt() {
        return updated_at;
    }

    @Override
    public String toString() {
        return name;
    }
}
