package com.company.ppe.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.LoginResponse;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.databinding.FragmentLoginBinding;
import com.company.ppe.ui.common.AppViewModelFactory;
import com.company.ppe.ui.common.UiState;
import com.company.ppe.util.SessionManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.company.ppe.R;

public class LoginFragment extends Fragment {
  private FragmentLoginBinding binding;
  private LoginViewModel vm;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = FragmentLoginBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ApiService api = NetworkModule.api();
    AppRepository repo = new AppRepository(api);
    vm = new ViewModelProvider(this, new AppViewModelFactory(repo)).get(LoginViewModel.class);

    binding.btnLogin.setOnClickListener(v -> {
      String email = String.valueOf(binding.inputUsername.getText());
      String pass = String.valueOf(binding.inputPassword.getText());
      if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
        showError("Kullanıcı adı ve şifre zorunludur");
        return;
      }
      vm.login(email, pass);
    });

    // Hızlı giriş ve kayıt ol kaldırıldı

    vm.getState().observe(getViewLifecycleOwner(), this::render);
  }

  private void render(UiState<LoginResponse> s) {
    if (s == null) return;
    if (s.loading) {
      binding.progress.setVisibility(View.VISIBLE);
      binding.textError.setVisibility(View.GONE);
      binding.btnLogin.setEnabled(false);
      return;
    }
    binding.progress.setVisibility(View.GONE);
    binding.btnLogin.setEnabled(true);

    if (s.error != null) {
      showError(s.error);
      return;
    }
    if (s.data != null) {
      SessionManager.save(requireContext(), s.data.access_token, s.data.user != null ? s.data.user.username : null);
      if (s.data.user != null) {
        SessionManager.setRole(requireContext(), s.data.user.role);
      }
      NavController nav = Navigation.findNavController(requireView());
      String role = (s.data.user != null && s.data.user.role != null) ? s.data.user.role.toLowerCase() : "";
      if ("worker".equals(role) || "supervisor".equals(role)) {
        nav.navigate(R.id.action_LoginFragment_to_ModeSelectFragment);
      } else {
        nav.navigate(R.id.AdminDashboardFragment);
      }
    }
  }

  // Hızlı giriş kaldırıldı

  private void showError(String msg) {
    binding.textError.setText(msg);
    binding.textError.setVisibility(View.VISIBLE);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}


