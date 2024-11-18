package com.example.first;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Videos extends Fragment {
    GridView gridView;
    VideoAdapter videoAdaptor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_videos, container, false);
        gridView = view.findViewById(R.id.gridView);
        ArrayList<VideoModel> videoModels = getAllVideosFromDevice();
        videoAdaptor = new VideoAdapter(getContext(),videoModels);
        gridView.setAdapter(videoAdaptor);
        return view;
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<VideoModel> videoModels = getAllVideosFromDevice();
        Collections.reverse(videoModels);
        gridView.setOnItemClickListener((adapterView, view1, i, l) -> {
            String id = videoModels.get(i).getPath();
//            Toast.makeText(getContext(),id,Toast.LENGTH_SHORT).show();
            Uri videoUri = Uri.parse(id);
            // Create an intent to play the video
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(videoUri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start the external video player
            startActivity(intent);
        });
    }
    public ArrayList<VideoModel> getAllVideosFromDevice() {
        ArrayList<VideoModel> videoPaths = new ArrayList<>();
        ContentResolver contentResolver = getContext().getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Thumbnails.DATA
        };
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                String thumbnail = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                videoPaths.add(new VideoModel(title,path,thumbnail));
            }
            cursor.close();
        }
        return videoPaths;
    }
}