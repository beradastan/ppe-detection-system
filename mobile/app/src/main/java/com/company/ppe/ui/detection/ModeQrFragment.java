package com.company.ppe.ui.detection;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.company.ppe.R;
import com.company.ppe.util.SessionManager;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class ModeQrFragment extends Fragment {
  private ImageView image;
  private ProgressBar progress;
  private TextView label;
  private java.util.Timer autoCloseTimer;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_mode_qr, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    image = view.findViewById(R.id.qr);
    progress = view.findViewById(R.id.progress);
    label = view.findViewById(R.id.label);

    String username = SessionManager.userName(requireContext());
    String role = SessionManager.role(requireContext());
    label.setText(username + " – " + role);

    // Try server QR first
    ApiService api = NetworkModule.api();
    progress.setVisibility(View.VISIBLE);
    api.getUserQr().enqueue(new Callback<ResponseBody>() {
      @Override public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
        progress.setVisibility(View.GONE);
        if (response.isSuccessful() && response.body() != null) {
          try {
            byte[] bytes = response.body().bytes();
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) {
              image.setImageBitmap(bmp);
              return;
            }
          } catch (Exception ignored) {}
        }
        String payload = "{\"user\":\"" + username + "\",\"user_role\":\"" + role + "\"}";
        generateLocalQr(payload);
      }
      @Override public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
        progress.setVisibility(View.GONE);
        String payload = "{\"user\":\"" + username + "\",\"user_role\":\"" + role + "\"}";
        generateLocalQr(payload);
      }
    });

    // Auto close after 30 seconds similar to Flutter UX
    autoCloseTimer = new java.util.Timer();
    autoCloseTimer.schedule(new java.util.TimerTask() {
      @Override public void run() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
          androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(view);
          nav.popBackStack();
        });
      }
    }, 30_000);
  }

  private void generateLocalQr(String data) {
    progress.setVisibility(View.GONE);
    QRCodeWriter writer = new QRCodeWriter();
    try {
      int size = 600;
      com.google.zxing.common.BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
      Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
      for (int x = 0; x < size; x++) {
        for (int y = 0; y < size; y++) {
          bmp.setPixel(x, y, bm.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
        }
      }
      image.setImageBitmap(bmp);
    } catch (WriterException e) {
      label.setText("QR oluşturulamadı: " + e.getMessage());
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    if (autoCloseTimer != null) { autoCloseTimer.cancel(); autoCloseTimer = null; }
  }
}


