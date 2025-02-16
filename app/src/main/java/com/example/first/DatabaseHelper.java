package com.example.first;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;

public class DatabaseHelper {
    private static final String TAG = "DatabaseHelper";

    // Database Configuration
    private static final String DB_NAME = "project";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    private static final String DB_IP = "192.168.75.137";
    private static final int DB_PORT = 3306;
    private static final int TIMEOUT = 5000;

    // Callback Interfaces
    public interface DatabaseCallback {
        void onResult(boolean success, String message, int userId);
    }
    public interface BranchCallback {
        void onBranchResult(boolean success, ArrayList<String> branches, String message);
    }

    // Hashing Method for Password
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Password hashing error", e);
            return null;
        }
    }

    // Database Connection Method
    private Connection getConnection() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            Properties props = new Properties();
            props.put("user", DB_USER);
            props.put("password", DB_PASS);
            props.put("connectTimeout", String.valueOf(TIMEOUT));
            props.put("socketTimeout", String.valueOf(TIMEOUT));
            props.put("autoReconnect", "true");
            props.put("useSSL", "false");

            @SuppressLint("DefaultLocale") String connectionString = String.format("jdbc:mysql://%s:%d/%s", DB_IP, DB_PORT, DB_NAME);
            return DriverManager.getConnection(connectionString, props);
        } catch (ClassNotFoundException | SQLException e) {
            Log.e(TAG, "Connection error", e);
            throw e;
        }
    }

    // Test Connection Method
    @SuppressLint("StaticFieldLeak")
    public void testConnection(final DatabaseCallback callback) {
        new AsyncTask<Void, Void, ConnectionResult>() {
            @Override
            protected ConnectionResult doInBackground(Void... voids) {
                ConnectionResult result = new ConnectionResult();
                try (Connection conn = getConnection()) {
                    result.setSuccess(true);
                    result.setMessage("Database connection successful");
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("Connection failed: " + e.getMessage());
                }
                return result;
            }

            @Override
            protected void onPostExecute(ConnectionResult result) {
                callback.onResult(result.isSuccess(), result.getMessage(), result.getUserId());
            }
        }.execute();
    }

    // User Login Method
    @SuppressLint("StaticFieldLeak")
    public void loginUser(final String username, final String password, final DatabaseCallback callback) {
        new AsyncTask<Void, Void, ConnectionResult>() {
            @Override
            protected ConnectionResult doInBackground(Void... voids) {
                ConnectionResult result = new ConnectionResult();
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "SELECT id, status FROM users WHERE email = ? AND password = ?")) {

                    String hashedPassword = hashPassword(password);
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String status = rs.getString("status");
                            if ("approved".equals(status)) {
                                result.setSuccess(true);
                                result.setMessage("Login successful");
                                result.setUserId(rs.getInt("id"));
                            } else {
                                result.setSuccess(false);
                                result.setMessage("Account pending approval");
                                result.setUserId(-1);
                            }
                        } else {
                            result.setSuccess(false);
                            result.setMessage("Invalid username or password");
                            result.setUserId(-1);
                        }
                    }
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("Login error: " + e.getMessage());
                    result.setUserId(-1);
                    Log.e(TAG, "Login error", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(ConnectionResult result) {
                callback.onResult(result.isSuccess(), result.getMessage(), result.getUserId());
            }
        }.execute();
    }
    // User Registration Method
    @SuppressLint("StaticFieldLeak")
    public void registerUser(final String phone, final String email,
                             final String password, final String branchName,
                             final String aadhar, final String address,
                             final DatabaseCallback callback) {
        new AsyncTask<Void, Void, ConnectionResult>() {
            @Override
            protected ConnectionResult doInBackground(Void... voids) {
                ConnectionResult result = new ConnectionResult();
                Connection conn = null;
                PreparedStatement pstmt = null;
                ResultSet rs = null;

                try {
                    conn = getConnection();

                    // First, get the branch ID from branch name
                    String getBranchIdSql = "SELECT id FROM branch WHERE branch_name = ?";
                    pstmt = conn.prepareStatement(getBranchIdSql);
                    pstmt.setString(1, branchName);
                    rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        result.setSuccess(false);
                        result.setMessage("Invalid branch selected");
                        return result;
                    }

                    int branchId = rs.getInt("id");

                    // Close the previous PreparedStatement
                    pstmt.close();

                    // Now insert the user with the branch ID
                    String insertUserSql = "INSERT INTO users (phone, email, password, bid, aadhar, address, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'pending')";

                    pstmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);

                    String hashedPassword = hashPassword(password);
                    pstmt.setString(1, phone);
                    pstmt.setString(2, email);
                    pstmt.setString(3, hashedPassword);
                    pstmt.setInt(4, branchId);
                    pstmt.setString(5, aadhar);
                    pstmt.setString(6, address);

                    int rowsAffected = pstmt.executeUpdate();

                    if (rowsAffected > 0) {
                        // Get the generated user ID
                        rs = pstmt.getGeneratedKeys();
                        if (rs.next()) {
                            result.setSuccess(true);
                            result.setMessage("Registration successful");
                            result.setUserId(rs.getInt(1));
                        }
                    } else {
                        result.setSuccess(false);
                        result.setMessage("Registration failed");
                    }
                } catch (SQLException e) {
                    result.setSuccess(false);
                    result.setMessage(Objects.requireNonNull(e.getMessage()).contains("Duplicate")
                            ? "Email or phone number already exists"
                            : "Registration error: " + e.getMessage());
                    Log.e(TAG, "Registration error", e);
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("Unexpected error: " + e.getMessage());
                    Log.e(TAG, "Unexpected error", e);
                } finally {
                    // Close resources in reverse order
                    try {
                        if (rs != null) rs.close();
                        if (pstmt != null) pstmt.close();
                        if (conn != null) conn.close();
                    } catch (SQLException e) {
                        Log.e(TAG, "Error closing database resources", e);
                    }
                }
                return result;
            }

            @Override
            protected void onPostExecute(ConnectionResult result) {
                callback.onResult(result.isSuccess(), result.getMessage(), result.getUserId());
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public void insertLog(final String sourceMac, final String destinationMac,
                          final String filename, final DatabaseCallback callback) {
        int sender_id = -1;
        int receiver_id = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sender_id = Connect.myUserId;
            receiver_id = Connect.targetUserId;
        }
        int finalSender_id = sender_id;
        int finalReceiver_id = receiver_id;
        new AsyncTask<Void, Void, ConnectionResult>() {
            @Override
            protected ConnectionResult doInBackground(Void... voids) {
                ConnectionResult result = new ConnectionResult();
                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "INSERT INTO logs (sender_id, receiver_id, source_mac, destination_mac, filename) VALUES (?, ?, ?, ?, ?)")) {

                    pstmt.setInt(1, finalSender_id);
                    pstmt.setInt(2, finalReceiver_id);
                    pstmt.setString(3, sourceMac);
                    pstmt.setString(4, destinationMac);
                    pstmt.setString(5, filename);

                    int rowsAffected = pstmt.executeUpdate();
                    result.setSuccess(rowsAffected > 0);
                    result.setMessage(rowsAffected > 0 ? "Log inserted successfully" : "Failed to insert log");
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("Log insertion error: " + e.getMessage());
                    Log.e(TAG, "Log insertion error", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(ConnectionResult result) {
                callback.onResult(result.isSuccess(), result.getMessage(), result.getUserId());
            }
        }.execute();
    }

    // Internal Result Handling Class
    static class ConnectionResult {
        private boolean success;
        private String message;
        private int userId;
        private ArrayList<String> branchNames;
        public void setBranchNames(ArrayList<String> branchNames) { this.branchNames = branchNames; }
        public ArrayList<String> getBranchNames() { return branchNames; }

        public void setSuccess(boolean success) { this.success = success; }
        public boolean isSuccess() { return success; }

        public void setMessage(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setUserId(int userId) { this.userId = userId; }
        public int getUserId() { return userId; }
    }
    @SuppressLint("StaticFieldLeak")
    public void fetchBranches(final BranchCallback callback) {
        new AsyncTask<Void, Void, ConnectionResult>() {
            @Override
            protected ConnectionResult doInBackground(Void... voids) {
                ConnectionResult result = new ConnectionResult();
                ArrayList<String> branchNames = new ArrayList<>();

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT branch_name FROM branch");
                     ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        branchNames.add(rs.getString("branch_name"));
                    }

                    result.setSuccess(true);
                    result.setMessage("Fetched " + branchNames.size() + " branches");
                    result.setBranchNames(branchNames);
                } catch (Exception e) {
                    result.setSuccess(false);
                    result.setMessage("Error fetching branches: " + e.getMessage());
                    Log.e(TAG, "Branch fetch error", e);
                }
                return result;
            }

            @Override
            protected void onPostExecute(ConnectionResult result) {
                callback.onBranchResult(
                        result.isSuccess(),
                        result.getBranchNames(),
                        result.getMessage()
                );
            }
        }.execute();
    }
}