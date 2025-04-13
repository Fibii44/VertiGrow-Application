package com.example.finalproject_vertigrow.models;

public class Farm {
    private String id;
    private String plantName;
    private String plantType;

    // Empty constructor needed for Firebase
    public Farm() {
    }

    public Farm(String plantName, String plantType) {
        this.plantName = plantName;
        this.plantType = plantType;
    }

    public Farm(String id, String plantName, String plantType) {
        this.id = id;
        this.plantName = plantName;
        this.plantType = plantType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
} 