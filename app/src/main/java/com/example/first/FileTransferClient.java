package com.example.first;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.text.DecimalFormat;

/** @noinspection ALL*/
public class FileTransferClient {
    private static final String TAG = "FileTransferClient";
    private static final int BUFFER_SIZE = 65535;
    private static final int PORT = 5000;
    private DatabaseHelper db = new DatabaseHelper();
    private static String senderMac = "";
    private static long fileSize;

    @SuppressLint("StaticFieldLeak")
    public void sendFile(final String serverIp, final String filePath,
                         final int userId, final String sender, final String receiver,
                         final String destinationMac) {
        if (Connect.myMacAddress.isEmpty()) {
            senderMac = MacAddressUtil.getMacAddress();
        }else senderMac = Connect.myMacAddress;
        new AsyncTask<Void, TransferProgress, TransferResult>() {
            private long lastUpdateTime;
            private long lastBytesSent;
            private String fileName;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                lastUpdateTime = System.currentTimeMillis();
                lastBytesSent = 0;

                // Reset progress UI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.progressBar != null) {
                        Connect.progressBar.setProgress(0);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.speed != null) {
                        Connect.speed.setText("0 B/s");
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.textViewContent != null) {
                        Connect.textViewContent.append("Starting file transfer...\n");
                    }
                }
            }

            @SuppressLint("NewApi")
            @Override
            protected TransferResult doInBackground(Void... params) {
                Socket socket = null;
                DataOutputStream dos = null;
                BufferedInputStream bis = null;
                DataInputStream dis = null;

                try {
                    File fileToSend = new File(filePath);
                    fileName = fileToSend.getName();

                    if (!fileToSend.exists()) {
                        Log.e(TAG, "File not found: " + filePath);
                        publishProgress(new TransferProgress(-1, 0, "File not found"));
                        return new TransferResult(false, "File not found");
                    }

                    // Connect to server
                    socket = new Socket(serverIp, PORT);
                    dos = new DataOutputStream(socket.getOutputStream());
                    bis = new BufferedInputStream(Files.newInputStream(fileToSend.toPath()));
                    dis = new DataInputStream(socket.getInputStream());

                    // Send file metadata
                    dos.writeUTF(fileName);
                    dos.writeLong(fileToSend.length());
                    dos.flush();

                    // Send file content
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesSent = 0;
                    fileSize = fileToSend.length();

                    while ((bytesRead = bis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                        totalBytesSent += bytesRead;

                        // Calculate progress and speed
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdateTime >= 1000) {
                            long timeElapsed = currentTime - lastUpdateTime;
                            long bytesSentSinceLastUpdate = totalBytesSent - lastBytesSent;
                            long speed = (bytesSentSinceLastUpdate * 1000) / timeElapsed;

                            int progress = (int)((totalBytesSent * 100) / fileSize);
                            publishProgress(new TransferProgress(
                                    progress,
                                    speed,
                                    "Sending: " + fileName
                            ));
                            lastUpdateTime = currentTime;
                            lastBytesSent = totalBytesSent;
                        }
                    }

                    dos.flush();

                    // Wait for server confirmation
                    String response = dis.readUTF();
                    publishProgress(new TransferProgress(100, 0, response));

                    return new TransferResult(response.contains("successfully"), response);

                } catch (IOException e) {
                    Log.e(TAG, "File transfer error", e);
                    String errorMessage = "Error: " + e.getMessage();
                    publishProgress(new TransferProgress(-1, 0, errorMessage));
                    return new TransferResult(false, errorMessage);
                } finally {
                    // Close all streams
                    try {
                        if (bis != null) bis.close();
                        if (dos != null) dos.close();
                        if (dis != null) dis.close();
                        if (socket != null) socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing streams", e);
                    }
                }
            }

            @Override
            protected void onProgressUpdate(TransferProgress... values) {
                if (values.length > 0) {
                    TransferProgress progress = values[0];

                    // Update progress bar
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (Connect.progressBar != null && progress.progress >= 0) {
                            Connect.progressBar.setProgress(progress.progress);
                        }
                    }

                    // Update speed display
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (Connect.speed != null) {
                            Connect.speed.setText(formatSpeed(progress.speed));
                        }
                    }

                    // Update status text
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        if (Connect.textViewContent != null && progress.message != null) {
                            Connect.textViewContent.append(progress.message + "\n");
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(TransferResult result) {
                // Reset speed display
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.speed != null) {
                        Connect.speed.setText("0 B/s");
                    }
                }

                // Update status
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.textViewContent != null) {
                        Connect.textViewContent.append(result.success ?
                                "File transfer completed successfully\n" :
                                "File transfer failed: " + result.message + "\n");
                    }
                }

                // Insert log only if transfer was successful
                String receiveMac = Connect.serverMac;
                if (result.success) {
                    db.insertLog(senderMac, receiveMac, fileName, fileSize, new DatabaseHelper.DatabaseCallback() {
                                @Override
                                public void onResult(boolean success, String message, int userId) {
                                    if (!success) {
                                        Log.e(TAG, "Failed to log file transfer: " + message);
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            if (Connect.textViewContent != null) {
                                                Connect.textViewContent.append("Failed to log transfer: " + message + "\n");
                                            }
                                        }
                                    }
                                }
                            });
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
        }.execute();
    }


    private static class TransferProgress {
        final int progress;
        final long speed;
        final String message;

        TransferProgress(int progress, long speed, String message) {
            this.progress = progress;
            this.speed = speed;
            this.message = message;
        }
    }

    private static class TransferResult {
        final boolean success;
        final String message;

        TransferResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}