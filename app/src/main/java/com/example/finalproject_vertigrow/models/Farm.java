package com.example.finalproject_vertigrow.models;

public class Farm {
    private String id; // Firestore document ID
    private int farm; // Numeric farm number
    private String userId;
    private String plantName;
    private String plantType;
    private boolean isOnline;

    // Empty constructor needed for Firebase
    public Farm() {
    }

    public Farm(String userId, String plantName, String plantType) {
        this.userId = userId;
        this.plantName = plantName;
        this.plantType = plantType;
        this.isOnline = false; // Default value
    }

    public Farm(String id, int farm, String userId, String plantName, String plantType, boolean isOnline) {
        this.id = id;
        this.farm = farm;
        this.userId = userId;
        this.plantName = plantName;
        this.plantType = plantType;
        this.isOnline = isOnline;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFarm() {
        return farm;
    }

    public void setFarm(int farm) {
        this.farm = farm;
    }

    // For Firestore compatibility - converts string to int
    public void setFarm(String farm) {
        try {
            this.farm = Integer.parseInt(farm);
        } catch (NumberFormatException e) {
            this.farm = 0; // Default to 0 if parsing fails
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getPlantType() {
        return plantType;
    }

    public void setPlantType(String plantType) {
        this.plantType = plantType;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
} 