package com.example.first;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Session manager to handle login sessions and user information
 */
public class SessionManager {
    // Shared preferences file name
    private static final String PREF_NAME = "AppPrefs";

    // Shared preferences keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";

    // Shared preferences object
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    // Constructor
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Create login session
     * @param userId User ID
     * @param email User email
     */
    public void createLoginSession(int userId, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    /**
     * Check login status
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get stored user ID
     * @return User ID or -1 if not found
     */
    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }

    /**
     * Get stored user email
     * @return User email or empty string if not found
     */
    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    /**
     * Clear session details and redirect to login
     */
    public void logout() {
        // Clear all data from shared preferences
        editor.clear();
        editor.apply();

        // Redirect to LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);
        // Add flags to clear activity stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}