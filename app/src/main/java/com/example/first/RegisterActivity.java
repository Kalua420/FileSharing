package com.example.first;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPhone, editTextPassword, editTextConfirmPassword, editTextAadharNumber, editTextAddress;
    private Button btnRegister;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private Spinner spinnerBranch;
    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView txtLogin = findViewById(R.id.txtLogin);
        progressBar = findViewById(R.id.progressBar);
        dbHelper = new DatabaseHelper();
        spinnerBranch = findViewById(R.id.spinnerBranch);
        editTextAadharNumber = findViewById(R.id.editTextAadharNumber);
        editTextAddress = findViewById(R.id.editTextAddress);

        // Set up password visibility toggles
        setupPasswordVisibilityToggles();

        // Register button click listener
        btnRegister.setOnClickListener(v -> validateAndRegister());

        // Login text click listener
        txtLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
        setupBranchSpinner();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupPasswordVisibilityToggles() {
        // Set up password visibility toggle
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

        // Set up confirm password visibility toggle
        editTextConfirmPassword.setOnTouchListener((v, event) -> {
            // Check if the touch was on the right side of the EditText (where the drawable is)
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    event.getRawX() >= (editTextConfirmPassword.getRight() - editTextConfirmPassword.getCompoundDrawables()[2].getBounds().width() - editTextConfirmPassword.getPaddingRight())) {

                // Toggle confirm password visibility
                confirmPasswordVisible = !confirmPasswordVisible;

                // Update input type and drawable based on visibility state
                if (confirmPasswordVisible) {
                    // Show password
                    editTextConfirmPassword.setTransformationMethod(null);
                    editTextConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0);
                } else {
                    // Hide password
                    editTextConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    editTextConfirmPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0);
                }

                // Move cursor to end of text
                editTextConfirmPassword.setSelection(editTextConfirmPassword.getText().length());
                return true;
            }
            return false;
        });
    }

    private void validateAndRegister() {
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();
        String aadharNumber = editTextAadharNumber.getText().toString().trim();
        String address = editTextAddress.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Invalid email format");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("Phone number is required");
            editTextPhone.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            editTextPhone.setError("Invalid phone number");
            editTextPhone.requestFocus();
            return;
        }
        if (aadharNumber.isEmpty()){
            editTextAadharNumber.setError("Aadhar number is required");
            editTextAadharNumber.requestFocus();
            return;
        }
        if (aadharNumber.length() < 12) {
            editTextAadharNumber.setError("Invalid Aadhar Number");
            editTextAadharNumber.requestFocus();
            return;
        }
        if (address.isEmpty()){
            editTextAddress.setError("Address is required");
            editTextAddress.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            editTextConfirmPassword.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        String branch = spinnerBranch.getSelectedItem().toString();
        // Perform registration
        dbHelper.registerUser(phone, email, password, branch, aadharNumber, address,(success, message, userId) -> {
            // Hide progress
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                // Handle registration result
                if (success) {
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
    private void setupBranchSpinner() {
        // Initialize with default branches
        ArrayList<String> branchList = new ArrayList<>();
        branchList.add("Select Branch"); // Default first item

        // Set up adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                branchList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBranch.setAdapter(adapter);

        // Fetch branches from database
        dbHelper.fetchBranches((success, branches, message) -> {
            if (success && branches != null) {
                // Clear existing items except "Select Branch"
                branchList.clear();
                branchList.add("Select Branch");

                // Add branches from database
                branchList.addAll(branches);

                // Notify adapter of data change
                runOnUiThread(adapter::notifyDataSetChanged);
            } else {
                // If database fetch fails, use default branches
                branchList.clear();
                branchList.add("Select Branch");

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Log.e("Branches", "Error loading from DB: " + message);
                });
            }
        });
    }
}