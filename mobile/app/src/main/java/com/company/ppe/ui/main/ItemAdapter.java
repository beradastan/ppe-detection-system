package com.company.ppe.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.company.ppe.R;
import com.company.ppe.data.network.dto.Item;

public class ItemAdapter extends ListAdapter<Item, ItemAdapter.VH> {

  public interface OnItemClick {
    void onClick(Item item);
  }

  private final OnItemClick click;

  public ItemAdapter(OnItemClick click) {
    super(DIFF);
    this.click = click;
  }

  private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
    @Override public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) { return safeEq(oldItem.id, newItem.id); }
    @Override public boolean areContentsTheSame(@NonNull Item o, @NonNull Item n) {
      return safeEq(o.title, n.title) && safeEq(o.subtitle, n.subtitle) && safeEq(o.imageUrl, n.imageUrl) && safeEq(o.updatedAt, n.updatedAt);
    }
    private boolean safeEq(Object a, Object b) { return a == b || (a != null && a.equals(b)); }
  };

  @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard, parent, false);
    return new VH(v);
  }

  @Override public void onBindViewHolder(@NonNull VH h, int position) {
    Item it = getItem(position);
    h.title.setText(it.title);
    h.subtitle.setText(it.subtitle);
    Glide.with(h.image.getContext()).load(it.imageUrl).centerCrop().into(h.image);
    h.itemView.setOnClickListener(v -> { if (click != null) click.onClick(it); });
  }

  static class VH extends RecyclerView.ViewHolder {
    ImageView image; TextView title; TextView subtitle;
    VH(@NonNull View itemView) {
      super(itemView);
      image = itemView.findViewById(R.id.image);
      title = itemView.findViewById(R.id.title);
      subtitle = itemView.findViewById(R.id.subtitle);
    }
  }
}


