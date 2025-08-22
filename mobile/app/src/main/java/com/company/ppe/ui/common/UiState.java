package com.company.ppe.ui.common;

public class UiState<T> {
    public final boolean loading;
    public final T data;
    public final String error;

    private UiState(boolean loading, T data, String error) {
        this.loading = loading; this.data = data; this.error = error;
    }
    public static <T> UiState<T> loading() { return new UiState<>(true, null, null); }
    public static <T> UiState<T> success(T data) { return new UiState<>(false, data, null); }
    public static <T> UiState<T> error(String msg) { return new UiState<>(false, null, msg); }
}


