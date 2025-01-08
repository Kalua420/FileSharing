package com.example.first;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Build;
import android.os.Bundle;
import android.widget.GridView;
import com.google.android.material.tabs.TabLayout;
import java.util.Objects;


@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {
    com.google.android.material.tabs.TabItem images;
    com.google.android.material.tabs.TabItem audios;
    com.google.android.material.tabs.TabItem videos;
    com.google.android.material.tabs.TabItem files;
    TabLayout tabLayout;
    ViewPager2 viewPager;
    CustomAdaptor customAdaptor;
    GridView gridView;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        customAdaptor = new CustomAdaptor(this);
        viewPager.setAdapter(customAdaptor);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(tabLayout.getTabAt(position)).select();
            }
        });
    }
    public void init(){
        images = findViewById(R.id.images);
        audios = findViewById(R.id.audios);
        videos = findViewById(R.id.videos);
        files = findViewById(R.id.files);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        gridView = findViewById(R.id.gridView);
        toolbar = findViewById(R.id.toolbar);
    }
}