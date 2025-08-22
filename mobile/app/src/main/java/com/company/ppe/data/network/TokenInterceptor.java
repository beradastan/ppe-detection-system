package com.company.ppe.data.network;

import android.content.Context;

import com.company.ppe.util.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {
  private final Context appContext;

  public TokenInterceptor(Context context) {
    this.appContext = context.getApplicationContext();
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    String token = SessionManager.token(appContext);
    Request original = chain.request();
    Request.Builder builder = original.newBuilder();
    if (token != null && !token.isEmpty()) {
      builder.header("Authorization", "Bearer " + token);
    }
    return chain.proceed(builder.build());
  }
}


