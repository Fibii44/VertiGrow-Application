package com.example.finalproject_vertigrow.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String name;
    private String email;
    private String role;
    private String authProvider;
    private Date createdAt;

    public User() {}

    public User(String name, String email, String role, String authProvider) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.authProvider = authProvider;
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("authProvider", authProvider);
        userData.put("createdAt", createdAt);
        return userData;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}