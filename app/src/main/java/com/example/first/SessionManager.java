package com.example.first;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;


public class SessionManager {
    // Shared preferences file name
    private static final String PREF_NAME = "AppPrefs";

    // Shared preferences keys
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";

    // Shared preferences object
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // Constructor
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int userId, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }


    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }


    public int getUserId() {
        return pref.getInt(KEY_USER_ID, -1);
    }


    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }


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