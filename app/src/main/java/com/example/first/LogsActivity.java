package com.example.first;

import static java.security.AccessController.getContext;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class LogsActivity extends AppCompatActivity {
    TextView textView;
    NestedScrollView scrollView;
    Toolbar toolbar;
    AppCompatButton clearLogs;
    SwipeRefreshLayout swipeRefreshLayout;
    String fileDir = "/storage/emulated/0/ShareGT/.logs.txt";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_logs);
        textView = findViewById(R.id.tab1);
        scrollView = findViewById(R.id.scrollView);
        toolbar = findViewById(R.id.toolbar);
        clearLogs = findViewById(R.id.clearLogs);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        readAndDisplayFile();
        clearLogs.setOnClickListener(v -> {
            File file = new File("/storage/emulated/0/ShareGT/.logs.txt");
            if (!isLogFileEmpty()){
                eraseLogFileContent();
                Toast.makeText(getApplicationContext(), "Logs cleared successfully", Toast.LENGTH_SHORT).show();
                refreshData();
            }else {
                Toast.makeText(getApplicationContext(), "No Logs To Clear", Toast.LENGTH_SHORT).show();
            }
        });
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }
        });

    }
    private void readAndDisplayFile() {
        File file = new File(fileDir);

        if (file.exists()) {
            StringBuilder text = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }

                if (textView != null) {
                    textView.setText(text.toString());
                    scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                } else {
                    Toast.makeText(getApplicationContext(), "TextView not initialized", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Logs", Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshData() {
        readAndDisplayFile();
        swipeRefreshLayout.setRefreshing(false);
    }
    public void eraseLogFileContent() {
        File logFile = new File(fileDir);

        // Check if file exists and has read/write permissions
        if (!logFile.exists()) {
            System.out.println("File does not exist");
            return;
        }

        if (!logFile.canWrite()) {
            System.out.println("No write permission");
            return;
        }

        try {
            // Open file in write mode (which truncates content) and immediately close it
            FileWriter writer = new FileWriter(logFile, false);
            writer.write("");
            writer.close();
            System.out.println("Log file content erased successfully");
        } catch (IOException e) {
            System.out.println("Error erasing file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean isLogFileEmpty() {
        File logFile = new File(fileDir);

        // Check if file exists
        if (!logFile.exists()) {
            System.out.println("File does not exist");
            return true;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String firstLine = reader.readLine();
            reader.close();

            // If firstLine is null, the file is empty
            boolean isEmpty = (firstLine == null);
            System.out.println("File is " + (isEmpty ? "empty" : "not empty"));
            return isEmpty;

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
            return true; // Assume empty if we can't read it
        }
    }
}