package com.company.ppe.data.network.dto;

public class RegisterResponse {
  public String message;
  public String access_token;
  public String token_type;
  public User user;

  public static class User {
    public int id;
    public String username;
    public String role;
    public String email;
    public String full_name;
    public Integer supervisor_id;
    public String qr_image_base64;
  }
}
