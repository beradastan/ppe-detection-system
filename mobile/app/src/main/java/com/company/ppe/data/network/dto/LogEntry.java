package com.company.ppe.data.network.dto;

import java.util.List;
import java.util.Map;

public class LogEntry {
  public int id;
  public int user_id;
  public String timestamp;
  public boolean can_pass;
  public String status;
  public List<String> missing_required;
  public List<String> missing_optional;
  public List<Object> detected_items;
  public boolean person_detected;
  public Map<String, Object> confidence_scores;
  public String frame_image;
  public String username;
  public String user_role;
}


