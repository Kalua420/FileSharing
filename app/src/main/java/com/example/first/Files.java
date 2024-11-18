package com.example.first;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.util.ArrayList;

public class Files extends Fragment {
    GridView gridView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fetchAudioFiles();
        return inflater.inflate(R.layout.fragment_files, container, false);
    }
    private ArrayList<String> fetchAudioFiles() {
        ArrayList<String> listOfAllImages = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                listOfAllImages.add(cursor.getString(columnIndex));
                Log.d("Files : ",cursor.getString(columnIndex));
            }
            cursor.close();
        }
        return listOfAllImages;
    }
}