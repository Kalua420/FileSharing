package com.example.first;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class Files extends Fragment {
    private static final int PICK_FILES_REQUEST = 1;
    private FilesAdapter adapter;
    private ArrayList<FileModel> selectedFiles;
    private static final String TAG = "Files";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        selectedFiles = new ArrayList<>();

        // Initialize views
        RecyclerView selectedFilesRecyclerView = view.findViewById(R.id.selectedFilesRecyclerView);
        AppCompatButton selectFilesButton = view.findViewById(R.id.selectFilesButton);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) AppCompatButton sendButton = view.findViewById(R.id.send);

        // Setup RecyclerView
        selectedFilesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FilesAdapter(selectedFiles);
        selectedFilesRecyclerView.setAdapter(adapter);

        // Setup button click listener
        selectFilesButton.setOnClickListener(v -> openFileChooser());

        sendButton.setOnClickListener(v -> {
            // Check if there are any files selected before attempting to send
            if (selectedFiles.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one file first", Toast.LENGTH_SHORT).show();
                return;
            }

            String ip = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ip = Connect.ipToConnect;
            } else {
                Toast.makeText(getContext(), "Android version doesn't support this operation", Toast.LENGTH_SHORT).show();
                return;
            }

            // Start sending process
            sendFiles(ip, selectedFiles);
        });

        return view;
    }

    private void openFileChooser() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Files"), PICK_FILES_REQUEST);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "File selection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILES_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    selectedFiles.clear(); // Clear previous selections

                    if (data.getClipData() != null) {
                        // Multiple files selected
                        android.content.ClipData clipData = data.getClipData();
                        int count = clipData.getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            String fileName = getFileName(uri);
                            String path = getStoragePath(uri, fileName);

                            // Debug the file selection
                            Log.d(TAG, "Selected file: " + fileName + " with path: " + path);

                            if (path != null && !path.isEmpty()) {
                                selectedFiles.add(new FileModel(fileName, path));
                            } else {
                                // Use a default path if we couldn't determine one
                                String defaultPath = "/storage/emulated/0/Download/" + fileName;
                                selectedFiles.add(new FileModel(fileName, defaultPath));
                                Log.d(TAG, "Using default path: " + defaultPath);
                            }
                        }
                    } else if (data.getData() != null) {
                        // Single file selected
                        Uri uri = data.getData();
                        String fileName = getFileName(uri);
                        String path = getStoragePath(uri, fileName);

                        // Debug the file selection
                        Log.d(TAG, "Selected file: " + fileName + " with path: " + path);

                        if (path != null && !path.isEmpty()) {
                            selectedFiles.add(new FileModel(fileName, path));
                        } else {
                            // Use a default path if we couldn't determine one
                            String defaultPath = "/storage/emulated/0/Download/" + fileName;
                            selectedFiles.add(new FileModel(fileName, defaultPath));
                            Log.d(TAG, "Using default path: " + defaultPath);
                        }
                    }

                    adapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error processing files: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else if (resultCode != Activity.RESULT_CANCELED) {
                Toast.makeText(requireContext(), "File selection failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getStoragePath(Uri uri, String fileName) {
        if (uri == null) {
            return "";
        }

        String filePath = null;

        try {
            // First try to use the file's actual path if available
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                filePath = uri.getPath();
                if (filePath != null && !filePath.isEmpty()) {
                    Log.d(TAG, "File scheme path: " + filePath);
                    return filePath;
                }
            }

            // For content URIs, try to get a real path
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                // Try to get the path from MediaStore
                String[] projection = {MediaStore.MediaColumns.DATA};
                Cursor cursor = requireContext().getContentResolver().query(
                        uri, projection, null, null, null);

                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        filePath = cursor.getString(columnIndex);
                        Log.d(TAG, "MediaStore path: " + filePath);
                    }
                    cursor.close();
                }

                // If we still don't have a path, copy the file to local storage
                if (filePath == null || filePath.isEmpty()) {
                    filePath = copyFileToLocalStorage(uri, fileName);
                }
            }

            // If all else fails, use a default path
            if (filePath == null || filePath.isEmpty()) {
                filePath = "/storage/emulated/0/Download/" + fileName;
                Log.d(TAG, "Using default download path: " + filePath);
            }

            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            // Use the downloads folder as a fallback
            String fallbackPath = "/storage/emulated/0/Download/" + fileName;
            Log.d(TAG, "Exception occurred, using fallback path: " + fallbackPath);
            return fallbackPath;
        }
    }

    private String copyFileToLocalStorage(Uri uri, String fileName) {
        try {
            // Create a directory in internal storage
            File directory = new File(Environment.getExternalStorageDirectory() + "/Download");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Create a new file in the directory
            File destinationFile = new File(directory, fileName);
            String destinationPath = destinationFile.getAbsolutePath();

            // Copy the content from the URI to the new file
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                FileOutputStream outputStream = new FileOutputStream(destinationFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                Log.d(TAG, "File copied to: " + destinationPath);
                return destinationPath;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error copying file: " + e.getMessage());
        }
        return null;
    }

    private String getFileName(Uri uri) {
        String fileName = "";
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex != -1) {
                            fileName = cursor.getString(columnIndex);
                        }
                    }
                }
            }
            if (fileName.isEmpty()) {
                fileName = uri.getLastPathSegment();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public void sendFiles(String ip, ArrayList<FileModel> files) {
        if (ip == null || ip.isEmpty()) {
            Toast.makeText(requireContext(), "Error: IP address is null or empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use a separate thread for network operations
        new Thread(() -> {
            try {
                FileTransferClient fileTransferClient = new FileTransferClient();
                for (FileModel file : files) {
                    try {
                        final String filePath = file.getFilePath();
                        final String fileName = file.getFileName();

                        if (filePath == null || filePath.isEmpty()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Invalid file path for: " + fileName, Toast.LENGTH_SHORT).show()
                            );
                            continue;
                        }

                        // Show starting file transfer
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Starting transfer: " + fileName, Toast.LENGTH_SHORT).show()
                        );

                        Log.d(TAG, "Sending file: " + fileName + " from path: " + filePath);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            // Call sendFile method - it's void, so we can't check its return value
                            fileTransferClient.sendFile(ip, filePath, 1, "", "", Connect.serverMac);

                            // Since we can't check success directly, we assume it worked if no exception was thrown
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "File sent: " + fileName, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        final String errorMsg = e.getMessage();
                        final String fileName = file.getFileName();
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Error sending " + fileName + ": " + errorMsg, Toast.LENGTH_SHORT).show()
                        );
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Transfer error: " + errorMsg, Toast.LENGTH_SHORT).show()
                );
                e.printStackTrace();
            }
        }).start();
    }
}