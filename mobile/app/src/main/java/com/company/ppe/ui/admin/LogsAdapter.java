package com.company.ppe.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.ppe.R;
import com.company.ppe.data.network.dto.LogEntry;

import java.util.List;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {
  public interface OnLogClickListener { void onLogClick(LogEntry log); }

  private final List<LogEntry> logs;
  private final OnLogClickListener listener;

  public LogsAdapter(List<LogEntry> logs, OnLogClickListener listener) {
    this.logs = logs;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    LogEntry log = logs.get(position);
    holder.bind(log);
    holder.itemView.setOnClickListener(v -> {
      int pos = holder.getAdapterPosition();
      if (pos != RecyclerView.NO_POSITION) {
        listener.onLogClick(logs.get(pos));
      }
    });
  }

  @Override
  public int getItemCount() {
    return logs.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView textUser, textTimestamp, textMissing, textBadge;
    android.widget.ImageView imageThumb;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      textUser = itemView.findViewById(R.id.textUser);
      textTimestamp = itemView.findViewById(R.id.textTimestamp);
      textMissing = itemView.findViewById(R.id.textMissing);
      textBadge = itemView.findViewById(R.id.textBadge);
      imageThumb = itemView.findViewById(R.id.imageThumb);
    }

    void bind(LogEntry log) {
      textUser.setText(log.username + " • " + log.user_role);
      textBadge.setText(log.can_pass ? "GEÇTİ" : "RED");
      textBadge.setBackgroundResource(log.can_pass ? R.drawable.bg_badge_pass : R.drawable.bg_badge_fail);
      textTimestamp.setText(getRelativeTime(log.timestamp));
      
      StringBuilder missing = new StringBuilder();
      if (log.missing_required != null && !log.missing_required.isEmpty()) {
        java.util.List<String> translatedRequired = new java.util.ArrayList<>();
        for (String item : log.missing_required) {
          translatedRequired.add(com.company.ppe.utils.EquipmentTranslator.translate(item));
        }
        missing.append("Eksikler: ").append(String.join(", ", translatedRequired));
      }
      if (log.missing_optional != null && !log.missing_optional.isEmpty()) {
        java.util.List<String> translatedOptional = new java.util.ArrayList<>();
        for (String item : log.missing_optional) {
          translatedOptional.add(com.company.ppe.utils.EquipmentTranslator.translate(item));
        }
        if (missing.length() > 0) missing.append(" | ops: ");
        missing.append(String.join(", ", translatedOptional));
      }
      
      if (missing.length() > 0) {
        textMissing.setText(missing.toString());
        textMissing.setVisibility(View.VISIBLE);
      } else {
        textMissing.setVisibility(View.GONE);
      }

      // thumbnail
      if (log.frame_image != null && log.frame_image.startsWith("data:image")) {
        try {
          String b64 = log.frame_image.substring(log.frame_image.indexOf(',') + 1);
          byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
          android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
          if (bmp != null) {
            imageThumb.setVisibility(View.VISIBLE);
            imageThumb.setImageBitmap(bmp);
          } else {
            imageThumb.setVisibility(View.GONE);
          }
        } catch (Exception e) {
          imageThumb.setVisibility(View.GONE);
        }
      } else {
        imageThumb.setVisibility(View.GONE);
      }
    }

    private String getRelativeTime(String ts) {
      try {
        java.time.OffsetDateTime odt;
        try { odt = java.time.OffsetDateTime.parse(ts); }
        catch (Exception ignore) {
          String core = ts.length() >= 19 ? ts.substring(0, 19) : ts;
          core = core.replace(' ', 'T');
          odt = java.time.LocalDateTime.parse(core).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        }
        long then = odt.toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - then);
        long min = diff / 60000L;
        if (min < 1) return "şimdi";
        if (min < 60) return min + " dk önce";
        long hr = min / 60;
        if (hr < 24) return hr + " sa önce";
        long day = hr / 24;
        return day + " gün önce";
      } catch (Exception e) {
        return ts;
      }
    }
  }
}
