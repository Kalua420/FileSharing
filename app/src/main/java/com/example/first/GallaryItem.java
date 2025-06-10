package com.example.first;

public class GallaryItem {
    private String name;
    private String path;
    private long size;
    private String type;
    private String mimeType;

    public GallaryItem() {}

    public GallaryItem(String name, String path, long size, String type, String mimeType) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.type = type;
        this.mimeType = mimeType;
    }

    // Getters
    public String getName() { return name; }
    public String getPath() { return path; }
    public long getSize() { return size; }
    public String getType() { return type; }
    public String getMimeType() { return mimeType; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setPath(String path) { this.path = path; }
    public void setSize(long size) { this.size = size; }
    public void setType(String type) { this.type = type; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}