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
import java.util.Locale;

/** @noinspection CallToPrintStackTrace*/
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
    private static final String FILE_NAME = "/storage/emulated/0/ShareGT/";

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
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientConnected("Client connected: " + clientSocket.getInetAddress());
                        clientIp = String.valueOf(clientSocket.getInetAddress());
                        Log.d("client ip : ",clientIp);
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

    private void updateUI(final String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.textViewContent != null) {
                runOnUiThread(() -> Connect.textViewContent.append(message + "\n"));
            }
        }
    }

    private void updateLogs(final String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String logMessage = "[ "+timestamp+" ]\n" + message;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.textViewContent != null) {
                runOnUiThread(() -> createAndWriteFile(logMessage+"\n"));
            }
        }
    }

    private void clientConnected(final String clientIp){
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            if (Connect.clientConnected!=null){
                runOnUiThread(()->Connect.clientConnected.setText(clientIp));
            }
        }
    }

    private void updateProgress(int progress) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.progressBar != null) {
                runOnUiThread(() -> Connect.progressBar.setProgress(progress));
            }
        }
    }

    private void updateSpeed(long bytesPerSecond) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.speed != null) {
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
            try {
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                updateUI("Receiving file: " + fileName);
                File outputFile = new File(uploadDirectory, fileName);
                FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                long lastSpeedUpdateTime = System.currentTimeMillis();
                long bytesReadSinceLastUpdate = 0;

                while (totalBytesRead < fileSize &&
                        (bytesRead = dis.read(buffer, 0,
                                (int)Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    bytesReadSinceLastUpdate += bytesRead;

                    // Update progress
                    int progress = (int)((totalBytesRead * 100) / fileSize);
                    updateProgress(progress);

                    // Update speed every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSpeedUpdateTime >= 1000) {
                        long bytesPerSecond = bytesReadSinceLastUpdate * 1000 / (currentTime - lastSpeedUpdateTime);
                        updateSpeed(bytesPerSecond);
                        lastSpeedUpdateTime = currentTime;
                        bytesReadSinceLastUpdate = 0;
                    }
                }

                bos.flush();
                bos.close();
                fos.close();

                updateUI("File received: " + fileName);
                updateLogs("File Received : "+fileName+"\nLocation : "+FILE_NAME+fileName);
                updateProgress(100);
                updateSpeed(0);

                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                dos.writeUTF("File transferred successfully");
                clientSocket.close();
            } catch (IOException e) {
                updateUI("Error during file transfer: " + e.getMessage());
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

    private void createAndWriteFile(String content) {
        try {
            File file = new File(FILE_NAME, ".logs.txt");
            if (!file.exists()) {
                boolean created = file.createNewFile();
                if (created) {
                    // Write content to the file
                    FileWriter writer = new FileWriter(file);
                    writer.write(content);
                    writer.close();
                }
            } else {
                // If file exists, append new content
                FileWriter writer = new FileWriter(file, true); // true for append mode
                writer.write("\n" + content);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}