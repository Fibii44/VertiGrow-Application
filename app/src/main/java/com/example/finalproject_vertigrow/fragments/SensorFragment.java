package com.example.finalproject_vertigrow.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.SensorData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class SensorFragment extends Fragment {
    private static final String TAG = "SensorFragment";
    
    // Title
    private TextView textSensorTitle;
    
    // Layer 3
    private TextView textL3;
    private TextView textMoistureL3;
    private TextView soilmoistureL3;
    private TextView textModerateL3;
    private ProgressBar progressL3;
    
    // Layer 2
    private TextView textL2;
    private TextView textMoistureL2;
    private TextView soilmoistureL2;
    private TextView textModerateL2;
    private ProgressBar progressL2;
    
    // Layer 1
    private TextView textL1;
    private TextView textMoistureL1;
    private TextView soilmoistureL1;
    private TextView textModerateL1;
    private ProgressBar progressL1;
    
    // Monitoring
    private ProgressBar batteryProgress;
    private TextView batteryPercentage;
    private TextView phLevel;
    private TextView temperature;
    private TextView humidity;
    private TextView lightIntensity;
    private TextView waterTank;
    private TextView growLights;
    private TextView waterPump;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration sensorListener;
    private int farm;

    public SensorFragment() {
        // Required empty constructor
    }

    public static SensorFragment newInstance(int farm) {
        SensorFragment fragment = new SensorFragment();
        Bundle args = new Bundle();
        args.putInt("farm", farm);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            farm = getArguments().getInt("farm");
        }
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupSensorDataListener();
    }

    private void initializeViews(View view) {
        // Title
        textSensorTitle = view.findViewById(R.id.text_sensor_title);
        
        // Layer 3
        textL3 = view.findViewById(R.id.text_l3);
        textMoistureL3 = view.findViewById(R.id.text_moisture_l3);
        soilmoistureL3 = view.findViewById(R.id.soilmoisture_l3);
        textModerateL3 = view.findViewById(R.id.text_moderate_l3);
        progressL3 = view.findViewById(R.id.progress_l3);
        
        // Layer 2
        textL2 = view.findViewById(R.id.text_l2);
        textMoistureL2 = view.findViewById(R.id.text_moisture_l2);
        soilmoistureL2 = view.findViewById(R.id.soilmoisture);
        textModerateL2 = view.findViewById(R.id.text_moderate_l2);
        progressL2 = view.findViewById(R.id.progress_l2);
        
        // Layer 1
        textL1 = view.findViewById(R.id.text_l1);
        textMoistureL1 = view.findViewById(R.id.text_moisture_l1);
        soilmoistureL1 = view.findViewById(R.id.soilmoisture_l1);
        textModerateL1 = view.findViewById(R.id.text_moderate_l1);
        progressL1 = view.findViewById(R.id.progress_l1);
        
        // Monitoring
        batteryProgress = view.findViewById(R.id.battery_progress);
        batteryPercentage = view.findViewById(R.id.battery_percentage);
        phLevel = view.findViewById(R.id.ph_level);
        temperature = view.findViewById(R.id.temperature);
        humidity = view.findViewById(R.id.humidity);
        lightIntensity = view.findViewById(R.id.light_intensity);
        waterTank = view.findViewById(R.id.water_tank);
        growLights = view.findViewById(R.id.grow_lights);
        waterPump = view.findViewById(R.id.water_pump);

        textSensorTitle.setText("Farm #" + farm + " Sensors");
    }

    private void setupSensorDataListener() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please sign in to view sensor data", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Setting up sensor listener for farm: " + farm);
        
        // Log all possible farm value types to help with debugging
        Log.d(TAG, "Farm value types to match:");
        Log.d(TAG, "Integer value: " + farm);
        Log.d(TAG, "Long value: " + (long)farm);
        Log.d(TAG, "String value: \"" + farm + "\"");
        
        // Define field names as constants to avoid typos and make it clear these are database field names
        final String FIELD_FARM = "farm";
        final String FIELD_CREATED_AT = "createdAt";
        final String FIELD_BATCH_ID = "batch_id";
        final String FIELD_BATTERY_LEVEL = "battery_level";
        final String FIELD_GROW_LIGHTS_STATUS = "grow_lights_status";
        final String FIELD_HUMIDITY = "humidity";
        final String FIELD_LAYER_1 = "layer_1";
        final String FIELD_LAYER_2 = "layer_2";
        final String FIELD_LAYER_3 = "layer_3";
        final String FIELD_LIGHT = "light";
        final String FIELD_PH_LEVEL = "ph_level";
        final String FIELD_TEMPERATURE = "temperature";
        final String FIELD_UPDATED_AT = "updatedAt";
        final String FIELD_WATER_LEVEL = "water_level";
        final String FIELD_WATER_PUMP_STATUS = "water_pump_status";
        
        // Try multiple approaches to find the farm data
        db.collection("sensors").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " sensor documents");
                
                // Log document farm fields to help debug
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Object farmValue = doc.get(FIELD_FARM);
                    String farmType = farmValue != null ? farmValue.getClass().getSimpleName() : "null";
                    Log.d(TAG, "Document ID: " + doc.getId() + " | farm: " + farmValue + " (" + farmType + ")");
                }
                
                QueryDocumentSnapshot latestDoc = null;
                long latestTimestamp = 0;
                
                // Find the documents that match our farm
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    try {
                        // Get the farm value using multiple approaches
                        boolean isMatch = false;
                        Object farmValue = doc.get(FIELD_FARM);
                        
                        if (farmValue == null) {
                            Log.d(TAG, "Document " + doc.getId() + " has null farm value");
                            continue;
                        }
                        
                        // Compare as long
                        if (farmValue instanceof Long) {
                            isMatch = ((Long) farmValue).intValue() == farm;
                            Log.d(TAG, "Comparing as Long: " + farmValue + " == " + farm + " ? " + isMatch);
                        } 
                        // Compare as integer
                        else if (farmValue instanceof Integer) {
                            isMatch = ((Integer) farmValue) == farm;
                            Log.d(TAG, "Comparing as Integer: " + farmValue + " == " + farm + " ? " + isMatch);
                        } 
                        // Compare as string
                        else if (farmValue instanceof String) {
                            isMatch = Integer.parseInt((String) farmValue) == farm;
                            Log.d(TAG, "Comparing as String: " + farmValue + " == " + farm + " ? " + isMatch);
                        }
                        // Compare as Double or other numeric type
                        else if (farmValue instanceof Number) {
                            isMatch = ((Number) farmValue).intValue() == farm;
                            Log.d(TAG, "Comparing as Number: " + farmValue + " == " + farm + " ? " + isMatch);
                        }
                        
                        // If this document is for our farm
                        if (isMatch) {
                            Log.d(TAG, "Found matching farm document: " + doc.getId());
                            
                            // Get the timestamp to find the latest document
                            Object createdAt = doc.get(FIELD_CREATED_AT);
                            if (createdAt == null) {
                                Log.d(TAG, "Document " + doc.getId() + " has null createdAt value");
                                continue;
                            }
                            
                            long timestamp = 0;
                            
                            if (createdAt instanceof Long) {
                                timestamp = (Long) createdAt;
                            } else if (createdAt instanceof Integer) {
                                timestamp = ((Integer) createdAt).longValue();
                            } else if (createdAt instanceof String) {
                                // If it's a string timestamp, just use the most recent document found
                                // We could implement proper string timestamp comparison here if needed
                                latestDoc = doc;
                                Log.d(TAG, "Using document with string timestamp: " + createdAt);
                                break;
                            } else if (createdAt instanceof com.google.firebase.Timestamp) {
                                timestamp = ((com.google.firebase.Timestamp) createdAt).toDate().getTime();
                            }
                            
                            // Keep track of the latest document
                            if (timestamp > latestTimestamp) {
                                latestTimestamp = timestamp;
                                latestDoc = doc;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing document: " + doc.getId(), e);
                    }
                }
                
                // Process the latest document if found
                if (latestDoc != null) {
                    Log.d(TAG, "Processing latest document: " + latestDoc.getId());
                    processSensorData(latestDoc);
                    
                    // Set up a listener for future changes
                    setupDocumentListener(latestDoc.getReference());
                } else {
                    Log.d(TAG, "No matching documents found for farm: " + farm);
                    Log.d(TAG, "This suggests either no farm data exists or there's a data type mismatch.");
                    if (getContext() != null) {
                        // Remove this toast message
                        // Toast.makeText(getContext(), "No sensor data available for Farm #" + farm, 
                        //              Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Log.d(TAG, "No sensor data available in the collection");
                if (getContext() != null) {
                    // Remove this toast message
                    // Toast.makeText(getContext(), "No sensor data available", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting sensor data", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupDocumentListener(com.google.firebase.firestore.DocumentReference docRef) {
        sensorListener = docRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening for document updates", error);
                return;
            }
            
            if (documentSnapshot != null && documentSnapshot.exists()) {
                Log.d(TAG, "Document updated: " + documentSnapshot.getId());
                processSensorData(documentSnapshot);
            }
        });
    }
    
    private void processSensorData(com.google.firebase.firestore.DocumentSnapshot document) {
        try {
            // Define field names as constants to avoid typos
            final String FIELD_BATCH_ID = "batch_id";
            final String FIELD_FARM = "farm";
            final String FIELD_BATTERY_LEVEL = "battery_level";
            final String FIELD_CREATED_AT = "createdAt";
            final String FIELD_GROW_LIGHTS_STATUS = "grow_lights_status";
            final String FIELD_HUMIDITY = "humidity";
            final String FIELD_LAYER_1 = "layer_1";
            final String FIELD_LAYER_2 = "layer_2";
            final String FIELD_LAYER_3 = "layer_3";
            final String FIELD_LIGHT = "light";
            final String FIELD_PH_LEVEL = "ph_level";
            final String FIELD_TEMPERATURE = "temperature";
            final String FIELD_UPDATED_AT = "updatedAt";
            final String FIELD_WATER_LEVEL = "water_level";
            final String FIELD_WATER_PUMP_STATUS = "water_pump_status";
            
            // Log document data for debugging
            Log.d(TAG, "Document data: " + document.getData());
            
            // Create a SensorData object and set all the values
            SensorData sensorData = new SensorData();
            sensorData.setId(document.getId());
            
            // Handle potential missing fields or type mismatches
            if (document.contains(FIELD_BATCH_ID)) {
                Object batchId = document.get(FIELD_BATCH_ID);
                if (batchId instanceof Long) {
                    sensorData.setBatchId(((Long) batchId).intValue());
                } else if (batchId instanceof Integer) {
                    sensorData.setBatchId((Integer) batchId);
                } else if (batchId instanceof String) {
                    sensorData.setBatchId(Integer.parseInt((String) batchId));
                }
            }
            
            if (document.contains(FIELD_FARM)) {
                Object farmValue = document.get(FIELD_FARM);
                if (farmValue instanceof Long) {
                    sensorData.setFarm(((Long) farmValue).intValue());
                } else if (farmValue instanceof Integer) {
                    sensorData.setFarm((Integer) farmValue);
                } else if (farmValue instanceof String) {
                    sensorData.setFarm(Integer.parseInt((String) farmValue));
                }
            }
            
            if (document.contains(FIELD_BATTERY_LEVEL)) {
                Object batteryLevel = document.get(FIELD_BATTERY_LEVEL);
                if (batteryLevel instanceof Long) {
                    sensorData.setBatteryLevel(((Long) batteryLevel).intValue());
                } else if (batteryLevel instanceof Integer) {
                    sensorData.setBatteryLevel((Integer) batteryLevel);
                } else if (batteryLevel instanceof String) {
                    sensorData.setBatteryLevel(Integer.parseInt((String) batteryLevel));
                }
            }
            
            // Handle timestamp fields
            if (document.contains(FIELD_CREATED_AT)) {
                com.google.firebase.Timestamp timestamp = document.getTimestamp(FIELD_CREATED_AT);
                if (timestamp != null) {
                    // Format the timestamp for display
                    String formattedDate = new java.text.SimpleDateFormat("MMM d, yyyy h:mm a", 
                                  java.util.Locale.getDefault()).format(timestamp.toDate());
                    sensorData.setCreatedAt(formattedDate);
                    Log.d(TAG, "CreatedAt timestamp: " + timestamp.toDate() + " formatted as: " + formattedDate);
                } else {
                    // Fallback to string if not a timestamp
                    sensorData.setCreatedAt(document.getString(FIELD_CREATED_AT));
                    Log.d(TAG, "CreatedAt is not a timestamp, using string value");
                }
            }
            
            if (document.contains(FIELD_UPDATED_AT)) {
                com.google.firebase.Timestamp timestamp = document.getTimestamp(FIELD_UPDATED_AT);
                if (timestamp != null) {
                    // Format the timestamp for display
                    String formattedDate = new java.text.SimpleDateFormat("MMM d, yyyy h:mm a", 
                                  java.util.Locale.getDefault()).format(timestamp.toDate());
                    sensorData.setUpdatedAt(formattedDate);
                    Log.d(TAG, "UpdatedAt timestamp: " + timestamp.toDate() + " formatted as: " + formattedDate);
                } else {
                    // Fallback to string if not a timestamp
                    sensorData.setUpdatedAt(document.getString(FIELD_UPDATED_AT));
                    Log.d(TAG, "UpdatedAt is not a timestamp, using string value");
                }
            }
            
            sensorData.setGrowLightsStatus(document.getString(FIELD_GROW_LIGHTS_STATUS));
            sensorData.setHumidity((Map<String, Object>) document.get(FIELD_HUMIDITY));
            sensorData.setLayer1((Map<String, Object>) document.get(FIELD_LAYER_1));
            sensorData.setLayer2((Map<String, Object>) document.get(FIELD_LAYER_2));
            sensorData.setLayer3((Map<String, Object>) document.get(FIELD_LAYER_3));
            sensorData.setLight((Map<String, Object>) document.get(FIELD_LIGHT));
            sensorData.setPhLevel((Map<String, Object>) document.get(FIELD_PH_LEVEL));
            sensorData.setTemperature((Map<String, Object>) document.get(FIELD_TEMPERATURE));
            sensorData.setWaterLevel((Map<String, Object>) document.get(FIELD_WATER_LEVEL));
            sensorData.setWaterPumpStatus((Map<String, Object>) document.get(FIELD_WATER_PUMP_STATUS));
            
            Log.d(TAG, "Successfully parsed sensor data for farm: " + farm);
            updateUI(sensorData);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing sensor data", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error parsing sensor data: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateUI(SensorData sensorData) {
        if (sensorData == null) return;

        // Update Layer 3
        textMoistureL3.setText(String.format("%.0f", sensorData.getLayer3Moisture()));
        updateMoistureStatus(textModerateL3, progressL3, sensorData.getLayer3Moisture());
        
        // Update Layer 2
        textMoistureL2.setText(String.format("%.0f", sensorData.getLayer2Moisture()));
        updateMoistureStatus(textModerateL2, progressL2, sensorData.getLayer2Moisture());
        
        // Update Layer 1
        textMoistureL1.setText(String.format("%.0f", sensorData.getLayer1Moisture()));
        updateMoistureStatus(textModerateL1, progressL1, sensorData.getLayer1Moisture());
        
        // Update Battery
        batteryProgress.setProgress(sensorData.getBatteryLevel());
        batteryPercentage.setText(sensorData.getBatteryLevel() + "%");
        
        // Update pH Level
        phLevel.setText(String.format("%.1f", sensorData.getPhValue()));
        
        // Update Temperature
        temperature.setText(String.format("%.1f°C", sensorData.getTemperatureValue()));
        
        // Update Humidity
        humidity.setText(String.format("%.0f%%", sensorData.getHumidityValue()));
        
        // Update Light Intensity
        lightIntensity.setText(String.format("%.0f lux", sensorData.getLightValue()));
        
        // Update Water Tank
        updateWaterLevel(sensorData.getWaterLevelStatus());
        
        // Update Grow Lights
        growLights.setText(sensorData.getGrowLightsStatus().toUpperCase());
        growLights.setTextColor(getResources().getColor(
            sensorData.getGrowLightsStatus().equalsIgnoreCase("on") ? 
            android.R.color.holo_green_light : android.R.color.darker_gray));
        
        // Update Water Pump
        waterPump.setText(sensorData.getWaterPumpStatusValue().toUpperCase());
        waterPump.setTextColor(getResources().getColor(
            sensorData.getWaterPumpStatusValue().equalsIgnoreCase("on") ? 
            android.R.color.holo_green_light : android.R.color.darker_gray));
    }
    
    private void updateMoistureStatus(TextView statusText, ProgressBar progressBar, float moistureValue) {
        // Convert moisture value to percentage (assuming range 0-4000 maps to 0-100%)
        int percentage = (int) (moistureValue / 40);
        progressBar.setProgress(percentage);
        
        // Set status text and color based on moisture level
        if (percentage < 30) {
            statusText.setText("DRY");
            statusText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (percentage < 70) {
            statusText.setText("MODERATE");
            statusText.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            statusText.setText("WET");
            statusText.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }
    
    private void updateWaterLevel(String waterLevel) {
        // Convert water level to text status
        if (waterLevel.equalsIgnoreCase("low")) {
            waterTank.setText("Low");
            waterTank.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else if (waterLevel.equalsIgnoreCase("medium")) {
            waterTank.setText("Medium");
            waterTank.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            waterTank.setText("Full");
            waterTank.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sensorListener != null) {
            sensorListener.remove();
        }
    }
} 