package com.example.first;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gallary extends AppCompatActivity implements GallaryAdapter.OnSelectionModeChangeListener {

    private static final String TAG = "Gallary";

    private RecyclerView recyclerView;
    private GallaryAdapter gallaryAdapter;
    private List<GallaryItem> gallaryItemList;
    private final String folderPath = Environment.getExternalStorageDirectory() + "/ShareGT";

    private boolean isSelectionMode = false;
    private TextView selectionCounter;
    private FloatingActionButton fabSend;
    private MenuItem selectAllMenuItem;
    private MenuItem cancelSelectionMenuItem;
    private MenuItem startSelectionMenuItem;

    // Executor for background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gallary);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        executorService = Executors.newFixedThreadPool(2);
        initializeViews();
        setupToolbar();
        loadFiles();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        gallaryItemList = new ArrayList<>();

        gallaryAdapter = new GallaryAdapter(gallaryItemList, this::onFileClick, this::onFileLongClick);
        // Set the selection mode change listener
        gallaryAdapter.setSelectionModeChangeListener(this);
        recyclerView.setAdapter(gallaryAdapter);

        // Initialize selection counter and send FAB
        selectionCounter = findViewById(R.id.selectionCounter);
        fabSend = findViewById(R.id.fabSend);

        if (fabSend != null) {
            fabSend.setOnClickListener(v -> sendSelectedFiles());
            fabSend.hide();
        }
    }

    private void setupToolbar() {Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle("Gallery");
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery_menu, menu);
        selectAllMenuItem = menu.findItem(R.id.action_select_all);
        cancelSelectionMenuItem = menu.findItem(R.id.action_cancel_selection);
        startSelectionMenuItem = menu.findItem(R.id.action_start_selection);
        updateMenuVisibility();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_select_all) {
            toggleSelectAll();
            return true;
        } else if (id == R.id.action_cancel_selection) {
            exitSelectionMode();
            return true;
        } else if (id == R.id.action_start_selection) {
            enterSelectionMode();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadFiles() {
        // Load files in background thread
        executorService.execute(() -> {
            try {
                File folder = new File(folderPath);
                List<GallaryItem> tempList = new ArrayList<>();

                if (!folder.exists()) {
                    // Try to create the directory
                    if (!folder.mkdirs()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Cannot create folder: " + folderPath, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        });
                        return;
                    }
                }

                if (!folder.isDirectory()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Path is not a directory: " + folderPath, Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    });
                    return;
                }

                File[] files = folder.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.canRead()) {
                            try {
                                GallaryItem gallaryItem = new GallaryItem();
                                gallaryItem.setName(file.getName());
                                gallaryItem.setPath(file.getAbsolutePath());
                                gallaryItem.setSize(file.length());
                                gallaryItem.setType(getFileType(file));
                                gallaryItem.setMimeType(getMimeType(file.getAbsolutePath()));
                                tempList.add(gallaryItem);
                            } catch (Exception e) {
                                Log.w(TAG, "Error processing file: " + file.getName(), e);
                            }
                        }
                    }
                }

                // Update UI on main thread
                runOnUiThread(() -> {
                    gallaryItemList.clear();
                    gallaryItemList.addAll(tempList);
                    gallaryAdapter.notifyDataSetChanged();
                    updateFileCount();

                    if (gallaryItemList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading files", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateFileCount() {
        TextView fileCount = findViewById(R.id.fileCount);
        if (fileCount != null) {
            int count = gallaryItemList.size();
            fileCount.setText(count + (count == 1 ? " file" : " files"));
        }
    }

    private void showEmptyState() {
        View emptyState = findViewById(R.id.emptyState);
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
        recyclerView.setVisibility(View.GONE);
        updateFileCount();
    }

    private void hideEmptyState() {
        View emptyState = findViewById(R.id.emptyState);
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
        recyclerView.setVisibility(View.VISIBLE);
    }

    private String getFileType(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toUpperCase();
        }
        return "Unknown";
    }

    private String getMimeType(String filePath) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
            if (extension != null && !extension.isEmpty()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting MIME type for: " + filePath, e);
        }
        return "application/octet-stream";
    }

    private void onFileClick(GallaryItem gallaryItem) {
        if (isSelectionMode) {
            // This is handled by the adapter now
            return;
        } else {
            openFile(gallaryItem);
        }
    }

    private void onFileLongClick(GallaryItem gallaryItem) {
        if (!isSelectionMode) {
            enterSelectionMode();
        }
        // Selection is handled by the adapter
    }

    // Implementation of OnSelectionModeChangeListener
    @Override
    public void onSelectionModeChanged(boolean isSelectionMode, int selectedCount) {
        this.isSelectionMode = isSelectionMode;
        updateSelectionUI(selectedCount);
        updateMenuVisibility();
        updateSelectAllIcon(selectedCount);
        updateSendButtonVisibility(selectedCount);
    }

    private void openFile(GallaryItem gallaryItem) {
        try {
            File file = new File(gallaryItem.getPath());
            if (!file.exists() || !file.canRead()) {
                Toast.makeText(this, "File not accessible: " + gallaryItem.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                fileUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider", file);
            } else {
                fileUri = Uri.fromFile(file);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, gallaryItem.getMimeType());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                showFileInfo(gallaryItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + gallaryItem.getName(), e);
            Toast.makeText(this, "Cannot open file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFileInfo(GallaryItem gallaryItem) {
        String info = "File: " + gallaryItem.getName() + "\n" +
                "Type: " + gallaryItem.getType() + "\n" +
                "Size: " + formatFileSize(gallaryItem.getSize()) + "\n" +
                "Path: " + gallaryItem.getPath();

        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void enterSelectionMode() {
        gallaryAdapter.setSelectionMode(true);
    }

    private void exitSelectionMode() {
        gallaryAdapter.setSelectionMode(false);
        if (fabSend != null) {
            fabSend.hide();
        }
    }

    private void updateSendButtonVisibility(int selectedCount) {
        if (fabSend != null) {
            if (isSelectionMode && selectedCount > 0) {
                fabSend.show();
            } else {
                fabSend.hide();
            }
        }
    }

    private void toggleSelectAll() {
        if (gallaryAdapter.isAllItemsSelected()) {
            gallaryAdapter.clearSelections();
        } else {
            gallaryAdapter.selectAll();
        }
    }

    private void updateSelectAllIcon(int selectedCount) {
        if (selectAllMenuItem != null) {
            boolean isAllSelected = selectedCount == gallaryItemList.size() && !gallaryItemList.isEmpty();
            if (isAllSelected) {
                selectAllMenuItem.setIcon(R.drawable.checkbox);
                selectAllMenuItem.setTitle("Deselect All");
            } else {
                selectAllMenuItem.setIcon(R.drawable.uncheck);
                selectAllMenuItem.setTitle("Select All");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSelectionUI(int selectedCount) {
        if (selectionCounter != null) {
            if (isSelectionMode) {
                selectionCounter.setVisibility(View.VISIBLE);
                selectionCounter.setText(selectedCount + " selected");
            } else {
                selectionCounter.setVisibility(View.GONE);
            }
        }

        // Update action bar title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isSelectionMode) {
                actionBar.setTitle(selectedCount + " selected");
            } else {
                actionBar.setTitle("Gallery");
            }
        }
    }

    private void updateMenuVisibility() {
        if (selectAllMenuItem != null) {
            selectAllMenuItem.setVisible(isSelectionMode);
        }
        if (cancelSelectionMenuItem != null) {
            cancelSelectionMenuItem.setVisible(isSelectionMode);
        }
        if (startSelectionMenuItem != null) {
            startSelectionMenuItem.setVisible(!isSelectionMode);
        }
    }

    private void sendSelectedFiles() {
        // Get selected items from adapter
        List<GallaryItem> selectedItems = gallaryAdapter.getSelectedItems();

        Log.d(TAG, "sendSelectedFiles called with " + selectedItems.size() + " selected items");

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log all selected items
        for (int i = 0; i < selectedItems.size(); i++) {
            Log.d(TAG, "Selected for sending " + i + ": " + selectedItems.get(i).getName() + " at " + selectedItems.get(i).getPath());
        }

        // Check connection availability
        String ip = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ip = Connect.ipToConnect;
            } else {
                Toast.makeText(this, "Android version doesn't support this operation", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "Connect class not accessible", e);
        }

        if (ip == null || ip.isEmpty()) {
            Toast.makeText(this, "No connection available. Please connect to a device first.", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<FileModel> filesToSend = new ArrayList<>();
        for (GallaryItem item : selectedItems) {
            File file = new File(item.getPath());
            if (file.exists() && file.canRead()) {
                filesToSend.add(new FileModel(item.getName(), item.getPath()));
                Log.d(TAG, "Added to filesToSend: " + item.getName());
            } else {
                Toast.makeText(this, "Skipping inaccessible file: " + item.getName(), Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Skipping inaccessible file: " + item.getPath());
            }
        }

        Log.d(TAG, "Final filesToSend size: " + filesToSend.size());

        if (filesToSend.isEmpty()) {
            Toast.makeText(this, "No accessible files to send", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the corrected batch method
        sendFilesInBatch(ip, filesToSend);
        exitSelectionMode();
    }
    // Send files in batch like the Files class does for "select all"
    private void sendFilesInBatch(String ip, ArrayList<FileModel> files) {
        if (ip == null || ip.isEmpty()) {
            Toast.makeText(this, "Error: IP address is null or empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Starting batch transfer of " + files.size() + " files...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Starting batch transfer of " + files.size() + " files to IP: " + ip);

        new Thread(() -> {
            try {
                // Validate files first
                ArrayList<FileModel> validFiles = new ArrayList<>();
                for (FileModel file : files) {
                    File fileToCheck = new File(file.getFilePath());
                    if (fileToCheck.exists() && fileToCheck.canRead() && fileToCheck.isFile()) {
                        validFiles.add(file);
                    } else {
                        Log.w(TAG, "Skipping invalid file: " + file.getFileName());
                        runOnUiThread(() ->
                                Toast.makeText(this, "Skipping invalid file: " + file.getFileName(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }

                if (validFiles.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No valid files to send", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Send files one by one
                int successCount = 0;
                int failureCount = 0;

                for (int i = 0; i < validFiles.size(); i++) {
                    FileModel file = validFiles.get(i);
                    final int fileIndex = i + 1;

                    try {
                        // Update UI with current file progress
                        runOnUiThread(() ->
                                Toast.makeText(this, "Sending (" + fileIndex + "/" + validFiles.size() + "): " +
                                        file.getFileName(), Toast.LENGTH_SHORT).show()
                        );

                        Log.d(TAG, "Sending file " + fileIndex + "/" + validFiles.size() + ": " + file.getFileName());

                        // Create new FileTransferClient for each file
                        FileTransferClient fileTransferClient = new FileTransferClient();

                        // Send individual file - this is the correct way
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            fileTransferClient.sendFile(ip, file.getFilePath(), 1, "", "", Connect.serverMac);
                        }

                        // Wait a bit between files to avoid overwhelming the receiver
                        // and to let the AsyncTask complete
                        Thread.sleep(2000); // 2 seconds delay between files

                        successCount++;
                        Log.d(TAG, "Successfully initiated transfer for file " + fileIndex + ": " + file.getFileName());

                    } catch (Exception e) {
                        failureCount++;
                        Log.e(TAG, "Failed to send file " + fileIndex + ": " + file.getFileName(), e);

                        runOnUiThread(() ->
                                Toast.makeText(this, "Failed to send: " + file.getFileName(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }

                final int finalSuccess = successCount;
                final int finalFailure = failureCount;

                runOnUiThread(() -> {
                    String result = "Transfer initiated for " + finalSuccess + " files";
                    if (finalFailure > 0) {
                        result += ", " + finalFailure + " failed to start";
                    }
                    Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Overall batch transfer error: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Transfer error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    // Helper method to send files as a consolidated batch
    private void sendFilesAsConsolidatedBatch(FileTransferClient client, String ip, ArrayList<FileModel> files) {
        try {
            StringBuilder fileList = new StringBuilder();
            long totalSize = 0;

            for (int i = 0; i < files.size(); i++) {
                FileModel file = files.get(i);
                File fileObj = new File(file.getFilePath());

                if (i > 0) fileList.append("|"); // Use delimiter to separate files
                fileList.append(file.getFilePath());
                totalSize += fileObj.length();
            }

            Log.d(TAG, "Sending consolidated batch: " + files.size() + " files, total size: " + totalSize + " bytes");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                client.sendFile(ip, fileList.toString(), files.size(), "", "", Connect.serverMac);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Successfully sent " + files.size() + " files", Toast.LENGTH_LONG).show();
            });

            Log.d(TAG, "Batch transfer completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in consolidated batch send: " + e.getMessage(), e);

            // Fallback: If batch method fails, try individual sends with proper synchronization
            sendFilesWithSynchronization(client, ip, files);
        }
    }

    // Fallback method: Send files individually but with proper synchronization
    private void sendFilesWithSynchronization(FileTransferClient client, String ip, ArrayList<FileModel> files) {
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < files.size(); i++) {
            FileModel file = files.get(i);
            final int fileIndex = i + 1;

            try {
                // Add synchronization delay
                if (i > 0) {
                    Thread.sleep(1000); // Wait 1 second between files
                }

                runOnUiThread(() ->
                        Toast.makeText(this, "Sending (" + fileIndex + "/" + files.size() + "): " + file.getFileName(), Toast.LENGTH_SHORT).show()
                );

                // Send individual file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    client.sendFile(ip, file.getFilePath(), 1, "", "", Connect.serverMac);
                }

                successCount++;
                Log.d(TAG, "Successfully sent file " + fileIndex + ": " + file.getFileName());

            } catch (Exception e) {
                failureCount++;
                Log.e(TAG, "Failed to send file " + fileIndex + ": " + file.getFileName(), e);
            }
        }

        final int finalSuccess = successCount;
        final int finalFailure = failureCount;

        runOnUiThread(() -> {
            String result = "Transfer completed: " + finalSuccess + " sent";
            if (finalFailure > 0) {
                result += ", " + finalFailure + " failed";
            }
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("DefaultLocale")
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        else if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        else return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}