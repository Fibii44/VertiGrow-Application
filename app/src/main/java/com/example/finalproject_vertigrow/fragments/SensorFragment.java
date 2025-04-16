package com.example.finalproject_vertigrow.fragments;

import android.graphics.Color;
import android.content.res.ColorStateList;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private View faultIndicatorL3;
    
    // Layer 2
    private TextView textL2;
    private TextView textMoistureL2;
    private TextView soilmoistureL2;
    private TextView textModerateL2;
    private ProgressBar progressL2;
    private View faultIndicatorL2;
    
    // Layer 1
    private TextView textL1;
    private TextView textMoistureL1;
    private TextView soilmoistureL1;
    private TextView textModerateL1;
    private ProgressBar progressL1;
    private View faultIndicatorL1;
    
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

    // Fault Indicators
    private View faultIndicatorPh;
    private View faultIndicatorTemp;
    private View faultIndicatorHumidity;
    private View faultIndicatorLight;
    private View faultIndicatorWater;

    private FirebaseAuth auth;
    private DatabaseReference sensorsRef;
    private ValueEventListener sensorListener;
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
        // Initialize Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        sensorsRef = database.getReference("sensors");
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
        faultIndicatorL3 = view.findViewById(R.id.fault_indicator_l3);
        
        // Layer 2
        textL2 = view.findViewById(R.id.text_l2);
        textMoistureL2 = view.findViewById(R.id.text_moisture_l2);
        soilmoistureL2 = view.findViewById(R.id.soilmoisture_l2);
        textModerateL2 = view.findViewById(R.id.text_moderate_l2);
        progressL2 = view.findViewById(R.id.progress_l2);
        faultIndicatorL2 = view.findViewById(R.id.fault_indicator_l2);
        
        // Layer 1
        textL1 = view.findViewById(R.id.text_l1);
        textMoistureL1 = view.findViewById(R.id.text_moisture_l1);
        soilmoistureL1 = view.findViewById(R.id.soilmoisture_l1);
        textModerateL1 = view.findViewById(R.id.text_moderate_l1);
        progressL1 = view.findViewById(R.id.progress_l1);
        faultIndicatorL1 = view.findViewById(R.id.fault_indicator_l1);
        
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

        // Fault Indicators
        faultIndicatorPh = view.findViewById(R.id.fault_indicator_ph);
        faultIndicatorTemp = view.findViewById(R.id.fault_indicator_temp);
        faultIndicatorHumidity = view.findViewById(R.id.fault_indicator_humidity);
        faultIndicatorLight = view.findViewById(R.id.fault_indicator_light);
        faultIndicatorWater = view.findViewById(R.id.fault_indicator_water);

        textSensorTitle.setText("Farm #" + farm + " Sensors");
    }

    private void setupSensorDataListener() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please sign in to view sensor data", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Setting up sensor listener for farm: " + farm);
        
        // Listen for new data in Realtime Database
        sensorListener = sensorsRef.orderByKey().limitToLast(1)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        Log.d(TAG, "No sensor data available");
                        return;
                    }

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            // Get the push ID
                            String pushId = snapshot.getKey();
                            Log.d(TAG, "Latest data push ID: " + pushId);
                            
                            // Get the data
                            Map<String, Object> sensorData = (Map<String, Object>) snapshot.getValue();
                            if (sensorData != null) {
                                // Check if this data belongs to the current farm
                                String dataFarm = sensorData.get("farm").toString();
                                if (dataFarm.equals(String.valueOf(farm))) {
                                    processSensorData(sensorData);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing sensor data", e);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error reading data", databaseError.toException());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void processSensorData(Map<String, Object> sensorData) {
        try {
            // Create a SensorData object and set all the values
            SensorData data = new SensorData();
            
            // Set basic fields
            data.setFarm(Integer.parseInt(sensorData.get("farm").toString()));
            data.setBatteryLevel(Integer.parseInt(sensorData.get("battery_level").toString()));
            data.setGrowLightsStatus(sensorData.get("grow_lights_status").toString());
            
            // Set nested objects
            data.setHumidity((Map<String, Object>) sensorData.get("humidity"));
            data.setLayer1((Map<String, Object>) sensorData.get("layer_1"));
            data.setLayer2((Map<String, Object>) sensorData.get("layer_2"));
            data.setLayer3((Map<String, Object>) sensorData.get("layer_3"));
            data.setLight((Map<String, Object>) sensorData.get("light"));
            data.setPhLevel((Map<String, Object>) sensorData.get("ph_level"));
            data.setTemperature((Map<String, Object>) sensorData.get("temperature"));
            data.setWaterLevel((Map<String, Object>) sensorData.get("water_level"));
            data.setWaterPumpStatus((Map<String, Object>) sensorData.get("water_pump_status"));
            
            // Update UI with the new data
            updateUI(data);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing sensor data", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error parsing sensor data: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFaultIndicator(View indicator, String faultStatus) {
        if (faultStatus.equals("none")) {
            indicator.setBackgroundResource(R.drawable.circle_background);
        } else {
            indicator.setBackgroundResource(R.drawable.circle_background_red);
        }
    }

    private void updateUI(SensorData sensorData) {
        if (sensorData == null) {
            Log.e(TAG, "Sensor data is null");
            return;
        }

        try {
            // Update Layer 3
            if (textMoistureL3 != null) {
                textMoistureL3.setText(String.format("%.0f", sensorData.getLayer3Moisture()));
            }
            if (textModerateL3 != null && progressL3 != null) {
                updateMoistureStatus(textModerateL3, progressL3, sensorData.getLayer3Moisture());
            }
            if (faultIndicatorL3 != null) {
                updateFaultIndicator(faultIndicatorL3, sensorData.getLayer3Fault());
            }
            
            // Update Layer 2
            if (textMoistureL2 != null) {
                textMoistureL2.setText(String.format("%.0f", sensorData.getLayer2Moisture()));
            }
            if (textModerateL2 != null && progressL2 != null) {
                updateMoistureStatus(textModerateL2, progressL2, sensorData.getLayer2Moisture());
            }
            if (faultIndicatorL2 != null) {
                updateFaultIndicator(faultIndicatorL2, sensorData.getLayer2Fault());
            }
            
            // Update Layer 1
            if (textMoistureL1 != null) {
                textMoistureL1.setText(String.format("%.0f", sensorData.getLayer1Moisture()));
            }
            if (textModerateL1 != null && progressL1 != null) {
                updateMoistureStatus(textModerateL1, progressL1, sensorData.getLayer1Moisture());
            }
            if (faultIndicatorL1 != null) {
                updateFaultIndicator(faultIndicatorL1, sensorData.getLayer1Fault());
            }
            
            // Update Battery
            if (batteryProgress != null && batteryPercentage != null) {
                batteryProgress.setProgress(sensorData.getBatteryLevel());
                batteryPercentage.setText(sensorData.getBatteryLevel() + "%");
            }
            
            // Update pH Level
            if (phLevel != null) {
                phLevel.setText(String.format("%.1f", sensorData.getPhValue()));
            }
            if (faultIndicatorPh != null) {
                updateFaultIndicator(faultIndicatorPh, sensorData.getPhFault());
            }
            
            // Update Temperature
            if (temperature != null) {
                temperature.setText(String.format("%.1f°C", sensorData.getTemperatureValue()));
            }
            if (faultIndicatorTemp != null) {
                updateFaultIndicator(faultIndicatorTemp, sensorData.getTemperatureFault());
            }
            
            // Update Humidity
            if (humidity != null) {
                humidity.setText(String.format("%.0f%%", sensorData.getHumidityValue()));
            }
            if (faultIndicatorHumidity != null) {
                updateFaultIndicator(faultIndicatorHumidity, sensorData.getHumidityFault());
            }
            
            // Update Light Intensity
            if (lightIntensity != null) {
                lightIntensity.setText(String.format("%.0f lux", sensorData.getLightValue()));
            }
            if (faultIndicatorLight != null) {
                updateFaultIndicator(faultIndicatorLight, sensorData.getLightFault());
            }
            
            // Update Water Tank
            if (waterTank != null) {
                updateWaterLevel(sensorData.getWaterLevelStatus());
            }
            if (faultIndicatorWater != null) {
                updateFaultIndicator(faultIndicatorWater, sensorData.getWaterLevelFault());
            }
            
            // Update Grow Lights
            if (growLights != null) {
                growLights.setText(sensorData.getGrowLightsStatus().toUpperCase());
                growLights.setTextColor(getResources().getColor(
                    sensorData.getGrowLightsStatus().equalsIgnoreCase("on") ? 
                    android.R.color.holo_green_light : android.R.color.darker_gray));
            }
            
            // Update Water Pump
            if (waterPump != null) {
                waterPump.setText(sensorData.getWaterPumpStatusValue().toUpperCase());
                waterPump.setTextColor(getResources().getColor(
                    sensorData.getWaterPumpStatusValue().equalsIgnoreCase("on") ? 
                    android.R.color.holo_green_light : android.R.color.darker_gray));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error updating sensor data", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void updateMoistureStatus(TextView statusText, ProgressBar progressBar, float moistureValue) {
        // Convert moisture value to percentage (0-4000 range)
        int percentage = (int) ((moistureValue / 4000) * 100);
        progressBar.setProgress(percentage);
        
        // Set status text and color based on moisture level thresholds
        if (moistureValue <= 1500) {
            statusText.setText("WET");
            statusText.setBackgroundColor(Color.parseColor("#3498db")); // Blue
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#3498db")));
        } else if (moistureValue <= 3000) {
            statusText.setText("MODERATE");
            statusText.setBackgroundColor(Color.parseColor("#f39c12")); // Orange
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#f39c12")));
        } else {
            statusText.setText("DRY");
            statusText.setBackgroundColor(Color.parseColor("#e74c3c")); // Red
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#e74c3c")));
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
            sensorsRef.removeEventListener(sensorListener);
        }
    }
} 