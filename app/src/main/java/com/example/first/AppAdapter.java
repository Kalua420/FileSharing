package com.example.first;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends BaseAdapter {
    private Apps context;
    private ArrayList<AppModel> appList;
    private PackageManager packageManager;
    public AppAdapter(Apps context, ArrayList<AppModel> appList) {
        this.context = context;
        this.appList = appList;
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int position) {
        return appList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(context.getContext()).inflate(R.layout.app_list, parent, false);
        AppModel app = appList.get(position);
        ImageView appIcon = convertView.findViewById(R.id.appIcon);
        TextView appName = convertView.findViewById(R.id.appName);
        appIcon.setImageDrawable(app.getAppIcon());
        appName.setText(app.getAppName());

        return convertView;
    }
}

