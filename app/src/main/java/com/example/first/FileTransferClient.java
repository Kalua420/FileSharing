package com.example.first;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DecimalFormat;

public class FileTransferClient {
    private static final String TAG = "FileTransferClient";
    private static final int BUFFER_SIZE = 65536;
    private static final int PORT = 5000;

    @SuppressLint("StaticFieldLeak")
    public void sendFile(final String serverIp, final String filePath) {
        new AsyncTask<Void, TransferProgress, Boolean>() {
            private long lastUpdateTime;
            private long lastBytesSent;

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

            @Override
            protected Boolean doInBackground(Void... params) {
                Socket socket = null;
                DataOutputStream dos = null;
                BufferedInputStream bis = null;
                DataInputStream dis = null;

                try {
                    File fileToSend = new File(filePath);
                    if (!fileToSend.exists()) {
                        Log.e(TAG, "File not found: " + filePath);
                        publishProgress(new TransferProgress(-1, 0, "File not found"));
                        return false;
                    }

                    // Connect to server
                    socket = new Socket(serverIp, PORT);
                    dos = new DataOutputStream(socket.getOutputStream());
                    bis = new BufferedInputStream(new FileInputStream(fileToSend));
                    dis = new DataInputStream(socket.getInputStream());

                    // Send file metadata
                    dos.writeUTF(fileToSend.getName());
                    dos.writeLong(fileToSend.length());
                    dos.flush();

                    // Send file content
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    long totalBytesSent = 0;
                    long fileSize = fileToSend.length();

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
                                    "Sending: " + fileToSend.getName()
                            ));

                            lastUpdateTime = currentTime;
                            lastBytesSent = totalBytesSent;
                        }
                    }

                    dos.flush();

                    // Wait for server confirmation
                    String response = dis.readUTF();
                    publishProgress(new TransferProgress(100, 0, response));
                    return response.contains("successfully");

                } catch (IOException e) {
                    Log.e(TAG, "File transfer error", e);
                    publishProgress(new TransferProgress(-1, 0, "Error: " + e.getMessage()));
                    return false;
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
            protected void onPostExecute(Boolean result) {
                // Reset speed display
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.speed != null) {
                        Connect.speed.setText("0 B/s");
                    }
                }

                // Update status
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (Connect.textViewContent != null) {
                        Connect.textViewContent.append(result ?
                                "File transfer completed successfully\n" :
                                "File transfer failed\n");
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
}