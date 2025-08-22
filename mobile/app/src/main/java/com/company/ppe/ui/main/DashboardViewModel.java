package com.company.ppe.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.company.ppe.data.network.dto.Item;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.ui.common.UiState;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends ViewModel {
  private final AppRepository repository;
  private final MutableLiveData<UiState<List<Item>>> state = new MutableLiveData<>();
  private final List<Item> accumulator = new ArrayList<>();

  private int currentPage = 1;
  private final int pageSize = 20;
  private boolean isLoading = false;
  private boolean hasMore = true;
  private String currentQuery = "";

  public DashboardViewModel(AppRepository repository) {
    this.repository = repository;
  }

  public LiveData<UiState<List<Item>>> getState() { return state; }

  public void loadFirst(String query) {
    currentQuery = query != null ? query : "";
    currentPage = 1;
    hasMore = true;
    accumulator.clear();
    fetch(true);
  }

  public void refresh() {
    loadFirst(currentQuery);
  }

  public void loadNext() {
    if (isLoading || !hasMore) return;
    currentPage += 1;
    fetch(false);
  }

  private void fetch(boolean showInitialLoading) {
    isLoading = true;
    if (showInitialLoading) state.postValue(UiState.loading());
    repository.fetchItems(currentPage, pageSize, currentQuery, new AppRepository.ItemsCallback() {
      @Override public void onSuccess(List<Item> data, boolean _hasMore) {
        hasMore = _hasMore;
        accumulator.addAll(data);
        state.postValue(UiState.success(new ArrayList<>(accumulator)));
        isLoading = false;
      }
      @Override public void onError(Throwable t) {
        state.postValue(UiState.error(t.getMessage()));
        isLoading = false;
      }
    });
  }
}


