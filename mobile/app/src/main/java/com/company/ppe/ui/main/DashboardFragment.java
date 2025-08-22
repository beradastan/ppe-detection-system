package com.company.ppe.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.ppe.R;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.databinding.FragmentDashboardBinding;
import com.company.ppe.ui.common.AppViewModelFactory;
import com.company.ppe.ui.common.UiState;

public class DashboardFragment extends Fragment {
  private FragmentDashboardBinding binding;
  private DashboardViewModel vm;
  private ItemAdapter adapter;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = FragmentDashboardBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    AppRepository repo = new AppRepository(NetworkModule.api());
    vm = new ViewModelProvider(this, new AppViewModelFactory(repo)).get(DashboardViewModel.class);

    adapter = new ItemAdapter(item -> {
      Bundle args = new Bundle();
      args.putString("itemId", item.id);
      Navigation.findNavController(requireView()).navigate(R.id.DetailsFragment, args);
    });

    binding.list.setAdapter(adapter);
    LinearLayoutManager lm = new LinearLayoutManager(requireContext());
    binding.list.setLayoutManager(lm);

    binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
        super.onScrolled(rv, dx, dy);
        int visible = lm.getChildCount();
        int total = lm.getItemCount();
        int first = lm.findFirstVisibleItemPosition();
        if (dy > 0 && (visible + first) >= total - 4) {
          vm.loadNext();
        }
      }
    });

    binding.swipe.setOnRefreshListener(vm::refresh);

    binding.search.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(String q) {
        vm.loadFirst(q);
        return true;
      }
      @Override public boolean onQueryTextChange(String q) { return false; }
    });

    vm.getState().observe(getViewLifecycleOwner(), this::render);
    vm.loadFirst("");
  }

  private void render(UiState<java.util.List<com.company.ppe.data.network.dto.Item>> s) {
    if (s == null) return;
    binding.progress.setVisibility(s.loading ? View.VISIBLE : View.GONE);
    binding.swipe.setRefreshing(false);

    if (s.error != null) {
      binding.empty.setText("Hata: " + s.error);
      binding.empty.setVisibility(View.VISIBLE);
      return;
    }

    if (s.data != null) {
      adapter.submitList(s.data);
      boolean empty = s.data.isEmpty();
      binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}


