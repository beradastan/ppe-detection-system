package com.company.ppe.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.navigation.Navigation;
import com.company.ppe.R;
import com.company.ppe.util.SessionManager;

import com.company.ppe.databinding.FragmentMainBinding;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.ui.common.AppViewModelFactory;

public class MainFragment extends Fragment {
    private FragmentMainBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ApiService api = NetworkModule.api();
        AppRepository repo = new AppRepository(api);
        AppViewModelFactory factory = new AppViewModelFactory(repo);
        MainViewModel vm = new ViewModelProvider(this, factory).get(MainViewModel.class);

        vm.getStatus().observe(getViewLifecycleOwner(), value -> binding.title.setText(value));
        vm.load();

        MenuHost host = requireActivity();
        host.addMenuProvider(new MenuProvider() {
            @Override public void onCreateMenu(Menu menu, MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_dashboard, menu);
            }
            @Override public boolean onMenuItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.action_logout) {
                    SessionManager.clear(requireContext());
                    Navigation.findNavController(requireView()).navigate(R.id.LoginFragment);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


