package com.company.ppe.ui.common;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.ui.main.MainViewModel;
import com.company.ppe.ui.login.LoginViewModel;
import com.company.ppe.ui.main.DashboardViewModel;
import com.company.ppe.ui.main.DetailsViewModel;

public class AppViewModelFactory implements ViewModelProvider.Factory {
    private final AppRepository repository;

    public AppViewModelFactory(AppRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(repository);
        }
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(repository);
        }
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(repository);
        }
        if (modelClass.isAssignableFrom(DetailsViewModel.class)) {
            return (T) new DetailsViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}


