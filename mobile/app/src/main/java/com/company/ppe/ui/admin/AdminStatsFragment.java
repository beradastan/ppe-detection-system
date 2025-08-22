package com.company.ppe.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatsFragment extends Fragment {
  private TextView textTodayPassed, textTodayDenied, textTotalUsers;
  private ProgressBar progress;
  private SwipeRefreshLayout swipeRefresh;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_stats, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    textTodayPassed = view.findViewById(R.id.textTodayPassed);
    textTodayDenied = view.findViewById(R.id.textTodayDenied);
    textTotalUsers = view.findViewById(R.id.textTotalUsers);
    progress = view.findViewById(R.id.progress);
    swipeRefresh = view.findViewById(R.id.swipeRefresh);

    swipeRefresh.setOnRefreshListener(this::loadStats);
    loadStats();
  }

  private void loadStats() {
    progress.setVisibility(View.VISIBLE);
    ApiService api = NetworkModule.api();
    api.getLogStats().enqueue(new Callback<Map<String, Object>>() {
      @Override
      public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
        progress.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        
        if (response.isSuccessful() && response.body() != null) {
          Map<String, Object> stats = response.body();
          int todayPassed = getIntValue(stats, "today_passed");
          int todayDenied = getIntValue(stats, "today_denied");
          int totalUsers = getIntValue(stats, "total_users");
          
          textTodayPassed.setText(String.valueOf(todayPassed));
          textTodayDenied.setText(String.valueOf(todayDenied));
          textTotalUsers.setText(String.valueOf(totalUsers));
        }
      }

      @Override
      public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
        progress.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
      }
    });
  }

  private int getIntValue(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }
}
