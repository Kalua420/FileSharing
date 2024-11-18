package com.example.first;

import android.graphics.Bitmap;

public class ImageItem {
    private final Bitmap image;
    private final String title;

    public ImageItem(Bitmap image, String title) {
        this.image = image;
        this.title = title;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }
}

