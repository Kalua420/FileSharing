package com.example.first;

public class VideoModel {
    private final String title;
    private final String path;
    private final String thumbnail;

    public VideoModel(String title, String path, String secondValue) {
        this.title = title;
        this.path = path;
        this.thumbnail = secondValue;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public String getPath(){
        return path;
    }

    public String getThumbnail() {
        return thumbnail;
    }

}
