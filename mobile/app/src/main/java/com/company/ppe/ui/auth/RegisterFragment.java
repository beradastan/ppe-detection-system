package com.company.ppe.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.RegisterRequest;
import com.company.ppe.data.network.dto.RegisterResponse;
import com.company.ppe.data.network.dto.SupervisorInfo;
import com.company.ppe.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {
  private EditText inputUsername, inputPassword, inputEmail, inputFullName;
  private Spinner spinnerRole, spinnerSupervisor;
  private Button btnRegister;
  private ProgressBar progress;
  private TextView textError;
  private View layoutSupervisor;
  
  private List<SupervisorInfo> supervisors = new ArrayList<>();
  private String[] roles = {"worker", "supervisor", "admin"};

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_register, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    inputUsername = view.findViewById(R.id.inputUsername);
    inputPassword = view.findViewById(R.id.inputPassword);
    inputEmail = view.findViewById(R.id.inputEmail);
    inputFullName = view.findViewById(R.id.inputFullName);
    spinnerRole = view.findViewById(R.id.spinnerRole);
    spinnerSupervisor = view.findViewById(R.id.spinnerSupervisor);
    btnRegister = view.findViewById(R.id.btnRegister);
    progress = view.findViewById(R.id.progress);
    textError = view.findViewById(R.id.textError);
    layoutSupervisor = view.findViewById(R.id.layoutSupervisor);

    ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
    roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerRole.setAdapter(roleAdapter);

    loadSupervisors();
    
    btnRegister.setOnClickListener(v -> doRegister());
  }

  private void loadSupervisors() {
    ApiService api = NetworkModule.api();
    api.getSupervisors().enqueue(new Callback<List<SupervisorInfo>>() {
      @Override
      public void onResponse(@NonNull Call<List<SupervisorInfo>> call, @NonNull Response<List<SupervisorInfo>> response) {
        if (response.isSuccessful() && response.body() != null) {
          supervisors = response.body();
          List<String> names = new ArrayList<>();
          for (SupervisorInfo s : supervisors) {
            String name = !TextUtils.isEmpty(s.full_name) ? s.full_name + " (" + s.username + ")" : s.username;
            names.add(name);
          }
          ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
          spinnerSupervisor.setAdapter(adapter);
        }
      }

      @Override
      public void onFailure(@NonNull Call<List<SupervisorInfo>> call, @NonNull Throwable t) {
        // Silent fail
      }
    });
  }

  private void doRegister() {
    String username = inputUsername.getText().toString().trim();
    String password = inputPassword.getText().toString();
    String email = inputEmail.getText().toString().trim();
    String fullName = inputFullName.getText().toString().trim();
    String role = roles[spinnerRole.getSelectedItemPosition()];
    
    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
      showError("Kullanıcı adı ve şifre zorunludur");
      return;
    }

    Integer supervisorId = null;
    if ("worker".equals(role) && !supervisors.isEmpty()) {
      supervisorId = supervisors.get(spinnerSupervisor.getSelectedItemPosition()).id;
    }

    progress.setVisibility(View.VISIBLE);
    btnRegister.setEnabled(false);
    textError.setVisibility(View.GONE);

    RegisterRequest req = new RegisterRequest(username, password, role, 
        TextUtils.isEmpty(email) ? null : email,
        TextUtils.isEmpty(fullName) ? null : fullName, 
        supervisorId);

    ApiService api = NetworkModule.api();
    api.register(req).enqueue(new Callback<RegisterResponse>() {
      @Override
      public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
        progress.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
        
        if (response.isSuccessful() && response.body() != null) {
          RegisterResponse resp = response.body();
          SessionManager.save(requireContext(), resp.access_token, resp.user.username);
          SessionManager.setRole(requireContext(), resp.user.role);
          Navigation.findNavController(requireView()).navigate(R.id.action_RegisterFragment_to_ModeSelectFragment);
        } else {
          showError("Kayıt başarısız: " + response.code());
        }
      }

      @Override
      public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
        progress.setVisibility(View.GONE);
        btnRegister.setEnabled(true);
        showError("Ağ hatası: " + t.getMessage());
      }
    });
  }

  private void showError(String msg) {
    textError.setText(msg);
    textError.setVisibility(View.VISIBLE);
  }
}
