package com.company.ppe.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.LogEntry;
import com.company.ppe.data.network.dto.LogsResponse;
import com.company.ppe.utils.EquipmentTranslator;
import com.company.ppe.utils.StatusTranslator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLogsFragment extends Fragment {
  private RecyclerView recyclerView;
  private SwipeRefreshLayout swipeRefresh;
  private ProgressBar progress;
  private TextView textEmpty;
  private LogsAdapter adapter;
  private android.widget.EditText inputSearch;
  private android.widget.Button btnPickFrom, btnPickTo, btnClear, btnRefresh, btnPickRange;
  private com.google.android.material.chip.Chip chipPassed, chipRejected, chipPartial;
  private View filterPanel;
  private com.google.android.material.chip.Chip chipQuickToday, chipQuickWeek;
  private android.widget.Spinner spinnerSort;
  
  private List<LogEntry> logs = new ArrayList<>();
  private int offset = 0;
  private final int limit = 50;
  private boolean hasMore = true;
  private boolean loading = false;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_admin_logs, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    recyclerView = view.findViewById(R.id.recyclerView);
    swipeRefresh = view.findViewById(R.id.swipeRefresh);
    progress = view.findViewById(R.id.progress);
    textEmpty = view.findViewById(R.id.textEmpty);
    inputSearch = view.findViewById(R.id.inputSearch);
    filterPanel = view.findViewById(R.id.filterPanel);
    spinnerSort = view.findViewById(R.id.spinnerSort);
    btnPickRange = view.findViewById(R.id.btnPickRange);
    btnClear = view.findViewById(R.id.btnClear);
    btnRefresh = view.findViewById(R.id.btnRefresh);
    chipPassed = view.findViewById(R.id.chipPassed);
    chipRejected = view.findViewById(R.id.chipRejected);
    chipPartial = view.findViewById(R.id.chipPartial);
    chipQuickToday = view.findViewById(R.id.chipQuickToday);
    chipQuickWeek = view.findViewById(R.id.chipQuickWeek);

    adapter = new LogsAdapter(logs, this::showLogDetail);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

    swipeRefresh.setOnRefreshListener(() -> loadLogs(true));

    // Filters
    inputSearch.addTextChangedListener(new android.text.TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override public void afterTextChanged(android.text.Editable s) { applyFilters(); }
    });
    btnClear.setOnClickListener(v -> { inputSearch.setText(""); fromDate = null; toDate = null; updateRangeButtonLabel(); selectAllChip(); applyFilters(); updateClearEnabled(); });
    btnRefresh.setOnClickListener(v -> loadLogs(true));
    if (btnPickFrom != null) btnPickFrom.setOnClickListener(v -> pickDate(true));
    if (btnPickTo != null) btnPickTo.setOnClickListener(v -> pickDate(false));
    if (btnPickRange != null) btnPickRange.setOnClickListener(v -> pickDateRange());
    setupSortSpinner();

    chipPassed.setOnCheckedChangeListener((b, checked) -> { if (checked) { statusFilter = 1; applyFilters(); }});
    chipPartial.setOnCheckedChangeListener((b, checked) -> { if (checked) { statusFilter = 3; applyFilters(); }});
    chipRejected.setOnCheckedChangeListener((b, checked) -> { if (checked) { statusFilter = 2; applyFilters(); }});
    chipQuickToday.setOnCheckedChangeListener((b, checked) -> { if (checked) { setQuickToday(); updateRangeButtonLabel(); applyFilters(); }});
    chipQuickWeek.setOnCheckedChangeListener((b, checked) -> { if (checked) { setQuickThisWeek(); updateRangeButtonLabel(); applyFilters(); }});
    // Başlangıçta buton etiketini ayarla
    updateRangeButtonLabel();
    updateClearEnabled();

    loadLogs(true);
  }

  // Toolbar menüsünden çağrılır
  public void toggleSearch() {
    View container = getView() != null ? getView().findViewById(R.id.searchContainer) : null;
    if (container == null) return;
    container.setVisibility(container.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
  }

  // Filters state
  private java.util.Date fromDate = null, toDate = null;
  // 1=pass,2=reject,3=partial; null = no status filter
  private Integer statusFilter = null;

  private void selectAllChip() {
    statusFilter = null;
    chipPassed.setChecked(false);
    chipPartial.setChecked(false);
    chipRejected.setChecked(false);
  }

  private void pickDate(boolean isFrom) {
    final java.util.Calendar cal = java.util.Calendar.getInstance();
    android.app.DatePickerDialog dlg = new android.app.DatePickerDialog(requireContext(), (v, y, m, d) -> {
      java.util.Calendar c = java.util.Calendar.getInstance();
      c.set(y, m, d, isFrom ? 0 : 23, isFrom ? 0 : 59, isFrom ? 0 : 59);
      if (isFrom) fromDate = c.getTime(); else toDate = c.getTime();
      updateRangeButtonLabel();
      applyFilters();
    }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
    dlg.show();
  }

  private void pickDateRange() {
    // Basit yaklaşım: ardışık iki tarih seçtir (başlangıç ve bitiş)
    final java.util.Calendar cal = java.util.Calendar.getInstance();
    android.app.DatePickerDialog fromDlg = new android.app.DatePickerDialog(requireContext(), (v, y, m, d) -> {
      java.util.Calendar start = java.util.Calendar.getInstance();
      start.set(y, m, d, 0, 0, 0);
      fromDate = start.getTime();
      // Bitiş seçimi
      android.app.DatePickerDialog toDlg = new android.app.DatePickerDialog(requireContext(), (v2, y2, m2, d2) -> {
        java.util.Calendar end = java.util.Calendar.getInstance();
        end.set(y2, m2, d2, 23, 59, 59);
        toDate = end.getTime();
        updateRangeButtonLabel();
        applyFilters();
      }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
      toDlg.show();
    }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH));
    fromDlg.show();
  }

  private void updateRangeButtonLabel() {
    if (btnPickRange == null) return;
    java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("dd.MM.yyyy");
    if (fromDate == null && toDate == null) {
      btnPickRange.setText("Tarih Aralığı");
      return;
    }
    String from = fromDate != null ? fmt.format(fromDate) : "-";
    String to = toDate != null ? fmt.format(toDate) : "-";
    btnPickRange.setText(from + " → " + to);
  }

  private void applyFilters() {
    String q = inputSearch.getText() != null ? inputSearch.getText().toString().trim().toLowerCase() : "";
    java.util.List<LogEntry> filtered = new java.util.ArrayList<>();
    for (LogEntry e : logs) {
      // status
      if (statusFilter != null) {
        if (statusFilter == 1 && !e.can_pass) continue;
        if (statusFilter == 2 && e.can_pass) continue;
        if (statusFilter == 3 && (!"partial_compliance".equals(e.status))) continue;
      }
      // no extra quick flags
      // search username
      if (q.length() > 0) {
        String uname = e.username != null ? e.username.toLowerCase() : "";
        if (!uname.contains(q)) continue;
      }
      // date range (timestamp assumed ISO or "YYYY-MM-DD HH:MM:SS")
      if (fromDate != null || toDate != null) {
        java.util.Date ts = parseTimestamp(e.timestamp);
        if (ts == null) continue;
        if (fromDate != null && ts.before(fromDate)) continue;
        if (toDate != null && ts.after(toDate)) continue;
      }
      filtered.add(e);
    }
    // Sort
    sortList(filtered);
    textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    // Sadece adapter içeriğini güncellemek için yeni bir liste referansı kullan
    // Basit yaklaşım: adapter doğrudan logs'u gösterdiği için, geçici filtered'ı logs ile değiştiriyoruz
    logs.clear();
    logs.addAll(filtered);
    adapter.notifyDataSetChanged();
    // stats panel kaldırıldı
    updateClearEnabled();
  }

  private void updateClearEnabled() {
    boolean hasFilters = (fromDate != null || toDate != null) || (statusFilter != null) || (inputSearch.getText() != null && inputSearch.getText().length() > 0);
    if (getView() != null) {
      android.widget.Button btn = getView().findViewById(R.id.btnClear);
      if (btn != null) btn.setEnabled(hasFilters);
    }
  }

  private void setupSortSpinner() {
    // İlk öğe hint olarak "Sırala" görünsün
    String[] items = new String[] { "Sırala", "Tarih (Yeni→Eski)", "Tarih (Eski→Yeni)", "Kullanıcı (A→Z)" };
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
      @Override
      public boolean isEnabled(int position) {
        // Hint tıklanamaz
        return position != 0;
      }
      @Override
      public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = super.getDropDownView(position, convertView, parent);
        android.widget.TextView tv = (android.widget.TextView) v;
        if (position == 0) {
          tv.setTextColor(android.graphics.Color.GRAY);
        } else {
          tv.setTextColor(android.graphics.Color.BLACK);
        }
        return v;
      }
    };
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerSort.setAdapter(adapter);
    spinnerSort.setSelection(0); // Hint göster
    spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) return; // hint seçilirse işlem yapma
        applyFilters();
      }
      @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    });
  }

  private void sortList(java.util.List<LogEntry> list) {
    int pos = spinnerSort != null ? spinnerSort.getSelectedItemPosition() : 0;
    if (pos == 0) {
      // Hint seçiliyken varsayılan: Tarih (Yeni→Eski)
      pos = 1;
    }
    java.util.Comparator<LogEntry> byTsAsc = (a, b) -> {
      java.util.Date da = parseTimestamp(a.timestamp);
      java.util.Date db = parseTimestamp(b.timestamp);
      if (da == null && db == null) return 0;
      if (da == null) return -1;
      if (db == null) return 1;
      return da.compareTo(db);
    };
    switch (pos) {
      case 1: // Tarih (Yeni→Eski)
        java.util.Collections.sort(list, java.util.Collections.reverseOrder(byTsAsc));
        break;
      case 2: // Tarih (Eski→Yeni)
        java.util.Collections.sort(list, byTsAsc);
        break;
      case 3: // Kullanıcı (A→Z)
        java.util.Collections.sort(list, (a, b) -> {
          String ua = a.username != null ? a.username.toLowerCase() : "";
          String ub = b.username != null ? b.username.toLowerCase() : "";
          return ua.compareTo(ub);
        });
        break;
      default:
        break;
    }
  }

  private void setQuickToday() {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    java.util.Calendar start = (java.util.Calendar) cal.clone();
    start.set(java.util.Calendar.HOUR_OF_DAY, 0);
    start.set(java.util.Calendar.MINUTE, 0);
    start.set(java.util.Calendar.SECOND, 0);
    fromDate = start.getTime();
    java.util.Calendar end = (java.util.Calendar) cal.clone();
    end.set(java.util.Calendar.HOUR_OF_DAY, 23);
    end.set(java.util.Calendar.MINUTE, 59);
    end.set(java.util.Calendar.SECOND, 59);
    toDate = end.getTime();
  }

  private void setQuickThisWeek() {
    java.util.Calendar cal = java.util.Calendar.getInstance();
    // Haftanın başlangıcı (Pazartesi)
    cal.set(java.util.Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
    java.util.Calendar start = (java.util.Calendar) cal.clone();
    start.set(java.util.Calendar.HOUR_OF_DAY, 0);
    start.set(java.util.Calendar.MINUTE, 0);
    start.set(java.util.Calendar.SECOND, 0);
    fromDate = start.getTime();
    // Haftanın sonu (Pazar)
    java.util.Calendar end = (java.util.Calendar) start.clone();
    end.add(java.util.Calendar.DAY_OF_YEAR, 6);
    end.set(java.util.Calendar.HOUR_OF_DAY, 23);
    end.set(java.util.Calendar.MINUTE, 59);
    end.set(java.util.Calendar.SECOND, 59);
    toDate = end.getTime();
  }

  // stats panel kaldırıldı

  // toggleFilters kaldırıldı; panel her zaman açık

  private java.util.Date parseTimestamp(String s) {
    if (s == null) return null;
    try {
      // Try ISO
      java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(s);
      return java.util.Date.from(odt.toInstant());
    } catch (Exception ignored) {}
    try {
      String core = s.length() >= 19 ? s.substring(0, 19) : s;
      core = core.replace(' ', 'T');
      java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(core);
      return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    } catch (Exception ignored) {}
    return null;
  }

  private void showLogDetail(LogEntry e) {
    android.view.LayoutInflater inflater = android.view.LayoutInflater.from(requireContext());
    android.view.View sheet = inflater.inflate(R.layout.dialog_log_detail, null, false);
    android.widget.TextView textUserRole = sheet.findViewById(R.id.textUserRole);
    android.widget.TextView textTimestamp = sheet.findViewById(R.id.textTimestamp);
    android.widget.TextView pillPass = sheet.findViewById(R.id.pillPass);
    android.widget.TextView pillStatus = sheet.findViewById(R.id.pillStatus);
    android.widget.ImageView imageFrame = sheet.findViewById(R.id.imageFrame);
    android.widget.TextView textMissingRequired = sheet.findViewById(R.id.textMissingRequired);
    android.widget.TextView textMissingOptional = sheet.findViewById(R.id.textMissingOptional);
    android.widget.TextView textDetected = sheet.findViewById(R.id.textDetected);
    android.widget.Button btnClose = sheet.findViewById(R.id.btnClose);

    textUserRole.setText(e.username + " • " + e.user_role);
    textTimestamp.setText("Tarih: " + e.timestamp);
    pillPass.setText(e.can_pass ? "GEÇTİ" : "RED");
    pillPass.setBackgroundResource(e.can_pass ? R.drawable.pill_bg_green : R.drawable.pill_bg_red);
    pillStatus.setText(e.status != null ? StatusTranslator.translate(e.status) : "-");

    if (e.frame_image != null && e.frame_image.startsWith("data:image")) {
      try {
        String b64 = e.frame_image.substring(e.frame_image.indexOf(',') + 1);
        byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bmp != null) {
          imageFrame.setVisibility(View.VISIBLE);
          imageFrame.setImageBitmap(bmp);
        }
      } catch (Exception ignored) {}
    }

    // Translate missing required equipment names
    String missingRequiredText = "-";
    if (e.missing_required != null && !e.missing_required.isEmpty()) {
      java.util.List<String> translatedRequired = new java.util.ArrayList<>();
      for (String item : e.missing_required) {
        translatedRequired.add(EquipmentTranslator.translate(item));
      }
      missingRequiredText = android.text.TextUtils.join(", ", translatedRequired);
    }
    textMissingRequired.setText("Eksik (zorunlu): " + missingRequiredText);
    
    // Translate missing optional equipment names
    String missingOptionalText = "-";
    if (e.missing_optional != null && !e.missing_optional.isEmpty()) {
      java.util.List<String> translatedOptional = new java.util.ArrayList<>();
      for (String item : e.missing_optional) {
        translatedOptional.add(EquipmentTranslator.translate(item));
      }
      missingOptionalText = android.text.TextUtils.join(", ", translatedOptional);
    }
    textMissingOptional.setText("Eksik (opsiyonel): " + missingOptionalText);

    // detected list to string - improved processing
    String detectedText = "-";
    try {
      if (e.detected_items != null && !e.detected_items.isEmpty()) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Object item : e.detected_items) {
          if (item instanceof java.util.Map) {
            java.util.Map<?, ?> itemMap = (java.util.Map<?, ?>) item;
            // Try different possible field names for detection class
            Object className = itemMap.get("class_name");
            if (className == null) className = itemMap.get("name");
            if (className == null) className = itemMap.get("class");
            if (className == null) className = itemMap.get("label");
            
            if (className != null) {
              String nameStr = String.valueOf(className);
              if (!nameStr.isEmpty()) {
                String translatedName = EquipmentTranslator.translate(nameStr);
                if (!names.contains(translatedName)) {
                  names.add(translatedName);
                }
              }
            }
          } else if (item instanceof String) {
            // Handle case where detected_items contains direct string values
            String itemStr = (String) item;
            if (!itemStr.isEmpty()) {
              String translatedName = EquipmentTranslator.translate(itemStr);
              if (!names.contains(translatedName)) {
                names.add(translatedName);
              }
            }
          }
        }
        if (!names.isEmpty()) {
          detectedText = android.text.TextUtils.join(", ", names);
        }
      }
    } catch (Exception e1) {
      // Log the error for debugging
      android.util.Log.e("AdminLogsFragment", "Error processing detected items", e1);
    }
    textDetected.setText("Tespitler: " + detectedText);


    com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
    dialog.setContentView(sheet);
    btnClose.setOnClickListener(v -> dialog.dismiss());
    dialog.show();
  }

  private void loadLogs(boolean reset) {
    if (loading) return;
    loading = true;
    
    if (reset) {
      offset = 0;
      hasMore = true;
      logs.clear();
      progress.setVisibility(View.VISIBLE);
    }

    ApiService api = NetworkModule.api();
    api.getLogs(limit, offset).enqueue(new Callback<LogsResponse>() {
      @Override
      public void onResponse(@NonNull Call<LogsResponse> call, @NonNull Response<LogsResponse> response) {
        loading = false;
        progress.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        
        if (response.isSuccessful() && response.body() != null) {
          LogsResponse resp = response.body();
          if (resp.logs != null) {
            logs.addAll(resp.logs);
            hasMore = logs.size() < resp.total_count;
            offset += resp.logs.size();
          }
          adapter.notifyDataSetChanged();
          textEmpty.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        }
      }

      @Override
      public void onFailure(@NonNull Call<LogsResponse> call, @NonNull Throwable t) {
        loading = false;
        progress.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        textEmpty.setText("Hata: " + t.getMessage());
        textEmpty.setVisibility(View.VISIBLE);
      }
    });
  }
}
