package com.company.ppe.data.network.dto;

import java.util.List;
import java.util.Map;

public class DetectionResponse {
  public List<DetectedItem> detected_items;
  public boolean person_detected;
  public Analysis analysis;

  public static class DetectedItem {
    public String class_name;
    public double confidence;
    public List<Double> bbox;
  }

  public static class Analysis {
    public boolean can_pass;
    public String status;
    public String message;
    public List<String> missing_required;
    public List<String> missing_optional;
    public Integer total_missing;
  }
}


