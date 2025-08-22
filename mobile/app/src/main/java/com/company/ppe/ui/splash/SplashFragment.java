package com.company.ppe.ui.splash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.company.ppe.R;
import com.company.ppe.util.SessionManager;

public class SplashFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return new View(requireContext());
  }

  @Override
  public void onResume() {
    super.onResume();
    NavController nav = Navigation.findNavController(requireActivity(), R.id.nav_host);
    if (SessionManager.isLoggedIn(requireContext())) {
      String role = SessionManager.role(requireContext());
      if ("admin".equals(role)) {
        nav.navigate(R.id.AdminDashboardFragment);
      } else {
        nav.navigate(R.id.ModeSelectFragment);
      }
    } else {
      nav.navigate(R.id.LoginFragment);
    }
  }
}


