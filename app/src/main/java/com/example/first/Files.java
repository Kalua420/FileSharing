package com.example.first;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Objects;

public class Files extends Fragment {
    private static final int PICK_FILES_REQUEST = 1;
    private FilesAdapter adapter;
    private ArrayList<FileModel> selectedFiles;

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
            if (selectedFiles.size()>1){
                Toast.makeText(getContext(),"Sending Files",Toast.LENGTH_SHORT).show();
            }else Toast.makeText(getContext(),"Sending File : "+selectedFiles.get(0),Toast.LENGTH_SHORT).show();
            String ip = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ip = Connect.ipToConnect;
            }
            sendFiles(ip, selectedFiles);
        });

        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Files"), PICK_FILES_REQUEST);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILES_REQUEST) {
            getActivity();
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    selectedFiles.clear(); // Clear previous selections

                    if (data.getClipData() != null) {
                        // Multiple files selected
                        android.content.ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            String path = getPathFromUri(uri);
                            String fileName = getFileName(uri);
                            if (!path.isEmpty()) {
                                selectedFiles.add(new FileModel(fileName, path));
                            }
                        }
                    } else if (data.getData() != null) {
                        // Single file selected
                        Uri uri = data.getData();
                        String path = getPathFromUri(uri);
                        String fileName = getFileName(uri);
                        if (!path.isEmpty()) {
                            selectedFiles.add(new FileModel(fileName, path));
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String filePath = null;

        try {
            // Handle content scheme
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {MediaStore.MediaColumns.DATA};

                try (Cursor cursor = requireContext().getContentResolver().query(
                        uri,
                        projection,
                        null,
                        null,
                        null
                )) {

                    if (cursor != null && cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        filePath = cursor.getString(columnIndex);
                    }
                }
            }

            // Handle file scheme
            if (filePath == null && "file".equalsIgnoreCase(uri.getScheme())) {
                filePath = uri.getPath();
            }

            // If still null, try content resolver for specific types
            if (filePath == null && DocumentsContract.isDocumentUri(requireContext(), uri)) {
                String wholeID = DocumentsContract.getDocumentId(uri);

                // Split at colon, use second item
                String[] splits = wholeID.split(":");
                String type = splits[0];
                String id = splits.length > 1 ? splits[1] : splits[0];

                switch (type.toLowerCase()) {
                    case "image":
                        filePath = getPathFromType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        break;
                    case "video":
                        filePath = getPathFromType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                        break;
                    case "audio":
                        filePath = getPathFromType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                        break;
                    default:
                        filePath = getPathFromType(MediaStore.Files.getContentUri("external"), id);
                        break;
                }
            }
            if (filePath == null) {
                Cursor cursor = null;
                final String column = "_data";
                final String[] projection = {column};

                try {
                    cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        final int index = cursor.getColumnIndexOrThrow(column);
                        filePath = cursor.getString(index);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return filePath != null ? filePath : "";
    }

    private String getPathFromType(Uri contentUri, String id) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        String selection = "_id=?";
        String[] selectionArgs = new String[]{id};

        try (Cursor cursor = requireContext().getContentResolver().query(
                contentUri,
                projection,
                selection,
                selectionArgs,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(columnIndex);
            }
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
        FileTransferClient fileTransferClient = new FileTransferClient();
        for (FileModel file : files) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                fileTransferClient.sendFile(ip,file.getFilePath(),1,"","",Connect.serverMac,Connect.myMacAddress);
            }
        }
    }
}