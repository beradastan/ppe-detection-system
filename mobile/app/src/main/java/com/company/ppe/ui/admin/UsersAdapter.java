package com.company.ppe.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.ppe.R;
import com.company.ppe.data.network.dto.SimpleUser;
import com.company.ppe.util.SessionManager;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {
  public interface OnUserClickListener { void onUserClick(SimpleUser user); }
  private final List<SimpleUser> users;
  private final OnUserClickListener listener;

  public UsersAdapter(List<SimpleUser> users, OnUserClickListener listener) {
    this.users = users;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    SimpleUser user = users.get(position);
    holder.bind(user);
    holder.itemView.setOnClickListener(v -> {
      int pos = holder.getAdapterPosition();
      if (pos != RecyclerView.NO_POSITION) listener.onUserClick(users.get(pos));
    });
  }

  @Override
  public int getItemCount() {
    return users.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    TextView textUsername, textFullName, textSupervisor, textId;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      textUsername = itemView.findViewById(R.id.textUsername);
      textFullName = itemView.findViewById(R.id.textFullName);
      textSupervisor = itemView.findViewById(R.id.textSupervisor);
      textId = itemView.findViewById(R.id.textId);
    }

    void bind(SimpleUser user) {
      textUsername.setText(user.username + " • " + user.role);
      String fullName = user.full_name;
      if (fullName == null || fullName.trim().isEmpty()) {
        String guess = (user.username != null) ? user.username.replace('_', ' ') : "";
        fullName = capitalize(guess);
      }
      textFullName.setText(fullName != null && !fullName.isEmpty() ? ("ad soyad : " + fullName) : "");

      // Supervisor olarak giriş yapıldıysa, worker satırında supervisor bilgisi gösterilmez
      String currentRole = SessionManager.role(itemView.getContext());
      boolean hideSupervisor = currentRole != null && currentRole.equalsIgnoreCase("supervisor");
      if (hideSupervisor) {
        textSupervisor.setVisibility(View.GONE);
      } else {
        textSupervisor.setVisibility(View.VISIBLE);
        String sup = user.supervisor_full_name != null && !user.supervisor_full_name.isEmpty() ? user.supervisor_full_name
            : (user.supervisor_username != null ? user.supervisor_username : null);
        if (sup != null && !sup.isEmpty()) {
          textSupervisor.setText("Supervisor: " + sup);
        } else if (user.supervisor_id != null) {
          textSupervisor.setText("Supervisor ID: " + user.supervisor_id);
        } else {
          textSupervisor.setText("");
        }
      }
      textId.setText("ID: " + user.id);
    }

    private String capitalize(String s) {
      try {
        String[] parts = s.split(" ");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
          if (p.isEmpty()) continue;
          b.append(Character.toUpperCase(p.charAt(0))).append(p.length()>1?p.substring(1):"").append(' ');
        }
        return b.toString().trim();
      } catch (Exception e) { return s; }
    }
  }
}
