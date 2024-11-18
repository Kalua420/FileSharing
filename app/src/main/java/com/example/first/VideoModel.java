package com.example.first;

public class VideoModel {
    private String title;
    private String path;
    private String thumbnail;

    public VideoModel(String title, String path, String secondValue) {
        this.title = title;
        this.path = path;
        this.thumbnail = secondValue;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getPath(){
        return path;
    }
    public void setPath(){
        this.path = path;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
