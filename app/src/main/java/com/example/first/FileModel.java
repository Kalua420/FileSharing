package com.example.first;

import androidx.annotation.NonNull;

public class FileModel {
    private final String fileName;
    private final String filePath;

    public FileModel(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    @NonNull
    @Override
    public String toString() {
        return filePath;
    }
}