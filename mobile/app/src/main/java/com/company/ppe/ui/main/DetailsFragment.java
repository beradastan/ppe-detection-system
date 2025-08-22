package com.company.ppe.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.repository.AppRepository;
import com.company.ppe.databinding.FragmentDetailsBinding;
import com.company.ppe.ui.common.AppViewModelFactory;
import com.company.ppe.ui.common.UiState;

public class DetailsFragment extends Fragment {
  private FragmentDetailsBinding binding;
  private DetailsViewModel vm;
  private String itemId;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = FragmentDetailsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    itemId = getArguments() != null ? getArguments().getString("itemId") : null;

    vm = new ViewModelProvider(this, new AppViewModelFactory(new AppRepository(NetworkModule.api())))
      .get(DetailsViewModel.class);

    binding.btnOpen.setOnClickListener(v -> {
      Object tag = binding.btnOpen.getTag();
      if (tag instanceof String) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse((String) tag)));
      }
    });

    vm.getState().observe(getViewLifecycleOwner(), this::render);
    if (itemId != null) vm.load(itemId);
  }

  private void render(UiState<com.company.ppe.data.network.dto.ItemDetail> s) {
    if (s == null) return;
    binding.progress.setVisibility(s.loading ? View.VISIBLE : View.GONE);
    binding.error.setVisibility(View.GONE);

    if (s.error != null) {
      binding.error.setText("Hata: " + s.error);
      binding.error.setVisibility(View.VISIBLE);
      return;
    }
    if (s.data != null) {
      Glide.with(this).load(s.data.imageUrl).centerCrop().into(binding.image);
      binding.title.setText(s.data.title);
      binding.subtitle.setText(s.data.subtitle);
      binding.description.setText(s.data.description != null ? s.data.description : "");
      binding.btnOpen.setTag(s.data.externalUrl);
      binding.btnOpen.setVisibility(s.data.externalUrl != null && !s.data.externalUrl.isEmpty() ? View.VISIBLE : View.GONE);
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}


