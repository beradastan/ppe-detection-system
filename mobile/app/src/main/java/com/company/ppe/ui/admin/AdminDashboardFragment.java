package com.company.ppe.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.company.ppe.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminDashboardFragment extends Fragment {
  private TabLayout tabLayout;
  private ViewPager2 viewPager;
  private com.google.android.material.appbar.MaterialToolbar toolbar;
  private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;
  private AdminPagerAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    toolbar = view.findViewById(R.id.toolbar);
    viewPager = view.findViewById(R.id.pager);
    bottomNav = view.findViewById(R.id.bottomNav);

    adapter = new AdminPagerAdapter(requireActivity());
    viewPager.setAdapter(adapter);

    // Alttaki menü ile ViewPager senkronizasyonu
    bottomNav.setOnItemSelectedListener(item -> {
      if (item.getItemId() == R.id.nav_logs) { viewPager.setCurrentItem(0, true); return true; }
      if (item.getItemId() == R.id.nav_stats) { viewPager.setCurrentItem(1, true); return true; }
      if (item.getItemId() == R.id.nav_users) { viewPager.setCurrentItem(2, true); return true; }
      return false;
    });

    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override public void onPageSelected(int position) {
        super.onPageSelected(position);
        switch (position) {
          case 0: bottomNav.setSelectedItemId(R.id.nav_logs); break;
          case 1: bottomNav.setSelectedItemId(R.id.nav_stats); break;
          case 2: bottomNav.setSelectedItemId(R.id.nav_users); break;
        }
        updateToolbarTitle(position);
      }
    });

    // Başlangıç başlığı ve seçimleri ayarla
    updateToolbarTitle(viewPager.getCurrentItem());
    bottomNav.setSelectedItemId(R.id.nav_logs);

    // Add logout menu
    toolbar.inflateMenu(R.menu.menu_dashboard);
    toolbar.setOnMenuItemClickListener(item -> {
      if (item.getItemId() == R.id.action_logout) {
        com.company.ppe.util.SessionManager.clear(requireContext());
        // Return to Splash and clear back stack like Flutter logout
        androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host);
        androidx.navigation.NavOptions opts = new androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.SplashFragment, true)
            .build();
        nav.navigate(R.id.SplashFragment, null, opts);
        return true;
      } else if (item.getItemId() == R.id.action_search_toggle) {
        if (viewPager.getCurrentItem() == 0 && adapter != null) {
          Fragment f = adapter.getFragment(0);
          if (f instanceof AdminLogsFragment) {
            ((AdminLogsFragment) f).toggleSearch();
          }
        }
        return true;
      }
      return false;
    });
  }

  private void updateToolbarTitle(int position) {
    if (toolbar == null) return;
    switch (position) {
      case 0:
        toolbar.setTitle("Kayıtlar");
        if (toolbar.getMenu().findItem(R.id.action_search_toggle) != null)
          toolbar.getMenu().findItem(R.id.action_search_toggle).setVisible(true);
        break;
      case 1:
        toolbar.setTitle("İstatistik");
        if (toolbar.getMenu().findItem(R.id.action_search_toggle) != null)
          toolbar.getMenu().findItem(R.id.action_search_toggle).setVisible(false);
        break;
      case 2:
        toolbar.setTitle("Kullanıcılar");
        if (toolbar.getMenu().findItem(R.id.action_search_toggle) != null)
          toolbar.getMenu().findItem(R.id.action_search_toggle).setVisible(false);
        break;
      default:
        toolbar.setTitle("Admin Panel");
        if (toolbar.getMenu().findItem(R.id.action_search_toggle) != null)
          toolbar.getMenu().findItem(R.id.action_search_toggle).setVisible(false);
        break;
    }
  }

  private static class AdminPagerAdapter extends FragmentStateAdapter {
    private final java.util.Map<Integer, Fragment> fragmentMap = new java.util.HashMap<>();
    AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
      super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
      Fragment f;
      switch (position) {
        case 0: f = new AdminLogsFragment(); break;
        case 1: f = new AdminStatsFragment(); break;
        case 2: f = new AdminUsersFragment(); break;
        default: f = new AdminLogsFragment(); break;
      }
      fragmentMap.put(position, f);
      return f;
    }

    @Override
    public int getItemCount() {
      return 3;
    }

    Fragment getFragment(int position) {
      return fragmentMap.get(position);
    }
  }
}


