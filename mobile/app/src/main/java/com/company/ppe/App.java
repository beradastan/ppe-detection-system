package com.company.ppe;

import android.app.Application;
import android.content.SharedPreferences;

import com.company.ppe.data.network.NetworkModule;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Load persisted base URL if available (align with Flutter behavior)
        String defaultBase = BuildConfig.API_BASE_URL;
        if (defaultBase == null) defaultBase = "";
        SharedPreferences prefs = getSharedPreferences("ppe_prefs", 0);
        String baseUrl = prefs.getString("base_url", defaultBase);
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = defaultBase;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        NetworkModule.init(this, baseUrl);
    }
}


