package com.example.first;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

public class MediaHandler {
    String getMimeType(String filePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension != null) {
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
            if (type != null) {
                return type;
            }
        }
        return "*/*";
    }
    public void playAudioWithExternalPlayer(Context context, Uri audioUri) {
        String mimeType = getMimeType(audioUri.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(audioUri, mimeType);
        Intent chooser = Intent.createChooser(intent, "Choose Media Player");
        context.startActivity(chooser);
    }
}
