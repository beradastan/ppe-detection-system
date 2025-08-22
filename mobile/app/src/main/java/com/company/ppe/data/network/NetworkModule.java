package com.company.ppe.data.network;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class NetworkModule {
    private static Retrofit retrofit;
    private static ApiService apiService;

    private NetworkModule() {}

    public static void init(Context context, String baseUrl) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Only log in debug builds for security
        logging.setLevel(com.company.ppe.BuildConfig.DEBUG ? 
            HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new TokenInterceptor(context))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static void reinit(Context context, String baseUrl) {
        init(context, baseUrl);
    }

    public static ApiService api() {
        if (apiService == null) {
            throw new IllegalStateException("NetworkModule.init çağrılmalı");
        }
        return apiService;
    }
}


