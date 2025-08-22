package com.company.ppe.ui.detection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.company.ppe.R;
import com.company.ppe.data.network.ApiService;
import com.company.ppe.data.network.NetworkModule;
import com.company.ppe.data.network.dto.DetectionResponse;
import com.company.ppe.data.network.dto.FrameRequest;
import com.company.ppe.utils.StatusTranslator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.ref.WeakReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModeCameraFragment extends Fragment {

  private static final String TAG = "ModeCameraFragment";

  // XML’de olan görünümler
  private PreviewView previewView;
  private View borderOverlay;
  private DetectionOverlayView overlayView;
  private TextView textConn;
  private TextView textStatusPill, textStatusMessage, textAutoExitMessage;
  private ChipGroup chipsMissingBar, chipsExistingBar;
  private ProgressBar progress;

  // Kamera ve zamanlayıcılar
  private ImageCapture imageCapture;
  private ExecutorService cameraExecutor;
  private Timer countdownTimer;
  private Timer healthTimer;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  // Memory leak prevention
  private WeakReference<ModeCameraFragment> fragmentRef;

  // Akış kontrolü
  private volatile boolean inFlight = false;
  private boolean isExitCountdownActive = false;
  private int successCountdown = -1;
  private long streamStartAtMs = 0L;

  // Ayarlar
  private int fps = 2;
  private float jpegQuality = 0.8f;

  private final ActivityResultLauncher<String> requestCamera =
          registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) startCamera();
          });

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_mode_camera, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // Initialize weak reference for memory leak prevention
    fragmentRef = new WeakReference<>(this);

    // XML bağları
    previewView = view.findViewById(R.id.preview);
    overlayView = view.findViewById(R.id.overlay);
    borderOverlay = view.findViewById(R.id.borderOverlay);
    textConn = view.findViewById(R.id.textConn);
    textStatusPill = view.findViewById(R.id.textStatusPill);
    textStatusMessage = view.findViewById(R.id.textStatusMessage);
    textAutoExitMessage = view.findViewById(R.id.textAutoExitMessage);
    chipsMissingBar = view.findViewById(R.id.chipsMissingBar);
    chipsExistingBar = view.findViewById(R.id.chipsExistingBar);
    progress = view.findViewById(R.id.progress);

    // İlk görünürlük
    if (textStatusPill != null) textStatusPill.setVisibility(View.GONE);
    if (textStatusMessage != null) textStatusMessage.setVisibility(View.GONE);
    if (textAutoExitMessage != null) textAutoExitMessage.setVisibility(View.GONE);
    if (progress != null) progress.setVisibility(View.GONE);

    // Preview sabitleme (kırpmalı doldurur, dalgalanma azalır)
    if (previewView != null) {
      previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
      previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    }

    cameraExecutor = Executors.newSingleThreadExecutor();

    // Ayarları yükle
    android.content.SharedPreferences prefs = requireContext().getSharedPreferences("ppe_prefs", 0);
    fps = prefs.getInt("fps", 2);
    jpegQuality = prefs.getFloat("jpeg_quality", 0.8f);

    // Kamera izni
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      startCamera();
    } else {
      requestCamera.launch(Manifest.permission.CAMERA);
    }

    startHealthPing();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
      startCamera();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    // Kamerayı bloklamadan sal
    final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
    future.addListener(() -> {
      try {
        ProcessCameraProvider provider = future.get();
        provider.unbindAll();
      } catch (Exception ignored) {}
    }, ContextCompat.getMainExecutor(requireContext()));
    stopStreaming();
    stopHealthPing();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    
    // Proper cleanup to prevent memory leaks
    cleanupResources();
  }
  
  private void cleanupResources() {
    // Stop all timers first
    stopStreaming();
    stopHealthPing();
    
    // Clear handler callbacks
    if (mainHandler != null) {
      mainHandler.removeCallbacksAndMessages(null);
    }
    
    // Unbind camera
    unbindCamera();
    
    // Shutdown executor
    if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
      cameraExecutor.shutdown();
      try {
        if (!cameraExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
          cameraExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        cameraExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
      cameraExecutor = null;
    }
    
    // Clear references
    imageCapture = null;
    fragmentRef = null;
  }
  
  private void unbindCamera() {
    try {
      final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
      future.addListener(() -> {
        try {
          ProcessCameraProvider provider = future.get();
          provider.unbindAll();
        } catch (Exception e) {
          android.util.Log.e(TAG, "Error unbinding camera", e);
        }
      }, ContextCompat.getMainExecutor(requireContext()));
    } catch (Exception e) {
      android.util.Log.e(TAG, "Error getting camera provider for unbinding", e);
    }
  }

  private void startCamera() {
    final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
    cameraProviderFuture.addListener(() -> {
      try {
        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setJpegQuality((int) (jpegQuality * 100))
                .build();

        CameraSelector cameraSelector = pickAvailableCamera(cameraProvider);
        if (cameraSelector == null) return;

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        startStreaming();

      } catch (Exception e) {
        android.util.Log.e(TAG, "Error starting camera", e);
      }
    }, ContextCompat.getMainExecutor(requireContext()));
  }

  private CameraSelector pickAvailableCamera(ProcessCameraProvider provider) {
    try { if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) return CameraSelector.DEFAULT_BACK_CAMERA; } catch (Exception ignored) {}
    try { if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) return CameraSelector.DEFAULT_FRONT_CAMERA; } catch (Exception ignored) {}
    return null;
  }

  private void startStreaming() {
    if (streamStartAtMs != 0L) return;
    streamStartAtMs = System.currentTimeMillis();
    captureAndSend();

    // 1 sn sayaç
    countdownTimer = new Timer();
    countdownTimer.scheduleAtFixedRate(new java.util.TimerTask() {
      @Override public void run() {
        if (!isAdded()) { cancel(); return; }
        mainHandler.post(() -> {
          if (successCountdown > 0) {
            successCountdown--;
            if (successCountdown == 0) {
              exitAfterSuccess();
            }
          }
        });
      }
    }, 0, 1000);
  }

  private void stopStreaming() {
    if (countdownTimer != null) {
      countdownTimer.cancel();
      countdownTimer.purge(); // Important: purge to release references
      countdownTimer = null;
    }
    inFlight = false;
    streamStartAtMs = 0L;
  }

  private void captureAndSend() {
    if (imageCapture == null) return;
    if (inFlight) return;
    if (isExitCountdownActive) return;
    inFlight = true;

    try {
      final java.io.File outFile = java.io.File.createTempFile("frame_", ".jpg", requireContext().getCacheDir());
      ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(outFile).build();

      imageCapture.takePicture(opts, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
        @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
          try {
            byte[] bytes = java.nio.file.Files.readAllBytes(outFile.toPath());
            String dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);

            ApiService api = NetworkModule.api();
            String user = com.company.ppe.util.SessionManager.userName(requireContext());
            String role = com.company.ppe.util.SessionManager.role(requireContext());

            api.processFrame(new FrameRequest(dataUrl, user, role)).enqueue(new Callback<DetectionResponse>() {
              @Override public void onResponse(@NonNull Call<DetectionResponse> call, @NonNull Response<DetectionResponse> response) {
                if (!isAdded()) { inFlight = false; return; }
                mainHandler.post(() -> {
                  if (response.isSuccessful()) {
                    DetectionResponse body = response.body();
                    if (body != null && overlayView != null) {
                      overlayView.setDetections(body.detected_items, body.analysis != null ? body.analysis.status : null);

                      // VAR OLAN ekipmanlar (yeşil bar)
                      // Not: Exit sayacı aktifken UI sabit kalmalı; bu yüzden güncellemeleri aşağıdaki blokta yapacağız.

                      boolean justGranted = body.analysis != null && body.analysis.can_pass && successCountdown == -1;

                      // Exit sayacı aktifken UI sabit kalsın, ancak izin verildiği KARE için son bir güncelleme yap.
                      if (body.analysis != null && (!isExitCountdownActive || justGranted)) {
                        // Renk durumları (izin verildiği anda yeşile zorla)
                        int color = 0xFF8E8E93; // gri
                        String st = body.analysis.status;
                        if ("full_compliance".equals(st)) color = 0xFF34C759;      // yeşil
                        else if ("partial_compliance".equals(st)) color = 0xFFFFCC00; // sarı
                        else if ("non_compliance".equals(st)) color = 0xFFFF3B30;    // kırmızı
                        if (justGranted) {
                          color = 0xFF34C759; // geçiş anında çerçeve hep yeşil kalsın
                        }

                        // Status pill
                        if (textStatusPill != null) {
                          String pill = "";
                          if (justGranted) {
                            pill = "GEÇTİ";
                          } else if ("full_compliance".equals(st)) pill = "GEÇTİ";
                          else if ("partial_compliance".equals(st)) pill = "KISMİ";
                          else if ("non_compliance".equals(st)) pill = "RED";
                          else if ("no_person".equals(st)) pill = StatusTranslator.translate(st);
                          else if ("error".equals(st)) pill = StatusTranslator.translate(st);
                          else pill = StatusTranslator.translate(st);
                          textStatusPill.setText(pill);
                          textStatusPill.setVisibility(View.VISIBLE);
                          try {
                            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                            int bgColor = (color & 0x00FFFFFF) | 0x20000000;
                            bg.setColor(bgColor);
                            bg.setCornerRadius(24f);
                            bg.setStroke(2, color);
                            textStatusPill.setBackground(bg);
                            textStatusPill.setTextColor(color);
                          } catch (Exception ignored) {}
                        }

                        // Kişi algılanmadı mesajı kaldırıldı
                        if (textStatusMessage != null) {
                          textStatusMessage.setVisibility(View.GONE);
                        }

                        // Kenarlık
                        try {
                          android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                          gd.setColor(0x00000000);
                          gd.setCornerRadius(16f);
                          gd.setStroke(20, color);
                          if (borderOverlay != null) borderOverlay.setBackground(gd);
                        } catch (Exception ignored) {}

                        // Alt bar: eksik ZORUNLU ekipmanlar (kırmızı). Opsiyonelleri bu listeden çıkar.
                        if (chipsMissingBar != null) {
                          chipsMissingBar.removeAllViews();
                          List<String> missingRequired = new ArrayList<>();
                          if (body.analysis.missing_required != null) missingRequired.addAll(body.analysis.missing_required);
                          for (String item : missingRequired) {
                            Chip chip = new Chip(requireContext());
                            chip.setText(com.company.ppe.utils.EquipmentTranslator.translate(item));
                            chip.setCheckable(false);
                            chip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                            chip.setTextColor(android.graphics.Color.WHITE);
                            chipsMissingBar.addView(chip);
                          }
                        }

                        // Alt bar: var olan ekipmanlar (yeşil). Exit sayacı aktif değilken güncelle.
                        if (chipsExistingBar != null) {
                          chipsExistingBar.removeAllViews();
                          if (body.detected_items != null) {
                            for (DetectionResponse.DetectedItem item : body.detected_items) {
                              Chip chip = new Chip(requireContext());
                              chip.setText(com.company.ppe.utils.EquipmentTranslator.translate(item.class_name));
                              chip.setCheckable(false);
                              chip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                              chip.setTextColor(android.graphics.Color.WHITE);
                              chipsExistingBar.addView(chip);
                            }
                          }
                        }
                      }

                      // Geçiş izni → sayacı başlat ve o andan sonra don
                      if (justGranted) {
                        successCountdown = 3;
                        isExitCountdownActive = true;
                        // showAutoExitMessage(); // Mesaj kaldırıldı
                      }
                    }
                  }
                  inFlight = false;
                  scheduleNext();
                });
              }

              @Override public void onFailure(@NonNull Call<DetectionResponse> call, @NonNull Throwable t) {
                if (!isAdded()) { inFlight = false; return; }
                android.util.Log.e(TAG, "Network request failed", t);
                mainHandler.post(() -> {
                  handleNetworkError(t);
                  inFlight = false;
                  scheduleNext();
                });
              }
            });
          } catch (Exception e) {
            inFlight = false;
            scheduleNext();
          } finally {
            try { outFile.delete(); } catch (Exception ignored) {}
          }
        }

        @Override public void onError(@NonNull ImageCaptureException exception) {
          android.util.Log.e(TAG, "Image capture failed", exception);
          inFlight = false;
          scheduleNext();
        }
      });

    } catch (Exception e) {
      inFlight = false;
      scheduleNext();
    }
  }

  private void scheduleNext() {
    if (!isAdded()) return;
    if (isExitCountdownActive) return;
    long intervalMs = Math.max(200, Math.round(1000.0 / Math.max(1, fps)));
    mainHandler.postDelayed(this::captureAndSend, intervalMs);
  }

  private void startHealthPing() {
    if (healthTimer != null) return;
    healthTimer = new Timer();
    healthTimer.scheduleAtFixedRate(new java.util.TimerTask() {
      @Override public void run() {
        if (!isAdded()) return;
        ApiService api = NetworkModule.api();
        api.health().enqueue(new Callback<okhttp3.ResponseBody>() {
          @Override public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
            if (!isAdded()) return;
            mainHandler.post(() -> { if (textConn != null) textConn.setText("Bağlantı: bağlı"); });
          }
          @Override public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
            if (!isAdded()) return;
            android.util.Log.w(TAG, "Health check failed", t);
            mainHandler.post(() -> { if (textConn != null) textConn.setText("Bağlantı: bağlı değil"); });
          }
        });
      }
    }, 0, 5000);
  }

  private void stopHealthPing() {
    if (healthTimer != null) {
      healthTimer.cancel();
      healthTimer.purge(); // Important: purge to release references
      healthTimer = null;
    }
  }

  private void showAutoExitMessage() {
    if (textAutoExitMessage != null) {
      textAutoExitMessage.setVisibility(View.GONE);
    }
  }

  private void hideAutoExitMessage() {
    if (textAutoExitMessage != null) {
      textAutoExitMessage.setVisibility(View.GONE);
    }
  }

  private void exitAfterSuccess() {
    // UI donuk kalsın, mesajı gizleyebiliriz
    isExitCountdownActive = false;
    hideAutoExitMessage();
    // Streaming ve kamera kaynaklarını bırak
    stopStreaming();
    try {
      final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
      future.addListener(() -> {
        try {
          ProcessCameraProvider provider = future.get();
          provider.unbindAll();
        } catch (Exception e) {
          android.util.Log.e(TAG, "Error unbinding camera on exit", e);
        }
      }, ContextCompat.getMainExecutor(requireContext()));
    } catch (Exception e) {
      android.util.Log.e(TAG, "Error getting camera provider on exit", e);
    }
    // Geri dön (ModeSelectFragment'a geri)
    try {
      View v = getView();
      if (v != null) {
        Navigation.findNavController(v).popBackStack();
      }
    } catch (Exception e) {
      android.util.Log.e(TAG, "Error navigating back", e);
    }
  }

  private void handleNetworkError(Throwable error) {
    if (error instanceof java.net.SocketTimeoutException) {
      showUserMessage("Bağlantı zaman aşımı");
    } else if (error instanceof java.net.UnknownHostException) {
      showUserMessage("İnternet bağlantısını kontrol edin");
    } else if (error instanceof java.net.ConnectException) {
      showUserMessage("Sunucuya bağlanılamıyor");
    } else {
      showUserMessage("Bir hata oluştu, tekrar deneyin");
    }
  }

  private void showUserMessage(String message) {
    if (textConn != null) {
      textConn.setText("Bağlantı: " + message);
    }
  }
}
