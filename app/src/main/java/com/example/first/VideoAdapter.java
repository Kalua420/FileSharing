package com.example.first;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class VideoAdapter extends BaseAdapter{
    private final LayoutInflater inflater;
    Context context;
    ArrayList<VideoModel> videoList;
    public VideoAdapter(Context context, List<VideoModel> videoList) {
        this.context = context;
        Collections.reverse(videoList);
        this.videoList = (ArrayList<VideoModel>) videoList;
        this.inflater = LayoutInflater.from(context);
    }
    @Override
    public int getCount() {
        return videoList.size();
    }

    @Override
    public Object getItem(int position) {
        return videoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.video_list, parent, false);
        }
        ImageView videoThumbnail = convertView.findViewById(R.id.videoThumbnail);
        TextView videoTitle = convertView.findViewById(R.id.videoTitle);
        VideoModel video = videoList.get(position);
        videoTitle.setText(video.getTitle());
        Glide.with(context).load(video.getThumbnail()).into(videoThumbnail);
        return convertView;
    }
}