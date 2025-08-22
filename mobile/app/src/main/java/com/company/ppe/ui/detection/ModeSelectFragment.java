package com.company.ppe.ui.detection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.UserMeResponse;
import com.company.ppe.util.SessionManager;

public class ModeSelectFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_mode_select, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    com.google.android.material.appbar.MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
    if (toolbar != null) {
      toolbar.inflateMenu(R.menu.menu_mode_select);
      toolbar.setOnMenuItemClickListener(item -> {
        if (item.getItemId() == R.id.action_logout) {
          SessionManager.clear(requireContext());
          Navigation.findNavController(view).navigate(R.id.LoginFragment);
          return true;
        }
        return false;
      });
    }
    String role = SessionManager.role(requireContext());
    // Hoş geldin <full_name> başlığını toolbar'a yaz
    ApiService api = NetworkModule.api();
    api.getMe().enqueue(new retrofit2.Callback<UserMeResponse>() {
      @Override public void onResponse(@androidx.annotation.NonNull retrofit2.Call<UserMeResponse> call, @androidx.annotation.NonNull retrofit2.Response<UserMeResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
          String fn = response.body().full_name;
          if (fn != null && !fn.trim().isEmpty()) {
            String proper = toTitleCase(fn);
            if (toolbar != null) toolbar.setTitle("Hoş Geldin " + proper);
          } else {
            String username = SessionManager.userName(requireContext());
            String proper = toTitleCase(username != null ? username : "");
            if (toolbar != null) toolbar.setTitle("Hoş Geldin " + proper);
          }
        } else {
          String username = SessionManager.userName(requireContext());
          String proper = toTitleCase(username != null ? username : "");
          if (toolbar != null) toolbar.setTitle("Hoş Geldin " + proper);
        }
      }
      @Override public void onFailure(@androidx.annotation.NonNull retrofit2.Call<UserMeResponse> call, @androidx.annotation.NonNull Throwable t) {
        String username = SessionManager.userName(requireContext());
        String proper = toTitleCase(username != null ? username : "");
        if (toolbar != null) toolbar.setTitle("Hoş Geldin " + proper);
      }
    });

    View cardCamera = view.findViewById(R.id.cardCamera);
    View cardQr = view.findViewById(R.id.cardQr);
    Button btnAdmin = view.findViewById(R.id.btnAdmin);

    cardCamera.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.ModeCameraFragment));
    cardQr.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.ModeQrFragment));
    
    if ("admin".equals(role) || "supervisor".equals(role)) {
      btnAdmin.setVisibility(View.VISIBLE);
      btnAdmin.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.AdminDashboardFragment));
    } else {
      btnAdmin.setVisibility(View.GONE);
    }
  }

  private String toTitleCase(String s) {
    if (s == null) return "";
    try {
      String[] parts = s.replace('_', ' ').trim().split("\\s+");
      StringBuilder b = new StringBuilder();
      for (String p : parts) {
        if (p.isEmpty()) continue;
        b.append(Character.toUpperCase(p.charAt(0)));
        if (p.length() > 1) b.append(p.substring(1));
        b.append(' ');
      }
      return b.toString().trim();
    } catch (Exception e) {
      return s;
    }
  }
}


