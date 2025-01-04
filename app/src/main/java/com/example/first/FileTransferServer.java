package com.example.first;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Objects;

public class FileTransferServer extends Service {
    private static final String TAG = "FileTransferServer";
    private static final int PORT = 5000;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "FileTransferChannel";
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private String serverIp = "";
    public String clientIp = "";
    private final IBinder binder = new LocalBinder();
    private static final String FileDir = "/storage/emulated/0/";

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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
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
//                File uploadDir = new File(getExternalFilesDir(null), "server_uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                serverSocket = new ServerSocket(PORT);
                updateUI("Server started on port " + PORT);
                while (isRunning) {
                    try {
                        serverIp = getLocalIpAddress();
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
    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Filter for IPv4 addresses
                    if (Objects.requireNonNull(addr.getHostAddress()).indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting IP address", e);
        }
        return "0.0.0.0"; // fallback
    }

    private void updateUI(final String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Connect.textViewContent != null) {
                runOnUiThread(() -> Connect.textViewContent.append(message + "\n"));
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
    public String getClientIp() {
        return clientIp;
    }
}