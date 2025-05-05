package com.example.first;

public class LogEntry {
    private int senderId;
    private int receiverId;
    private String sourceMac;
    private String destinationMac;
    private String filename;
    private long fileSize; // 👈 New field for file size
    private java.sql.Timestamp timestamp;

    public void setLogId(int logId) {
    }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getSourceMac() { return sourceMac; }
    public void setSourceMac(String sourceMac) { this.sourceMac = sourceMac; }

    public String getDestinationMac() { return destinationMac; }
    public void setDestinationMac(String destinationMac) { this.destinationMac = destinationMac; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getFileSize() { return fileSize; } // 👈 Getter
    public void setFileSize(long fileSize) { this.fileSize = fileSize; } // 👈 Setter

    public java.sql.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(java.sql.Timestamp timestamp) { this.timestamp = timestamp; }
}
