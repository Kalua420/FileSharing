package com.example.first;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // User is already logged in, redirect to Connect activity
            navigateToConnectActivity();
            return;  // Important: prevent the rest of onCreate from executing
        }

        setContentView(R.layout.activity_login);

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView txtRegister = findViewById(R.id.txtRegister);
        progressBar = findViewById(R.id.progressBar);

        dbHelper = new DatabaseHelper();

        // Set up password visibility toggle
        setupPasswordVisibilityToggle();

        // Login button click listener
        btnLogin.setOnClickListener(v -> validateAndLogin());

        // Register text click listener (Navigates to RegisterActivity)
        txtRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordVisibilityToggle() {
        editTextPassword.setOnTouchListener((v, event) -> {
            // Check if the touch was on the right side of the EditText (where the drawable is)
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getRawX() >= (editTextPassword.getRight() - editTextPassword.getCompoundDrawables()[2].getBounds().width() - editTextPassword.getPaddingRight())) {

                // Toggle password visibility
                passwordVisible = !passwordVisible;

                // Update input type and drawable based on visibility state
                if (passwordVisible) {
                    // Show password
                    editTextPassword.setTransformationMethod(null);
                    editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0);
                } else {
                    // Hide password
                    editTextPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    editTextPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
                }

                // Move cursor to end of text
                editTextPassword.setSelection(editTextPassword.getText().length());
                return true;
            }
            return false;
        });
    }

    private void navigateToConnectActivity() {
        Intent intent = new Intent(LoginActivity.this, Connect.class);
        intent.putExtra("userId", sessionManager.getUserId());
        startActivity(intent);
        finish();  // Close the login activity
    }

    private void validateAndLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        // Show progress bar and disable login button
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Perform login
        dbHelper.loginUser(email, password, (success, message, userId) -> {
            // Hide progress bar and enable login button
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                // Handle login result
                if (success) {
                    // Save user session using SessionManager
                    sessionManager.createLoginSession(userId, email);

                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    navigateToConnectActivity();
                } else {
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}