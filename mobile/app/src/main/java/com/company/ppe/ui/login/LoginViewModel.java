package com.company.ppe.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.company.ppe.data.network.dto.LoginResponse;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.ui.common.UiState;

public class LoginViewModel extends ViewModel {
    private final AppRepository repository;
    private final MutableLiveData<UiState<LoginResponse>> state = new MutableLiveData<>();

    public LoginViewModel(AppRepository repository) {
        this.repository = repository;
    }

    public LiveData<UiState<LoginResponse>> getState() { return state; }

    public void login(String email, String password) {
        state.postValue(UiState.loading());
        repository.login(email, password, new AppRepository.LoginCallback() {
            @Override public void onSuccess(LoginResponse res) { state.postValue(UiState.success(res)); }
            @Override public void onError(Throwable t) { state.postValue(UiState.error(t.getMessage())); }
        });
    }
}


