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

public class Gallary extends AppCompatActivity {

    private static final String TAG = "Gallary";

    private RecyclerView recyclerView;
    private GallaryAdapter gallaryAdapter;
    private List<GallaryItem> gallaryItemList;
    private List<GallaryItem> selectedItems;
    private final String folderPath = Environment.getExternalStorageDirectory() + "/ShareGT";

    private boolean isSelectionMode = false;
    private boolean isAllSelected = false;
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
        selectedItems = new ArrayList<>();

        gallaryAdapter = new GallaryAdapter(gallaryItemList, this::onFileClick, this::onFileLongClick);
        recyclerView.setAdapter(gallaryAdapter);

        // Initialize selection counter and send FAB
        selectionCounter = findViewById(R.id.selectionCounter);
        fabSend = findViewById(R.id.fabSend);

        if (fabSend != null) {
            fabSend.setOnClickListener(v -> sendSelectedFiles());
            fabSend.hide();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
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
            toggleItemSelection(gallaryItem);
        } else {
            openFile(gallaryItem);
        }
    }

    private void onFileLongClick(GallaryItem gallaryItem) {
        if (!isSelectionMode) {
            enterSelectionMode();
        }
        toggleItemSelection(gallaryItem);
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
        isSelectionMode = true;
        isAllSelected = false;
        updateSelectionUI();
        gallaryAdapter.setSelectionMode(true);
        updateMenuVisibility();
        updateSelectAllIcon();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        isAllSelected = false;
        selectedItems.clear();
        updateSelectionUI();
        gallaryAdapter.setSelectionMode(false);
        gallaryAdapter.clearSelections();
        updateMenuVisibility();
        updateSelectAllIcon();

        if (fabSend != null) {
            fabSend.hide();
        }
    }

    private void toggleItemSelection(GallaryItem item) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item);
            gallaryAdapter.setItemSelected(item, false);
        } else {
            selectedItems.add(item);
            gallaryAdapter.setItemSelected(item, true);
        }

        // Update isAllSelected state
        isAllSelected = selectedItems.size() == gallaryItemList.size();
        updateSelectionUI();
        updateSelectAllIcon();

        // Show/hide send FAB based on selection
        if (fabSend != null) {
            if (selectedItems.isEmpty()) {
                fabSend.hide();
            } else {
                fabSend.show();
            }
        }
    }

    private void toggleSelectAll() {
        if (isAllSelected) {
            // Deselect all
            selectedItems.clear();
            gallaryAdapter.clearSelections();
            isAllSelected = false;

            if (fabSend != null) {
                fabSend.hide();
            }
        } else {
            // Select all
            selectedItems.clear();
            selectedItems.addAll(gallaryItemList);
            gallaryAdapter.selectAll();
            isAllSelected = true;

            if (fabSend != null && !selectedItems.isEmpty()) {
                fabSend.show();
            }
        }

        updateSelectionUI();
        updateSelectAllIcon();
    }

    private void updateSelectAllIcon() {
        if (selectAllMenuItem != null) {
            if (isAllSelected) {
                // Change to checked icon - using Android's built-in checked checkbox
                selectAllMenuItem.setIcon(android.R.drawable.checkbox_on_background);
                selectAllMenuItem.setTitle("Deselect All");
            } else {
                // Change to unchecked icon - using Android's built-in unchecked checkbox
                selectAllMenuItem.setIcon(android.R.drawable.checkbox_off_background);
                selectAllMenuItem.setTitle("Select All");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSelectionUI() {
        if (selectionCounter != null) {
            if (isSelectionMode) {
                selectionCounter.setVisibility(View.VISIBLE);
                int count = selectedItems.size();
                selectionCounter.setText(count + (" selected"));
            } else {
                selectionCounter.setVisibility(View.GONE);
            }
        }

        // Update action bar title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isSelectionMode) {
                int count = selectedItems.size();
                actionBar.setTitle(count + (" selected"));
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
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate that Connect class and its fields exist
        String ip = null;
        String serverMac = null;

        try {
            // Use reflection to safely check if Connect class exists
            Class<?> connectClass = Class.forName("com.example.first.Connect");
            ip = (String) connectClass.getDeclaredField("ipToConnect").get(null);
            serverMac = (String) connectClass.getDeclaredField("serverMac").get(null);
        } catch (Exception e) {
            Log.w(TAG, "Connect class or fields not found", e);
        }

        if (ip == null || ip.isEmpty()) {
            Toast.makeText(this, "No connection available. Please connect to a device first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Convert GallaryItem to FileModel for compatibility
        ArrayList<FileModel> filesToSend = new ArrayList<>();
        for (GallaryItem item : selectedItems) {
            filesToSend.add(new FileModel(item.getName(), item.getPath()));
        }

        // Send files
        sendFiles(ip, serverMac, filesToSend);

        // Exit selection mode after initiating send
        exitSelectionMode();
    }

    public void sendFiles(String ip, String serverMac, ArrayList<FileModel> files) {
        if (ip == null || ip.isEmpty()) {
            Toast.makeText(this, "Error: IP address is null or empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Starting file transfer...", Toast.LENGTH_SHORT).show();

        // Use executor service for network operations
        executorService.execute(() -> {
            try {
                // Check if FileTransferClient exists
                Class<?> clientClass = Class.forName("com.example.first.FileTransferClient");
                Object fileTransferClient = clientClass.newInstance();

                for (FileModel file : files) {
                    try {
                        final String filePath = file.getFilePath();
                        final String fileName = file.getFileName();

                        if (filePath == null || filePath.isEmpty()) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Invalid file path for: " + fileName, Toast.LENGTH_SHORT).show()
                            );
                            continue;
                        }

                        // Verify file exists and is readable
                        File fileToSend = new File(filePath);
                        if (!fileToSend.exists() || !fileToSend.canRead()) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Cannot access file: " + fileName, Toast.LENGTH_SHORT).show()
                            );
                            continue;
                        }

                        runOnUiThread(() ->
                                Toast.makeText(this, "Sending: " + fileName, Toast.LENGTH_SHORT).show()
                        );

                        Log.d(TAG, "Sending file: " + fileName + " from path: " + filePath);

                        // Use reflection to call sendFile method
                        try {
                            java.lang.reflect.Method sendFileMethod = clientClass.getMethod(
                                    "sendFile", String.class, String.class, int.class, String.class, String.class, String.class
                            );
                            sendFileMethod.invoke(fileTransferClient, ip, filePath, 1, "", "", serverMac);

                            runOnUiThread(() ->
                                    Toast.makeText(this, "Sent: " + fileName, Toast.LENGTH_SHORT).show()
                            );
                        } catch (NoSuchMethodException e) {
                            Log.w(TAG, "sendFile method not found, trying alternative", e);
                            runOnUiThread(() ->
                                    Toast.makeText(this, "File transfer method not available", Toast.LENGTH_SHORT).show()
                            );
                        }

                    } catch (Exception e) {
                        final String errorMsg = e.getMessage();
                        final String fileName = file.getFileName();
                        runOnUiThread(() ->
                                Toast.makeText(this, "Error sending " + fileName + ": " + errorMsg, Toast.LENGTH_SHORT).show()
                        );
                        Log.e(TAG, "Error sending file: " + fileName, e);
                    }
                }

                runOnUiThread(() ->
                        Toast.makeText(this, "File transfer completed", Toast.LENGTH_SHORT).show()
                );

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "FileTransferClient class not found", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "File transfer service not available", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                runOnUiThread(() ->
                        Toast.makeText(this, "Transfer error: " + errorMsg, Toast.LENGTH_SHORT).show()
                );
                Log.e(TAG, "Transfer error", e);
            }
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