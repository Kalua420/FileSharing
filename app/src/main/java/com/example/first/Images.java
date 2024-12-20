package com.example.first;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Images extends Fragment {
    GridView gridView;
    ImageAdapter imageAdapter;
    @SuppressLint("MissingInflatedId")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_images, container, false);
        gridView = view.findViewById(R.id.gridView);
        ArrayList<String> imagePaths = getImagesPath();
        imageAdapter = new ImageAdapter(getContext(), imagePaths);
        gridView.setAdapter(imageAdapter);
        return view;
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<String> imageList = getImagesPath();
        Collections.reverse(imageList);
        gridView.setOnItemClickListener((adapterView, view1, i, l) -> {
            String item = imageList.get(i);
            showDialogBox(item);
        });
    }
    private ArrayList<String> getImagesPath() {
        ArrayList<String> listOfAllImages = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
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

    public void showDialogBox(String item){
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.image_dialog_box);
        TextView title = dialog.findViewById(R.id.image_name);
        AppCompatButton close = dialog.findViewById(R.id.close);
        ImageView imageView = dialog.findViewById(R.id.imageView);
        int index = item.lastIndexOf("/");
        String name = item.substring(index);
        title.setText(name.substring(1));
        imageView.setImageURI(Uri.parse(item));
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(Objects.requireNonNull(dialog.getWindow()).getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT; // or any custom width
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT; // or any custom height
        dialog.getWindow().setAttributes(layoutParams);
        close.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();
    }
}