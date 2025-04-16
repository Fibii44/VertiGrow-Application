package com.example.finalproject_vertigrow.models;

import java.util.Map;

public class SensorData {
    private String id;  // Firebase push ID
    private int batchId;
    private int farm;
    private int batteryLevel;
    private String growLightsStatus;
    private Map<String, Object> humidity;
    private Map<String, Object> layer1;
    private Map<String, Object> layer2;
    private Map<String, Object> layer3;
    private Map<String, Object> light;
    private Map<String, Object> phLevel;
    private Map<String, Object> temperature;
    private Map<String, Object> waterLevel;
    private Map<String, Object> waterPumpStatus;
    private long timestamp;  // Added timestamp field

    // Empty constructor for Firebase
    public SensorData() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getFarm() {
        return farm;
    }

    public void setFarm(int farm) {
        this.farm = farm;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getGrowLightsStatus() {
        return growLightsStatus;
    }

    public void setGrowLightsStatus(String growLightsStatus) {
        this.growLightsStatus = growLightsStatus;
    }

    public Map<String, Object> getHumidity() {
        return humidity;
    }

    public void setHumidity(Map<String, Object> humidity) {
        this.humidity = humidity;
    }

    public Map<String, Object> getLayer1() {
        return layer1;
    }

    public void setLayer1(Map<String, Object> layer1) {
        this.layer1 = layer1;
    }

    public Map<String, Object> getLayer2() {
        return layer2;
    }

    public void setLayer2(Map<String, Object> layer2) {
        this.layer2 = layer2;
    }

    public Map<String, Object> getLayer3() {
        return layer3;
    }

    public void setLayer3(Map<String, Object> layer3) {
        this.layer3 = layer3;
    }

    public Map<String, Object> getLight() {
        return light;
    }

    public void setLight(Map<String, Object> light) {
        this.light = light;
    }

    public Map<String, Object> getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(Map<String, Object> phLevel) {
        this.phLevel = phLevel;
    }

    public Map<String, Object> getTemperature() {
        return temperature;
    }

    public void setTemperature(Map<String, Object> temperature) {
        this.temperature = temperature;
    }

    public Map<String, Object> getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(Map<String, Object> waterLevel) {
        this.waterLevel = waterLevel;
    }

    public Map<String, Object> getWaterPumpStatus() {
        return waterPumpStatus;
    }

    public void setWaterPumpStatus(Map<String, Object> waterPumpStatus) {
        this.waterPumpStatus = waterPumpStatus;
    }

    // Helper methods to get values from maps
    public float getHumidityValue() {
        return humidity != null && humidity.get("humidity_value") != null ? 
            ((Number) humidity.get("humidity_value")).floatValue() : 0f;
    }

    public String getHumidityFault() {
        return humidity != null && humidity.get("fault") != null ? 
            (String) humidity.get("fault") : "none";
    }

    public float getLayer1Moisture() {
        Object value = layer1 != null ? layer1.get("moisture_value") : null;
        if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getLayer1Fault() {
        return layer1 != null && layer1.get("fault") != null ? 
            (String) layer1.get("fault") : "none";
    }

    public float getLayer2Moisture() {
        Object value = layer2 != null ? layer2.get("moisture_value") : null;
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getLayer2Fault() {
        return layer2 != null && layer2.get("fault") != null ? 
            (String) layer2.get("fault") : "none";
    }

    public float getLayer3Moisture() {
        Object value = layer3 != null ? layer3.get("moisture_value") : null;
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getLayer3Fault() {
        return layer3 != null && layer3.get("fault") != null ? 
            (String) layer3.get("fault") : "none";
    }

    public float getLightValue() {
        Object value = light != null ? light.get("light_value") : null;
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getLightFault() {
        return light != null && light.get("fault") != null ? 
            (String) light.get("fault") : "none";
    }

    public float getPhValue() {
        Object value = phLevel != null ? phLevel.get("ph_value") : null;
        if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getPhFault() {
        return phLevel != null && phLevel.get("fault") != null ? 
            (String) phLevel.get("fault") : "none";
    }

    public float getTemperatureValue() {
        Object value = temperature != null ? temperature.get("temperature_value") : null;
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getTemperatureFault() {
        return temperature != null && temperature.get("fault") != null ? 
            (String) temperature.get("fault") : "none";
    }

    public String getWaterLevelStatus() {
        return waterLevel != null && waterLevel.get("status") != null ? 
            (String) waterLevel.get("status") : "low";
    }

    public String getWaterLevelFault() {
        return waterLevel != null && waterLevel.get("fault") != null ? 
            (String) waterLevel.get("fault") : "none";
    }

    public float getPumpRuntime() {
        Object value = waterPumpStatus != null ? waterPumpStatus.get("pump_runtime") : null;
        if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        return value != null ? ((Number) value).floatValue() : 0f;
    }

    public String getWaterPumpStatusValue() {
        return waterPumpStatus != null && waterPumpStatus.get("status") != null ? 
            (String) waterPumpStatus.get("status") : "off";
    }

    public String getWaterPumpFault() {
        return waterPumpStatus != null && waterPumpStatus.get("fault") != null ? 
            (String) waterPumpStatus.get("fault") : "none";
    }
}