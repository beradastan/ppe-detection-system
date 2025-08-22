package com.company.ppe.data.repository;

import androidx.annotation.NonNull;

import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.dto.LoginRequest;
import com.company.ppe.data.network.dto.LoginResponse;
import com.company.ppe.data.network.dto.PagedResponse;
import com.company.ppe.data.network.dto.Item;
import com.company.ppe.data.network.dto.ItemDetail;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppRepository {
    private final ApiService api;

    public interface StatusCallback {
        void onSuccess(String status);
        void onError(Throwable t);
    }

    public AppRepository(ApiService api) {
        this.api = api;
    }

    public void fetchStatus(StatusCallback callback) {
        api.getStatus().enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new RuntimeException("Response error"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }

    public interface LoginCallback {
        void onSuccess(LoginResponse res);
        void onError(Throwable t);
    }

    public void login(String email, String password, LoginCallback cb) {
        api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cb.onSuccess(response.body());
                } else {
                    cb.onError(new RuntimeException("Login başarısız"));
                }
            }
            @Override public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) { cb.onError(t); }
        });
    }

    public interface ItemsCallback {
        void onSuccess(java.util.List<Item> data, boolean hasMore);
        void onError(Throwable t);
    }

    public void fetchItems(int page, int size, String query, ItemsCallback cb) {
        api.getItems(page, size, query).enqueue(new Callback<PagedResponse<Item>>() {
            @Override public void onResponse(@NonNull Call<PagedResponse<Item>> call,
                                             @NonNull Response<PagedResponse<Item>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    PagedResponse<Item> body = resp.body();
                    boolean hasMore = body.page < body.totalPages;
                    cb.onSuccess(body.items != null ? body.items : new java.util.ArrayList<>(), hasMore);
                } else {
                    cb.onError(new RuntimeException("Liste alınamadı"));
                }
            }
            @Override public void onFailure(@NonNull Call<PagedResponse<Item>> call, @NonNull Throwable t) {
                cb.onError(t);
            }
        });
    }

    public interface ItemDetailCallback {
        void onSuccess(ItemDetail data);
        void onError(Throwable t);
    }

    public void fetchItemDetail(String id, ItemDetailCallback cb) {
        api.getItemDetail(id).enqueue(new Callback<ItemDetail>() {
            @Override public void onResponse(@NonNull Call<ItemDetail> call, @NonNull Response<ItemDetail> resp) {
                if (resp.isSuccessful() && resp.body() != null) cb.onSuccess(resp.body());
                else cb.onError(new RuntimeException("Detay alınamadı"));
            }
            @Override public void onFailure(@NonNull Call<ItemDetail> call, @NonNull Throwable t) {
                cb.onError(t);
            }
        });
    }
}


