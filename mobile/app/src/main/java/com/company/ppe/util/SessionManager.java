package com.company.ppe.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

public final class SessionManager {
  private static final String TAG = "SessionManager";
  private static final String KEY_TOKEN = "auth_token";
  private static final String KEY_USERNAME = "user_name";
  private static final String KEY_ROLE = "user_role";

  private SessionManager() {}

  public static void save(Context ctx, String token, String userName) {
    Prefs.putString(ctx, KEY_TOKEN, token);
    Prefs.putString(ctx, KEY_USERNAME, userName != null ? userName : "");
  }

  public static String token(Context ctx) {
    return Prefs.getString(ctx, KEY_TOKEN, "");
  }

  public static String userName(Context ctx) {
    return Prefs.getString(ctx, KEY_USERNAME, "");
  }

  public static boolean isLoggedIn(Context ctx) {
    String t = token(ctx);
    return t != null && !t.isEmpty() && isTokenValid(ctx);
  }

  public static boolean isTokenValid(Context ctx) {
    String token = token(ctx);
    if (token == null || token.isEmpty()) {
      return false;
    }

    try {
      // Basic JWT structure validation
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        Log.w(TAG, "Invalid JWT token structure");
        return false;
      }

      // Decode payload to check expiry
      String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
      JSONObject json = new JSONObject(payload);
      
      if (json.has("exp")) {
        long exp = json.getLong("exp");
        long now = System.currentTimeMillis() / 1000;
        
        if (now >= exp) {
          Log.w(TAG, "Token expired");
          clear(ctx); // Auto-clear expired token
          return false;
        }
      }
      
      return true;
    } catch (Exception e) {
      Log.e(TAG, "Token validation failed", e);
      return false;
    }
  }

  public static void clear(Context ctx) {
    Prefs.putString(ctx, KEY_TOKEN, "");
    Prefs.putString(ctx, KEY_USERNAME, "");
    Prefs.putString(ctx, KEY_ROLE, "");
  }

  public static void setRole(Context ctx, String role) {
    Prefs.putString(ctx, KEY_ROLE, role != null ? role : "");
  }

  public static String role(Context ctx) {
    return Prefs.getString(ctx, KEY_ROLE, "");
  }
}


