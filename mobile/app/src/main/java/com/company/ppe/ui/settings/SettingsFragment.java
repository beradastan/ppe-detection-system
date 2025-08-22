package com.company.ppe.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.company.ppe.R;
import com.company.ppe.data.network.NetworkModule;

public class SettingsFragment extends Fragment {
  private EditText inputBaseUrl;
  private SeekBar seekBarFps, seekBarJpeg;
  private TextView textFps, textJpeg;
  private Button btnSave;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_settings, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    inputBaseUrl = view.findViewById(R.id.inputBaseUrl);
    seekBarFps = view.findViewById(R.id.seekBarFps);
    seekBarJpeg = view.findViewById(R.id.seekBarJpeg);
    textFps = view.findViewById(R.id.textFps);
    textJpeg = view.findViewById(R.id.textJpeg);
    btnSave = view.findViewById(R.id.btnSave);

    loadSettings();
    
    seekBarFps.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        textFps.setText(String.valueOf(progress + 2)); // 2-5 range
      }
      @Override public void onStartTrackingTouch(SeekBar seekBar) {}
      @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    seekBarJpeg.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        float quality = 0.6f + (progress / 100.0f) * 0.2f; // 0.6-0.8 range
        textJpeg.setText(String.format("%.1f", quality));
      }
      @Override public void onStartTrackingTouch(SeekBar seekBar) {}
      @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    });

    btnSave.setOnClickListener(v -> saveSettings());
  }

  private void loadSettings() {
    SharedPreferences prefs = requireContext().getSharedPreferences("ppe_prefs", 0);
    String baseUrl = prefs.getString("base_url", "http://10.0.2.2:8000");
    int fps = prefs.getInt("fps", 2);
    float jpegQuality = prefs.getFloat("jpeg_quality", 0.8f);

    inputBaseUrl.setText(baseUrl);
    seekBarFps.setProgress(fps - 2); // 0-3 range for 2-5
    seekBarJpeg.setProgress((int)((jpegQuality - 0.6f) / 0.2f * 100)); // 0-100 for 0.6-0.8
    
    textFps.setText(String.valueOf(fps));
    textJpeg.setText(String.format("%.1f", jpegQuality));
  }

  private void saveSettings() {
    SharedPreferences prefs = requireContext().getSharedPreferences("ppe_prefs", 0);
    SharedPreferences.Editor editor = prefs.edit();
    
    String baseUrl = inputBaseUrl.getText().toString().trim();
    int fps = seekBarFps.getProgress() + 2;
    float jpegQuality = 0.6f + (seekBarJpeg.getProgress() / 100.0f) * 0.2f;
    
    editor.putString("base_url", baseUrl);
    editor.putInt("fps", fps);
    editor.putFloat("jpeg_quality", jpegQuality);
    editor.apply();

    // reconfigure network
    NetworkModule.reinit(requireContext().getApplicationContext(), baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");

    // Show success message (simple)
    btnSave.setText("Kaydedildi!");
    btnSave.postDelayed(() -> btnSave.setText("Kaydet"), 2000);
  }
}
