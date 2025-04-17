package com.example.finalproject_vertigrow.models;

import com.google.firebase.firestore.Exclude;

public class LogEntry {
    private String id;
    private String userId;
    private String farmId;
    private String description;
    private long timestamp;
    private String type;  // e.g., "PLANT", "SENSOR", "FARM", "SYSTEM"

    // Empty constructor for Firebase
    public LogEntry() {
    }

    public LogEntry(String id, String userId, String farmId, String description, long timestamp, String type) {
        this.id = id;
        this.userId = userId;
        this.farmId = farmId;
        this.description = description;
        this.timestamp = timestamp;
        this.type = type;
    }

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFarmId() {
        return farmId;
    }

    public void setFarmId(String farmId) {
        this.farmId = farmId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
} 