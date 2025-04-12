package com.example.first;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

    private final Context context;
    private final ArrayList<LogEntry> logsList;
    private final SimpleDateFormat dateFormat;
    private final int userId = LogsActivity.userId; // To determine if user is sender or receiver

    public LogsAdapter(Context context, ArrayList<LogEntry> logsList) {
        this.context = context;
        this.logsList = logsList;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry logEntry = logsList.get(position);

        // Determine log type based on sender/receiver relationship
        String logType = determineLogType(logEntry);
        holder.tvLogType.setText(logType);
        setLogTypeBackground(holder.tvLogType, logType);

        // Format and set timestamp
        String formattedDate = dateFormat.format(logEntry.getTimestamp());
        holder.tvTimestamp.setText(formattedDate);
        // Construct log message
        String logMessage = constructLogMessage(logEntry);
        holder.tvLogMessage.setText(logMessage);
        // Set device information if available
        String deviceInfo = constructDeviceInfo(logEntry);
        if (!deviceInfo.isEmpty()) {
            holder.tvDeviceInfo.setText(deviceInfo);
            holder.tvDeviceInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvDeviceInfo.setVisibility(View.GONE);
        }
    }

    private String determineLogType(LogEntry logEntry) {
        // Determine if user is sender or receiver
        if (logEntry.getSenderId() == userId) {
            return "SENT";
        } else if (logEntry.getReceiverId() == userId) {
            return "RECEIVED";
        } else {
            return "INFO"; // Default case
        }
    }

    private String constructLogMessage(LogEntry logEntry) {
        StringBuilder message = new StringBuilder();

        if (logEntry.getFilename() != null && !logEntry.getFilename().isEmpty()) {
            message.append("File: ").append(logEntry.getFilename());
        } else {
            message.append("File transfer record");
        }

        // Add sender/receiver information
        String fileSize = formatFileSize(logEntry.getFileSize());
        message.append("\nFile Size : ").append(fileSize);

        return message.toString();
    }

    private String constructDeviceInfo(LogEntry logEntry) {
        String logType = determineLogType(logEntry);
        StringBuilder deviceInfo = new StringBuilder();

        if (logType.equals("SENT")) {
            if (logEntry.getDestinationMac() != null && !logEntry.getDestinationMac().isEmpty()) {
                deviceInfo.append("To: ").append(logEntry.getDestinationMac());
            }
        } else if (logType.equals("RECEIVED")) {
            if (logEntry.getSourceMac() != null && !logEntry.getSourceMac().isEmpty()) {
                deviceInfo.append("From: ").append(logEntry.getSourceMac());
            }
        } else { // INFO or default
            if (logEntry.getSourceMac() != null && !logEntry.getSourceMac().isEmpty()) {
                deviceInfo.append("From: ").append(logEntry.getSourceMac());
            }
            if (logEntry.getDestinationMac() != null && !logEntry.getDestinationMac().isEmpty()) {
                if (deviceInfo.length() > 0) deviceInfo.append(" | ");
                deviceInfo.append("To: ").append(logEntry.getDestinationMac());
            }
        }

        return deviceInfo.toString();
    }

    private void setLogTypeBackground(TextView textView, String logType) {
        int backgroundColor;

        switch (logType.toUpperCase()) {
            case "SENT":
                backgroundColor = Color.parseColor("#FF5722"); // Orange/Red
                break;
            case "RECEIVED":
                backgroundColor = Color.parseColor("#3F51B5"); // Indigo
                break;
            case "ERROR":
                backgroundColor = Color.parseColor("#F44336"); // Red
                break;
            case "WARNING":
                backgroundColor = Color.parseColor("#FF9800"); // Orange
                break;
            case "INFO":
                backgroundColor = Color.parseColor("#9C27B0"); // Purple
                break;
            default:
                backgroundColor = Color.parseColor("#607D8B"); // Blue Grey
                break;
        }

        textView.getBackground().setTint(backgroundColor);
    }

    @Override
    public int getItemCount() {
        return logsList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvLogType, tvTimestamp, tvLogMessage, tvDeviceInfo;
        ConstraintLayout bg;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogType = itemView.findViewById(R.id.tvLogType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvLogMessage = itemView.findViewById(R.id.tvLogMessage);
            tvDeviceInfo = itemView.findViewById(R.id.tvDeviceInfo);
            bg = itemView.findViewById(R.id.bg);
        }
    }
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.2f %s", sizeInBytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}