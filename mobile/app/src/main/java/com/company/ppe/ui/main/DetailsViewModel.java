package com.company.ppe.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.company.ppe.data.network.dto.ItemDetail;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.ui.common.UiState;

public class DetailsViewModel extends ViewModel {
  private final AppRepository repository;
  private final MutableLiveData<UiState<ItemDetail>> state = new MutableLiveData<>();

  public DetailsViewModel(AppRepository repository) { this.repository = repository; }

  public LiveData<UiState<ItemDetail>> getState() { return state; }

  public void load(String id) {
    state.postValue(UiState.loading());
    repository.fetchItemDetail(id, new AppRepository.ItemDetailCallback() {
      @Override public void onSuccess(ItemDetail data) { state.postValue(UiState.success(data)); }
      @Override public void onError(Throwable t) { state.postValue(UiState.error(t.getMessage())); }
    });
  }
}


