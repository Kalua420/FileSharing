package com.example.first;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Apps extends Fragment {
    GridView gridView;
    AppAdapter appAdapter;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_apps, container, false);
        gridView = view.findViewById(R.id.gridView);
        ArrayList<AppModel> appModels = getAllApps();
        appAdapter = new AppAdapter(this, appModels);
        gridView.setAdapter(appAdapter);
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private ArrayList<AppModel> getAllApps() {
        ArrayList<AppModel> appModels = new ArrayList<>();
        PackageManager pm = requireContext().getPackageManager();
        ArrayList<ApplicationInfo> packages = (ArrayList<ApplicationInfo>) pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        for (ApplicationInfo packageInfo : packages) {
            Drawable icon = pm.getApplicationIcon(packageInfo);
            String appName = pm.getApplicationLabel(packageInfo).toString();
            appModels.add(new AppModel(appName,icon));
        }
        return appModels;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ArrayList<AppModel> appModels = getAllApps();
        gridView.setOnItemClickListener((adapterView, view1, i, l) -> {
            String appName = appModels.get(i).getAppName();
            Toast.makeText(getContext(),appName,Toast.LENGTH_SHORT).show();
        });
    }
}