package com.company.ppe.data.network.dto;

import java.util.List;

public class PagedResponse<T> {
  public int page;
  public int totalPages;
  public java.util.List<T> items;
}


