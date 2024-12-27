package com.example.first;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

public class Audios extends Fragment {
    GridView gridView;
    ArrayList<String> audioList;
    AudioAdapter audioAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audios, container, false);
        gridView = view.findViewById(R.id.gridAudio);
        ArrayList<String> audioFiles = fetchAudioFiles();
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) TextView txt = view.findViewById(R.id.tmpText);
        audioAdapter = new AudioAdapter(getContext(),audioFiles);
        gridView.setAdapter(audioAdapter);
        return view;
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<String> audioFiles = fetchAudioFiles();
        Collections.reverse(audioFiles);
        gridView.setOnItemClickListener((adapterView, view1, i, l) -> {
            FileTransferClient fileTransferClient;
            String item = audioFiles.get(i);
            String ip = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ip = Connect.ipToConnect;
            }
            fileTransferClient = new FileTransferClient();
            fileTransferClient.sendFile(ip,item);
            Toast.makeText(getContext(),"Sending :"+audioFiles.get(i),Toast.LENGTH_SHORT).show();
        });
    }
    private ArrayList<String> fetchAudioFiles() {
        ArrayList<String> listOfAllImages = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                listOfAllImages.add(cursor.getString(columnIndex));
            }
            cursor.close();
        }
        return listOfAllImages;
    }
}