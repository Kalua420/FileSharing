package com.example.first;

import androidx.appcompat.app.AppCompatActivity;

public class GetAbsoluteFileName extends AppCompatActivity {
    public String getAbsoluteFileName(String item) {
        int index = item.lastIndexOf("/");
        String name = item.substring(index);
        return name.substring(1);
    }
}
