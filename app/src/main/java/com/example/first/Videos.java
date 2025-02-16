package com.example.first;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
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

public class Videos extends Fragment {
    GridView gridView;
    VideoAdapter videoAdaptor;
    FileTransferClient fileTransferClient;
    GetAbsoluteFileName fileName = new GetAbsoluteFileName();

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
            String file = videoModels.get(i).getPath();
            String ip = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ip = Connect.ipToConnect;
            }
            fileTransferClient = new FileTransferClient();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                fileTransferClient.sendFile(ip,file,1,"","",Connect.serverMac);
            }
            Toast.makeText(getContext(),"Sending : "+fileName.getAbsoluteFileName(file),Toast.LENGTH_SHORT).show();
        });
        gridView.setOnItemLongClickListener((adapterView, view1, i, l) -> {
            String item = videoModels.get(i).getPath();
            MediaHandler mediaHandler = new MediaHandler();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaHandler.playAudioWithExternalPlayer(requireContext(),Uri.parse(item));
            }
            return true;
        });
    }
    public ArrayList<VideoModel> getAllVideosFromDevice() {
        ArrayList<VideoModel> videoPaths = new ArrayList<>();
        ContentResolver contentResolver = requireContext().getContentResolver();
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