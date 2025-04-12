package com.example.first;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;

public class LogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LogsAdapter logsAdapter;
    private ArrayList<LogEntry> logsList;
    private DatabaseHelper databaseHelper;
    private ProgressBar progressBar;
    private TextView emptyView;
    public static int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        // Check if the user is logged in
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            // If not logged in, redirect to login activity
            Intent intent = new Intent(LogsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();  // Close the current activity
            return;
        }

        // Get userId from SharedPreferences (not from Intent)
        userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewLogs);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Setup ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Activity Logs");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize database helper
        databaseHelper = new DatabaseHelper();

        // Initialize logs list and adapter
        logsList = new ArrayList<>();
        logsAdapter = new LogsAdapter(this, logsList);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(logsAdapter);

        // Add item decoration for dividers between items
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Fetch logs
        fetchLogs();
    }

    private void fetchLogs() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        databaseHelper.fetchLogsByUserId(userId, new DatabaseHelper.LogsCallback() {
            @SuppressLint({"NotifyDataSetChanged", "SetTextI18n"})
            @Override
            public void onLogsResult(boolean success, ArrayList<LogEntry> logs, String message) {
                // Hide progress bar
                progressBar.setVisibility(View.GONE);

                if (success) {
                    logsList.clear();

                    if (logs != null && !logs.isEmpty()) {
                        logsList.addAll(logs);
                        logsAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    } else {
                        // Show empty view if no logs found
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setText("No logs found");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Show error message
                    Toast.makeText(LogsActivity.this, message, Toast.LENGTH_SHORT).show();
                    emptyView.setText("Error loading logs: " + message);
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupSwipeRefresh() {
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchLogs();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    // Handle user logout
    public void logout() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId");
        editor.remove("isLoggedIn");
        editor.apply();

        // Navigate back to login activity
        Intent intent = new Intent(LogsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();  // Close the current activity
    }
}
