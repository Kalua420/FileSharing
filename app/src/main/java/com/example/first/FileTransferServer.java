package com.example.first;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferServer extends Service {
    private static final String TAG = "FileTransferServer";
    private static final int PORT = 5000;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FileTransferChannel";
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private Thread serverThread;
    public String clientIp = "";
    private final IBinder binder = new LocalBinder();
    private static final String FileDir = "/storage/emulated/0/";
    private static final int BufferSize = 65536;

    // Transfer state management
    private final ConcurrentHashMap<String, TransferStatus> activeTransfers = new ConcurrentHashMap<>();

    // Transfer states
    public enum TransferState {
        RUNNING,
        PAUSED,
        CANCELLED
    }

    // Transfer status class to track state of each transfer
    public static class TransferStatus {
        private volatile TransferState state;
        private final String fileName;
        private final String transferId;
        private long totalBytes;
        private volatile long bytesTransferred;
        private volatile long lastResumePosition;

        public TransferStatus(String fileName) {
            this.fileName = fileName;
            this.transferId = UUID.randomUUID().toString();
            this.state = TransferState.RUNNING;
            this.bytesTransferred = 0;
            this.lastResumePosition = 0;
        }

        public TransferState getState() {
            return state;
        }

        public void setState(TransferState state) {
            this.state = state;
        }

        public String getFileName() {
            return fileName;
        }

        public String getTransferId() {
            return transferId;
        }

        public void setTotalBytes(long totalBytes) {
            this.totalBytes = totalBytes;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public void setBytesTransferred(long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
        }

        public long getBytesTransferred() {
            return bytesTransferred;
        }

        public void setLastResumePosition(long position) {
            this.lastResumePosition = position;
        }

        public long getLastResumePosition() {
            return lastResumePosition;
        }
    }

    public class LocalBinder extends Binder {
        FileTransferServer getService() {
            return FileTransferServer.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service with notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        if (!isRunning) {
            isRunning = true;
            startFileTransferServer();
        }
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "File Transfer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for file transfer operations");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("File Transfer Service")
                .setContentText("Service is running")
                .setSmallIcon(android.R.drawable.ic_menu_always_landscape_portrait)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopFileTransferServer();
    }

    private void startFileTransferServer() {
        updateUI("Server starting...");
        serverThread = new Thread(() -> {
            try {
                File uploadDir = new File(FileDir + "ShareGT");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        // Permission granted for Android 11 and above
                        createDirectory(uploadDir);
                    }
                }
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                serverSocket = new ServerSocket(PORT);
                updateUI("Server started on port " + PORT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Connect.myMacAddress = MacAddressUtil.getMacAddress();
                }
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientConnected("Client connected: " + clientSocket.getInetAddress());
                        clientIp = String.valueOf(clientSocket.getInetAddress());
                        Log.d("client ip : ", clientIp);
                        new FileTransferHandler(clientSocket, uploadDir.getAbsolutePath()).start();
                    } catch (IOException e) {
                        if (isRunning) {
                            updateUI("Error accepting client connection: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                updateUI("Error starting server: " + e.getMessage());
            }
        });
        serverThread.start();
    }

    private void stopFileTransferServer() {
        isRunning = false;
        // Cancel all active transfers
        for (TransferStatus status : activeTransfers.values()) {
            status.setState(TransferState.CANCELLED);
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing server socket", e);
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
        updateUI("Server stopped");
    }

    // API methods for controlling transfers
    public void pauseTransfer(String transferId) {
        TransferStatus status = activeTransfers.get(transferId);
        if (status != null && status.getState() == TransferState.RUNNING) {
            status.setState(TransferState.PAUSED);
            updateUI("Transfer paused: " + status.getFileName());
        }
    }

    public void resumeTransfer(String transferId) {
        TransferStatus status = activeTransfers.get(transferId);
        if (status != null && status.getState() == TransferState.PAUSED) {
            status.setState(TransferState.RUNNING);
            synchronized (status) {
                status.notifyAll(); // Wake up the waiting thread
            }
            updateUI("Transfer resumed: " + status.getFileName());
        }
    }

    public void cancelTransfer(String transferId) {
        TransferStatus status = activeTransfers.get(transferId);
        if (status != null) {
            status.setState(TransferState.CANCELLED);
            if (status.getState() == TransferState.PAUSED) {
                synchronized (status) {
                    status.notifyAll(); // Wake up the waiting thread if it's paused
                }
            }
            updateUI("Transfer cancelled: " + status.getFileName());
        }
    }

    // Get all active transfers for UI display
    public Map<String, TransferStatus> getActiveTransfers() {
        return new HashMap<>(activeTransfers);
    }

    private void updateUI(final String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.textViewContent != null) {
                runOnUiThread(() -> Connect.textViewContent.append(message + "\n"));
            }
        }
    }

    private void clientConnected(final String clientIp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Connect.clientConnected != null) {
                runOnUiThread(() -> Connect.clientConnected.setText(clientIp));
            }
        }
    }

    private void updateProgress(TransferStatus status) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.progressBar != null && Connect.currentTransferId != null &&
                    Connect.currentTransferId.equals(status.getTransferId())) {
                int progress = (int) ((status.getBytesTransferred() * 100) / status.getTotalBytes());
                runOnUiThread(() -> {
                    Connect.progressBar.setProgress(progress);
                    Connect.transferStatus.setText(getStatusText(status));
                });
            }
        }
    }

    private String getStatusText(TransferStatus status) {
        switch(status.getState()) {
            case RUNNING:
                return "Transferring...";
            case PAUSED:
                return "Paused";
            case CANCELLED:
                return "Cancelled";
            default:
                return "";
        }
    }

    private void updateSpeed(long bytesPerSecond, String transferId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.speed != null && Connect.currentTransferId != null &&
                    Connect.currentTransferId.equals(transferId)) {
                runOnUiThread(() -> {
                    String speedText = formatSpeed(bytesPerSecond);
                    Connect.speed.setText(speedText);
                });
            }
        }
    }

    private String formatSpeed(long bytesPerSecond) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (bytesPerSecond < 1024) {
            return df.format(bytesPerSecond) + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return df.format(bytesPerSecond / 1024.0) + " KB/s";
        } else {
            return df.format(bytesPerSecond / (1024.0 * 1024.0)) + " MB/s";
        }
    }

    private void runOnUiThread(Runnable runnable) {
        android.os.Handler handler = new android.os.Handler(getMainLooper());
        handler.post(runnable);
    }

    private class FileTransferHandler extends Thread {
        private final Socket clientSocket;
        private final String uploadDirectory;

        public FileTransferHandler(Socket socket, String uploadDirectory) {
            this.clientSocket = socket;
            this.uploadDirectory = uploadDirectory;
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            DataInputStream dis = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            DataOutputStream dos = null;
            TransferStatus status = null;

            try {
                dis = new DataInputStream(clientSocket.getInputStream());
                dos = new DataOutputStream(clientSocket.getOutputStream());

                String fileName = dis.readUTF();
                long fileSize = dis.readLong();

                // Create and register transfer status
                status = new TransferStatus(fileName);
                status.setTotalBytes(fileSize);
                activeTransfers.put(status.getTransferId(), status);

                // Update UI with new transfer info
                updateUI("Receiving file: " + fileName);

                // Update Connect class with current transfer ID for UI updates
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    TransferStatus finalStatus = status;
                    runOnUiThread(() -> {
                        Connect.currentTransferId = finalStatus.getTransferId();
                        Connect.fileName.setText(fileName);
                        Connect.transferStatus.setText("Transferring...");

                        // Update UI controls for this transfer
                        if (Connect.btnPauseResume != null) {
                            Connect.btnPauseResume.setEnabled(true);
                            Connect.btnPauseResume.setText("Pause");
                        }
                        if (Connect.btnCancel != null) {
                            Connect.btnCancel.setEnabled(true);
                        }
                    });
                }

                File outputFile = new File(uploadDirectory, fileName);

                // Check if we need to append to an existing file (for resume)
                boolean append = false;
                if (status.getLastResumePosition() > 0) {
                    append = true;
                }

                fos = new FileOutputStream(outputFile, append);
                bos = new BufferedOutputStream(fos);

                byte[] buffer = new byte[BufferSize];
                int bytesRead;
                long totalBytesRead = status.getLastResumePosition();
                long lastSpeedUpdateTime = System.currentTimeMillis();
                long bytesReadSinceLastUpdate = 0;

                status.setBytesTransferred(totalBytesRead);

                while (totalBytesRead < fileSize && status.getState() != TransferState.CANCELLED &&
                        (bytesRead = dis.read(buffer, 0,
                                (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {

                    // Check if transfer is paused
                    while (status.getState() == TransferState.PAUSED) {
                        try {
                            updateSpeed(0, status.getTransferId());
                            synchronized (status) {
                                status.wait(1000); // Wait with timeout for resume signal
                            }
                        } catch (InterruptedException e) {
                            // Interrupted
                            if (status.getState() == TransferState.CANCELLED) {
                                break;
                            }
                        }
                    }

                    // If cancelled during pause, exit loop
                    if (status.getState() == TransferState.CANCELLED) {
                        break;
                    }

                    // Continue with transfer
                    bos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    bytesReadSinceLastUpdate += bytesRead;

                    // Update status
                    status.setBytesTransferred(totalBytesRead);

                    // Update progress
                    updateProgress(status);

                    // Update speed every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpeedUpdateTime >= 1000) {
                        long bytesPerSecond = bytesReadSinceLastUpdate * 1000 / (currentTime - lastSpeedUpdateTime);
                        updateSpeed(bytesPerSecond, status.getTransferId());
                        lastSpeedUpdateTime = currentTime;
                        bytesReadSinceLastUpdate = 0;
                    }
                }

                bos.flush();

                if (status.getState() == TransferState.CANCELLED) {
                    updateUI("File transfer cancelled: " + fileName);
                    // Optionally delete partial file
                    // outputFile.delete();
                } else if (totalBytesRead >= fileSize) {
                    updateUI("File received: " + fileName);
                    updateProgress(status); // Ensure 100% is shown

                    // Send success confirmation to client
                    dos.writeUTF("File transferred successfully");
                } else {
                    // Save position for resuming
                    status.setLastResumePosition(totalBytesRead);
                    updateUI("File transfer incomplete: " + fileName);

                    // Send incomplete transfer status to client
                    dos.writeUTF("Transfer incomplete");
                    dos.writeLong(totalBytesRead); // Send current position for potential resume
                }

                updateSpeed(0, status.getTransferId());

                // Reset UI elements
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    TransferStatus finalStatus1 = status;
                    runOnUiThread(() -> {
                        if (Connect.currentTransferId != null &&
                                Connect.currentTransferId.equals(finalStatus1.getTransferId())) {
                            if (Connect.btnPauseResume != null) {
                                Connect.btnPauseResume.setEnabled(false);
                            }
                            if (Connect.btnCancel != null) {
                                Connect.btnCancel.setEnabled(false);
                            }
                        }
                    });
                }

                // Remove completed/cancelled transfer from active transfers
                if (status.getState() != TransferState.PAUSED) {
                    activeTransfers.remove(status.getTransferId());
                }

            } catch (IOException e) {
                if (status != null) {
                    if (status.getState() == TransferState.CANCELLED) {
                        updateUI("Transfer cancelled: " + status.getFileName());
                    } else {
                        updateUI("Error during file transfer: " + e.getMessage());
                        status.setState(TransferState.CANCELLED);
                    }
                    activeTransfers.remove(status.getTransferId());
                } else {
                    updateUI("Error during file transfer: " + e.getMessage());
                }
            } finally {
                try {
                    if (bos != null) bos.close();
                    if (fos != null) fos.close();
                    if (dis != null) dis.close();
                    if (dos != null) dos.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing resources", e);
                }
            }
        }
    }

    private void createDirectory(File directory) {
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (success) {
                Log.d("FileTransferServer", "Directory created: " + directory.getAbsolutePath());
            } else {
                Log.d("FileTransferServer", "Failed to create directory: " + directory.getAbsolutePath());
            }
        } else {
            Log.d("FileTransferServer", "Directory already exists: " + directory.getAbsolutePath());
        }
    }
}