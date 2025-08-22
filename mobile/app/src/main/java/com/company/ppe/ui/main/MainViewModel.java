package com.company.ppe.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.company.ppe.data.repository.AppRepository;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<String> status = new MutableLiveData<>();
    private final AppRepository repository;

    public MainViewModel(AppRepository repository) {
        this.repository = repository;
    }

    public LiveData<String> getStatus() { return status; }

    public void load() {
        repository.fetchStatus(new AppRepository.StatusCallback() {
            @Override public void onSuccess(String s) { status.postValue(s); }
            @Override public void onError(Throwable t) { status.postValue("Hata: " + t.getMessage()); }
        });
    }
}


