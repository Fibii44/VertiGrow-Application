package com.example.finalproject_vertigrow.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String name;
    private String email;
    private String role;
    private Long createdAt;
    private List<Integer> farms;
    private String password;

    // Empty constructor needed for Firestore
    public User() {
        this.farms = new ArrayList<>();
    }

    public User(String name, String email, String role, Long createdAt) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.farms = new ArrayList<>();
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "Unknown";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role != null ? role : "user";
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getCreatedAt() {
        return createdAt != null ? createdAt : 0L;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public List<Integer> getFarms() {
        if (farms == null) {
            farms = new ArrayList<>();
        }
        return farms;
    }

    public void setFarms(List<Integer> farms) {
        this.farms = farms != null ? farms : new ArrayList<>();
    }

    public void addFarm(Integer farmId) {
        if (farmId == null) return;
        
        if (this.farms == null) {
            this.farms = new ArrayList<>();
        }
        if (!this.farms.contains(farmId)) {
            this.farms.add(farmId);
        }
    }

    public void removeFarm(Integer farmId) {
        if (this.farms != null && farmId != null) {
            this.farms.remove(farmId);
        }
    }

    public boolean isAdmin() {
        return "admin".equals(getRole());
    }

    @Exclude
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}