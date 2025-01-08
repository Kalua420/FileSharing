package com.example.first;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;

public class AudioAdapter extends BaseAdapter{
    Context context;
    ArrayList<String> audioList;
    public AudioAdapter(Context context,ArrayList<String> audioList){
        this.context = context;
        Collections.reverse(audioList);
        this.audioList = audioList;
    }
    @Override
    public int getCount() {
        return audioList.size();
    }

    @Override
    public Object getItem(int i) {
        return audioList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null){
            view = LayoutInflater.from(context).inflate(R.layout.audio_item,viewGroup,false);
        }
        TextView textView = view.findViewById(R.id.textView);
        int index = audioList.get(i).lastIndexOf("/");
        String title = audioList.get(i).substring(index);
        textView.setText(title.substring(1));
        return view;
    }
}
