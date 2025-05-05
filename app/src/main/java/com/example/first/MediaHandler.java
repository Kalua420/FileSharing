package com.example.first;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class MediaHandler {
    String getMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            // Audio files
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";

            // Video files
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
            case "wmv":
                return "video/mp4";
        }
        return extension;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void playAudioWithExternalPlayer(Context context, Uri audioUri) {
        String mimeType = getMimeType(audioUri.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(audioUri, mimeType);
        Intent chooser = Intent.createChooser(intent, "Choose Media Player");
        context.startActivity(chooser);
    }
}
