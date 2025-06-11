package com.example.first;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import java.io.File;

public class MediaHandler {

    /**
     * Get MIME type for any file extension
     */
    public String getMimeType(String filePath) {
        String extension = "";
        int lastDot = filePath.lastIndexOf(".");
        if (lastDot > 0) {
            extension = filePath.substring(lastDot + 1).toLowerCase();
        }

        switch (extension) {
            // Audio files
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "flac":
                return "audio/flac";
            case "aac":
                return "audio/aac";
            case "m4a":
                return "audio/mp4";
            case "wma":
                return "audio/x-ms-wma";

            // Video files
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mkv":
                return "video/x-matroska";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "webm":
                return "video/webm";
            case "3gp":
                return "video/3gpp";

            // Image files
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";

            // Document files
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "odt":
                return "application/vnd.oasis.opendocument.text";
            case "ods":
                return "application/vnd.oasis.opendocument.spreadsheet";
            case "odp":
                return "application/vnd.oasis.opendocument.presentation";

            // Text files
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "xml":
                return "text/xml";
            case "css":
                return "text/css";
            case "js":
                return "text/javascript";
            case "json":
                return "application/json";
            case "csv":
                return "text/csv";
            case "rtf":
                return "application/rtf";

            // Archive files
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "tar":
                return "application/x-tar";
            case "gz":
                return "application/gzip";

            // APK and executable files
            case "apk":
                return "application/vnd.android.package-archive";
            case "exe":
                return "application/x-msdownload";

            // Other common formats
            case "ttf":
                return "font/ttf";
            case "otf":
                return "font/otf";
            case "epub":
                return "application/epub+zip";
            case "mobi":
                return "application/x-mobipocket-ebook";

            default:
                // Try to get MIME type from system
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                return mimeType != null ? mimeType : "application/octet-stream";
        }
    }

    /**
     * Open any file with appropriate application
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openFile(Context context, Uri fileUri) {
        try {
            String mimeType = getMimeType(fileUri.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(intent, "Open with");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooser);
            } else {
                Toast.makeText(context, "No app found to open this file type", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open file from file path
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openFile(Context context, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri;
            // Use FileProvider for Android 7.0 and above
            fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);

            openFile(context, fileUri);
        } catch (Exception e) {
            Toast.makeText(context, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Specific method for media files (audio/video)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void playMedia(Context context, Uri mediaUri) {
        openFile(context, mediaUri);
    }

    /**
     * Specific method for document files
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openDocument(Context context, Uri documentUri) {
        openFile(context, documentUri);
    }

    /**
     * Specific method for text files
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openTextFile(Context context, Uri textUri) {
        openFile(context, textUri);
    }

    /**
     * Specific method for archive files (ZIP, RAR, etc.)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openArchive(Context context, Uri archiveUri) {
        openFile(context, archiveUri);
    }

    /**
     * Specific method for image files
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void openImage(Context context, Uri imageUri) {
        openFile(context, imageUri);
    }

    /**
     * Check if file type is supported
     */
    public boolean isFileSupported(String filePath) {
        String mimeType = getMimeType(filePath);
        return !mimeType.equals("application/octet-stream");
    }

    /**
     * Get file type category
     */
    public String getFileCategory(String filePath) {
        String mimeType = getMimeType(filePath);

        if (mimeType.startsWith("audio/")) {
            return "Audio";
        } else if (mimeType.startsWith("video/")) {
            return "Video";
        } else if (mimeType.startsWith("image/")) {
            return "Image";
        } else if (mimeType.startsWith("text/") ||
                mimeType.equals("application/pdf") ||
                mimeType.contains("document") ||
                mimeType.contains("spreadsheet") ||
                mimeType.contains("presentation")) {
            return "Document";
        } else if (mimeType.equals("application/zip") ||
                mimeType.contains("compressed") ||
                mimeType.contains("archive")) {
            return "Archive";
        } else {
            return "Other";
        }
    }
}