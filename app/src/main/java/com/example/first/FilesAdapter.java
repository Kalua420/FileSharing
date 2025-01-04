package com.example.first;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {
    private final List<FileModel> files;

    public FilesAdapter(List<FileModel> files) {
        this.files = files;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileModel file = files.get(position);
        holder.fileNameText.setText(file.getFileName());
        holder.filePathText.setText(file.getFilePath());
        String extension = file.getFilePath().substring(file.getFilePath().lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            // Image files
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                holder.fileIcon.setImageURI(Uri.parse(file.getFilePath()));
                break;

            // Document files
            case "pdf":
                holder.fileIcon.setImageResource(R.drawable.pdf);
                break;
            case "doc":
            case "docx":
                holder.fileIcon.setImageResource(R.drawable.txt);
                break;
            case "xls":
            case "xlsx":
                holder.fileIcon.setImageResource(R.drawable.xls);
                break;
            case "ppt":
            case "pptx":
                holder.fileIcon.setImageResource(R.drawable.ppt);
                break;

            // Text files
            case "txt":
            case "rtf":
            case "csv":
                holder.fileIcon.setImageResource(R.drawable.txt);
                break;

            // Audio files
            case "mp3":
            case "wav":
            case "ogg":
            case "m4a":
                holder.fileIcon.setImageResource(R.drawable.music);
                break;

            // Video files
            case "mp4":
            case "avi":
            case "mkv":
            case "mov":
            case "wmv":
                holder.fileIcon.setImageResource(R.drawable.video);
                break;

            // Archive files
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                holder.fileIcon.setImageResource(R.drawable.archive);
                break;

            // Code files
            case "java":
            case "kt":
            case "py":
            case "js":
            case "html":
            case "css":
            case "xml":
                holder.fileIcon.setImageResource(R.drawable.coding);
                break;

            // Default case for unknown file types
            default:
                holder.fileIcon.setImageResource(R.drawable.icon);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText;
        TextView filePathText;
        ImageView fileIcon;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            filePathText = itemView.findViewById(R.id.filePathText);
            fileIcon = itemView.findViewById(R.id.iconPic);
        }
    }
}