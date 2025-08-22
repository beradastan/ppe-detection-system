package com.company.ppe.data.network.dto;

public class RegisterRequest {
  public String username;
  public String password;
  public String role;
  public String email;
  public String full_name;
  public Integer supervisor_id;

  public RegisterRequest(String username, String password, String role, String email, String fullName, Integer supervisorId) {
    this.username = username;
    this.password = password;
    this.role = role;
    this.email = email;
    this.full_name = fullName;
    this.supervisor_id = supervisorId;
  }
}
