package com.company.ppe.data.network.dto;

public class SimpleUser {
  public int id;
  public String username;
  public String role;
  public String qr_payload;
  // Opsiyonel alanlar (backend destekliyorsa doldurulur)
  public String full_name;
  public Integer supervisor_id;
  public String supervisor_username;
  public String supervisor_full_name;
}
