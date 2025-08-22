package com.company.ppe.data.network.dto;

public class LoginResponse {
    public String access_token;
    public String token_type;
    public User user;

    public static class User {
        public int id;
        public String username;
        public String role;
    }
}


