package com.company.ppe.ui.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.company.ppe.data.network.dto.DetectionResponse;
import com.company.ppe.utils.EquipmentTranslator;

import java.util.ArrayList;
import java.util.List;

public class DetectionOverlayView extends View {
  private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private final List<DetectionResponse.DetectedItem> items = new ArrayList<>();
  private String status;

  public DetectionOverlayView(Context context) {
    super(context);
    init();
  }

  public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public DetectionOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    boxPaint.setStyle(Paint.Style.STROKE);
    boxPaint.setStrokeWidth(8f);
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(36f);
  }

  public void setDetections(List<DetectionResponse.DetectedItem> detections, @Nullable String analysisStatus) {
    items.clear();
    if (detections != null) items.addAll(detections);
    status = analysisStatus;
    invalidate();
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int color = Color.GRAY;
    if ("full_compliance".equals(status)) color = 0xFF34C759; // green
    else if ("partial_compliance".equals(status)) color = 0xFFFFCC00; // yellow
    else if ("non_compliance".equals(status)) color = 0xFFFF3B30; // red
    boxPaint.setColor(color);

    for (DetectionResponse.DetectedItem it : items) {
      if (it.bbox == null || it.bbox.size() < 4) continue;
      float x = it.bbox.get(0).floatValue();
      float y = it.bbox.get(1).floatValue();
      float w = it.bbox.get(2).floatValue();
      float h = it.bbox.get(3).floatValue();

      // assume bbox normalized [0,1]x[0,1]
      float left = x * getWidth();
      float top = y * getHeight();
      float right = (x + w) * getWidth();
      float bottom = (y + h) * getHeight();
      rect.set(left, top, right, bottom);
      canvas.drawRoundRect(rect, 16f, 16f, boxPaint);
      String displayName = it.class_name != null ? EquipmentTranslator.translate(it.class_name) : "";
      canvas.drawText(displayName, left + 8, top + 36, textPaint);
    }
  }
}


