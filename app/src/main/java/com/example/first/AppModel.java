package com.example.first;

import android.graphics.drawable.Drawable;

public class AppModel {
    private String appName;
    private Drawable appIcon;
    public AppModel(String appName, Drawable appIcon){
        this.appName = appName;
        this.appIcon = appIcon;
    }
    public String getAppName(){
        return appName;
    }
    public Drawable getAppIcon(){
        return appIcon;
    }
}
