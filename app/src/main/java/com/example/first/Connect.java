package com.example.first;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class Connect extends AppCompatActivity {
    LinearLayout send,receive;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        init();
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        send.setOnClickListener(v -> Toast.makeText(getApplicationContext(),"Send",Toast.LENGTH_SHORT).show());
        receive.setOnClickListener(v -> Toast.makeText(getApplicationContext(),"Received",Toast.LENGTH_SHORT).show());
    }
    public void init(){
        send = findViewById(R.id.send);
        receive = findViewById(R.id.recv);
        toolbar = findViewById(R.id.toolbar);
    }
}