package com.company.ppe.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.LogEntry;
import com.company.ppe.data.network.dto.LogsResponse;
import com.company.ppe.utils.StatusTranslator;
import com.company.ppe.data.network.dto.SimpleUser;
import com.company.ppe.data.network.dto.SupervisorInfo;
import com.company.ppe.data.network.dto.TestUsersResponse;
import com.company.ppe.data.network.dto.RegisterRequest;
import com.company.ppe.data.network.dto.RegisterResponse;
import com.company.ppe.util.SessionManager;
import com.company.ppe.ui.admin.UsersAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUsersFragment extends Fragment {
  private EditText inputUsername, inputPassword, inputFullName;
  private Spinner spinnerRole, spinnerSupervisor;
  private Button btnCreate;
  private Button btnToggleCreate;
  private ProgressBar progressCreate;
  private TextView textError;
  private android.view.View cardCreate;
  private RecyclerView recyclerView;
  private UsersAdapter adapter;
  private EditText inputSearchUser;
  
  private List<SimpleUser> users = new ArrayList<>();
  private List<SupervisorInfo> supervisors = new ArrayList<>();
  private String[] roles = {"worker", "supervisor", "admin"};

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_users, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    inputUsername = view.findViewById(R.id.inputUsername);
    inputPassword = view.findViewById(R.id.inputPassword);
    inputFullName = view.findViewById(R.id.inputFullName);
    spinnerRole = view.findViewById(R.id.spinnerRole);
    spinnerSupervisor = view.findViewById(R.id.spinnerSupervisor);
    btnCreate = view.findViewById(R.id.btnCreate);
    btnToggleCreate = view.findViewById(R.id.btnToggleCreate);
    cardCreate = view.findViewById(R.id.cardCreate);
    progressCreate = view.findViewById(R.id.progressCreate);
    textError = view.findViewById(R.id.textError);
    recyclerView = view.findViewById(R.id.recyclerView);
    inputSearchUser = view.findViewById(R.id.inputSearchUser);

    // Supervisor rolü ile giriş yapıldıysa oluşturma formunu tamamen gizle
    String roleOfCurrentUser = SessionManager.role(requireContext());
    if (roleOfCurrentUser != null && roleOfCurrentUser.equalsIgnoreCase("supervisor")) {
      if (btnToggleCreate != null) btnToggleCreate.setVisibility(View.GONE);
      if (cardCreate != null) cardCreate.setVisibility(View.GONE);
    }

    ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, roles);
    roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerRole.setAdapter(roleAdapter);

    adapter = new UsersAdapter(users, this::onUserClick);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    // role change toggles supervisor spinner visibility
    spinnerRole.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
        String role = roles[pos];
        view.findViewById(R.id.spinnerSupervisor).setVisibility("worker".equals(role) ? View.VISIBLE : View.GONE);
      }
      @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    });

    // search filter
    inputSearchUser.addTextChangedListener(new android.text.TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override public void afterTextChanged(android.text.Editable s) { applyUserFilter(); }
    });

    btnCreate.setOnClickListener(v -> createUser());
    btnToggleCreate.setOnClickListener(v -> toggleCreateCard());
    
    loadSupervisors();
    loadUsers();
  }

  private void applyUserFilter() {
    String q = inputSearchUser.getText() != null ? inputSearchUser.getText().toString().trim().toLowerCase() : "";
    java.util.List<SimpleUser> filtered = new java.util.ArrayList<>();
    for (SimpleUser u : users) {
      String uname = u.username != null ? u.username.toLowerCase() : "";
      if (q.length() == 0 || uname.contains(q)) filtered.add(u);
    }
    users.clear();
    users.addAll(filtered);
    adapter.notifyDataSetChanged();
  }

  private void onUserClick(SimpleUser u) {
    // Backend filtre desteklemiyor: /logs'u sayfa sayfa gezip ilgili kullanıcıya ait en güncel logu bul
    fetchLatestLogForUser(u, 200, 0);
  }

  private void showLogBottomSheet(LogEntry found) {
    android.view.View sheet = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_log_detail, null, false);
    android.widget.TextView textUserRole = sheet.findViewById(R.id.textUserRole);
    android.widget.TextView textTimestamp = sheet.findViewById(R.id.textTimestamp);
    android.widget.TextView pillPass = sheet.findViewById(R.id.pillPass);
    android.widget.TextView pillStatus = sheet.findViewById(R.id.pillStatus);
    android.widget.ImageView imageFrame = sheet.findViewById(R.id.imageFrame);
    android.widget.TextView textMissingRequired = sheet.findViewById(R.id.textMissingRequired);
    android.widget.TextView textMissingOptional = sheet.findViewById(R.id.textMissingOptional);
    android.widget.TextView textDetected = sheet.findViewById(R.id.textDetected);
    android.widget.Button btnClose = sheet.findViewById(R.id.btnClose);

    textUserRole.setText(found.username + " • " + found.user_role);
    textTimestamp.setText("Tarih: " + found.timestamp);
    pillPass.setText(found.can_pass ? "GEÇTİ" : "RED");
    pillStatus.setText(found.status != null ? StatusTranslator.translate(found.status) : "-");

    if (found.frame_image != null && found.frame_image.startsWith("data:image")) {
      try {
        String b64 = found.frame_image.substring(found.frame_image.indexOf(',') + 1);
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bmp != null) { imageFrame.setVisibility(View.VISIBLE); imageFrame.setImageBitmap(bmp); }
      } catch (Exception ignored) {}
    }

    // Translate missing required equipment names
    String missingRequiredText = "-";
    if (found.missing_required != null && !found.missing_required.isEmpty()) {
      java.util.List<String> translatedRequired = new java.util.ArrayList<>();
      for (String item : found.missing_required) {
        translatedRequired.add(com.company.ppe.utils.EquipmentTranslator.translate(item));
      }
      missingRequiredText = android.text.TextUtils.join(", ", translatedRequired);
    }
    textMissingRequired.setText("Eksik (zorunlu): " + missingRequiredText);
    
    // Translate missing optional equipment names
    String missingOptionalText = "-";
    if (found.missing_optional != null && !found.missing_optional.isEmpty()) {
      java.util.List<String> translatedOptional = new java.util.ArrayList<>();
      for (String item : found.missing_optional) {
        translatedOptional.add(com.company.ppe.utils.EquipmentTranslator.translate(item));
      }
      missingOptionalText = android.text.TextUtils.join(", ", translatedOptional);
    }
    textMissingOptional.setText("Eksik (opsiyonel): " + missingOptionalText);

    String detectedText = "-";
    try {
      if (found.detected_items != null) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Object it : found.detected_items) {
          if (it instanceof java.util.Map) {
            Object name = ((java.util.Map<?, ?>) it).get("class_name");
            if (name != null) names.add(com.company.ppe.utils.EquipmentTranslator.translate(String.valueOf(name)));
          }
        }
        if (!names.isEmpty()) detectedText = android.text.TextUtils.join(", ", names);
      }
    } catch (Exception ignored) {}
    textDetected.setText("Tespitler: " + detectedText);


    com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
    dialog.setContentView(sheet);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    dialog.show();
  }

  private void fetchLatestLogForUser(SimpleUser u, int limit, int offset) {
    ApiService api = NetworkModule.api();
    api.getLogs(limit, offset).enqueue(new Callback<LogsResponse>() {
      @Override public void onResponse(@NonNull Call<LogsResponse> call, @NonNull Response<LogsResponse> response) {
        if (!response.isSuccessful() || response.body() == null || response.body().logs == null) return;
        LogsResponse resp = response.body();
        LogEntry found = null;
        for (LogEntry e : resp.logs) { if (e.user_id == u.id) { found = e; break; } }
        if (found != null) {
          showLogBottomSheet(found);
          return;
        }
        int loaded = resp.logs.size();
        boolean hasMore = (offset + loaded) < resp.total_count;
        if (hasMore && loaded > 0) {
          fetchLatestLogForUser(u, limit, offset + loaded);
        } else {
          try {
            android.widget.Toast.makeText(requireContext(), "Seçilen kullanıcıya ait log bulunamadı", android.widget.Toast.LENGTH_SHORT).show();
          } catch (Exception ignored) { }
        }
      }
      @Override public void onFailure(@NonNull Call<LogsResponse> call, @NonNull Throwable t) {
        try {
          android.widget.Toast.makeText(requireContext(), "Loglar alınamadı: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) { }
      }
    });
  }

  private void loadSupervisors() {
    ApiService api = NetworkModule.api();
    api.getSupervisors().enqueue(new Callback<List<SupervisorInfo>>() {
      @Override
      public void onResponse(@NonNull Call<List<SupervisorInfo>> call, @NonNull Response<List<SupervisorInfo>> response) {
        if (response.isSuccessful() && response.body() != null) {
          supervisors = response.body();
          List<String> names = new ArrayList<>();
          for (SupervisorInfo s : supervisors) {
            String name = !TextUtils.isEmpty(s.full_name) ? s.full_name + " (" + s.username + ")" : s.username;
            names.add(name);
          }
          ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, names);
          adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
          spinnerSupervisor.setAdapter(adapter);
        }
      }

      @Override
      public void onFailure(@NonNull Call<List<SupervisorInfo>> call, @NonNull Throwable t) {
        // Silent fail
      }
    });
  }

  private void loadUsers() {
    ApiService api = NetworkModule.api();
    api.getTestUsers().enqueue(new Callback<TestUsersResponse>() {
      @Override
      public void onResponse(@NonNull Call<TestUsersResponse> call, @NonNull Response<TestUsersResponse> response) {
        if (response.isSuccessful() && response.body() != null && response.body().users != null) {
          users.clear();
          users.addAll(response.body().users);
          adapter.notifyDataSetChanged();
        }
      }

      @Override
      public void onFailure(@NonNull Call<TestUsersResponse> call, @NonNull Throwable t) {
        // Silent fail
      }
    });
  }

  private void createUser() {
    String username = inputUsername.getText().toString().trim();
    String password = inputPassword.getText().toString();
    String fullName = inputFullName.getText().toString().trim();
    String role = roles[spinnerRole.getSelectedItemPosition()];
    
    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
      showError("Kullanıcı adı ve şifre zorunludur");
      return;
    }

    Integer supervisorId = null;
    if ("worker".equals(role) && !supervisors.isEmpty()) {
      supervisorId = supervisors.get(spinnerSupervisor.getSelectedItemPosition()).id;
    }

    progressCreate.setVisibility(View.VISIBLE);
    btnCreate.setEnabled(false);
    textError.setVisibility(View.GONE);

    RegisterRequest req = new RegisterRequest(username, password, role, null,
        TextUtils.isEmpty(fullName) ? null : fullName, supervisorId);

    ApiService api = NetworkModule.api();
    api.register(req).enqueue(new Callback<RegisterResponse>() {
      @Override
      public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
        progressCreate.setVisibility(View.GONE);
        btnCreate.setEnabled(true);
        
        if (response.isSuccessful()) {
          inputUsername.setText("");
          inputPassword.setText("");
          inputFullName.setText("");
          loadUsers(); // Refresh list
          // Formu kapat
          toggleCreateCard(false);

          // Show QR (if available) like Flutter
          RegisterResponse body = response.body();
          if (body != null && body.user != null) {
            String qrB64 = body.user.qr_image_base64;
            if (qrB64 != null && !qrB64.isEmpty()) {
              try {
                byte[] bytes = android.util.Base64.decode(qrB64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                  android.widget.ImageView iv = new android.widget.ImageView(requireContext());
                  iv.setAdjustViewBounds(true);
                  iv.setImageBitmap(bmp);
                  new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                      .setTitle("Kullanıcı Oluşturuldu (QR)")
                      .setView(iv)
                      .setPositiveButton("Kapat", (d, w) -> d.dismiss())
                      .show();
                }
              } catch (Exception ignored) {}
            }
          }
        } else {
          showError("Kullanıcı oluşturulamadı: " + response.code());
        }
      }

      @Override
      public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
        progressCreate.setVisibility(View.GONE);
        btnCreate.setEnabled(true);
        showError("Ağ hatası: " + t.getMessage());
      }
    });
  }

  private void showError(String msg) {
    textError.setText(msg);
    textError.setVisibility(View.VISIBLE);
  }

  private void toggleCreateCard() {
    if (cardCreate == null) return;
    int vis = cardCreate.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
    cardCreate.setVisibility(vis);
    if (btnToggleCreate != null) {
      btnToggleCreate.setText(vis == View.VISIBLE ? "Yeni Kullanıcı Formunu Gizle" : "Yeni Kullanıcı Ekle");
    }
  }

  private void toggleCreateCard(boolean show) {
    if (cardCreate == null) return;
    cardCreate.setVisibility(show ? View.VISIBLE : View.GONE);
    if (btnToggleCreate != null) {
      btnToggleCreate.setText(show ? "Yeni Kullanıcı Formunu Gizle" : "Yeni Kullanıcı Ekle");
    }
  }
}
