package com.company.ppe.data.network;

import com.company.ppe.data.network.dto.LoginRequest;
import com.company.ppe.data.network.dto.LoginResponse;
import com.company.ppe.data.network.dto.PagedResponse;
import com.company.ppe.data.network.dto.Item;
import com.company.ppe.data.network.dto.ItemDetail;
import com.company.ppe.data.network.dto.DetectionResponse;
import com.company.ppe.data.network.dto.FrameRequest;
import okhttp3.ResponseBody;
import com.company.ppe.data.network.dto.LogsResponse;
import com.company.ppe.data.network.dto.TestUsersResponse;
import com.company.ppe.data.network.dto.SupervisorInfo;
import com.company.ppe.data.network.dto.RegisterRequest;
import com.company.ppe.data.network.dto.RegisterResponse;
import com.company.ppe.data.network.dto.UserMeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Path;

public interface ApiService {
    @GET("status")
    Call<String> getStatus();

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @GET("auth/random_jwt")
    Call<LoginResponse> randomJwt();

    @GET("items")
    Call<PagedResponse<Item>> getItems(
      @Query("page") int page,
      @Query("size") int size,
      @Query("q") String query
    );

    @GET("items/{id}")
    Call<ItemDetail> getItemDetail(
      @Path("id") String id
    );

    @POST("process_frame")
    Call<DetectionResponse> processFrame(@Body FrameRequest body);

    @GET("user/qr")
    Call<ResponseBody> getUserQr();

    @GET("user/me")
    Call<UserMeResponse> getMe();

    @GET("health")
    Call<ResponseBody> health();

    @GET("logs")
    Call<LogsResponse> getLogs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("logs/stats")
    Call<java.util.Map<String, Object>> getLogStats();

    @GET("test/users")
    Call<TestUsersResponse> getTestUsers();

    @GET("users/supervisors")
    Call<java.util.List<SupervisorInfo>> getSupervisors();

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest body);
}


