package com.company.ppe.data.network.dto;

public class FrameRequest {
  public String frame;
  public String user;
  public String user_role;

  public FrameRequest(String frame, String user, String userRole) {
    this.frame = frame;
    this.user = user;
    this.user_role = userRole;
  }
}


