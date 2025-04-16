package com.example.finalproject_vertigrow.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IrrigationFragment extends Fragment {
    private static final String TAG = "IrrigationFragment";
    
    private TableLayout tableLayout;
    private TextView titleTextView;
    private TextView noDataMessage;
    
    private DatabaseReference sensorsRef;
    private FirebaseAuth auth;
    private String currentUserId;
    
    public IrrigationFragment() {
        // Required empty public constructor
    }

    public static IrrigationFragment newInstance() {
        return new IrrigationFragment();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        sensorsRef = database.getReference("sensors");
        auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.e(TAG, "No authenticated user found");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_irrigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        tableLayout = view.findViewById(R.id.irrigation_table);
        titleTextView = view.findViewById(R.id.farm_title);
        noDataMessage = view.findViewById(R.id.no_data_message);
        
        // Set title
        titleTextView.setText("Irrigation Data - All Farms");
        
        // Load irrigation data
        loadIrrigationData();
    }
    
    private void loadIrrigationData() {
        if (currentUserId == null) {
            Log.d(TAG, "No current user found");
            showMessage("Please sign in to view irrigation data");
            return;
        }
        
        Log.d(TAG, "Loading irrigation data for user: " + currentUserId);
        
        // Get the latest 100 irrigation records
        sensorsRef.orderByKey().limitToLast(100)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Data snapshot received");
                    
                    if (!dataSnapshot.exists()) {
                        Log.d(TAG, "No data exists in snapshot");
                        showNoDataMessage("No irrigation data available");
                        return;
                    }
                    
                    List<SensorData> irrigationData = new ArrayList<>();
                    int totalRecords = 0;
                    int pumpOnRecords = 0;
                    
                    // Process each document
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        totalRecords++;
                        try {
                            // Get the push ID
                            String pushId = snapshot.getKey();
                            Log.d(TAG, "Processing record with ID: " + pushId);
                            
                            // Get the data
                            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                            if (data != null) {
                                Log.d(TAG, "Data fields: " + data.keySet());
                                
                                // Check if it has water pump data
                                if (data.containsKey("water_pump_status")) {
                                    Map<String, Object> pumpStatus = (Map<String, Object>) data.get("water_pump_status");
                                    if (pumpStatus != null) {
                                        String status = pumpStatus.get("status") != null ? 
                                            pumpStatus.get("status").toString() : "unknown";
                                        Log.d(TAG, "Pump status: " + status);
                                        
                                        // Create sensor data regardless of status
                                        SensorData sensorData = new SensorData();
                                        sensorData.setId(pushId);
                                        
                                        // Safely get farm value
                                        Object farmValue = data.get("farm");
                                        if (farmValue != null) {
                                            sensorData.setFarm(Integer.parseInt(farmValue.toString()));
                                        }
                                        
                                        sensorData.setWaterPumpStatus(pumpStatus);
                                        
                                        // Add timestamp from push ID
                                        long timestamp = extractTimestampFromPushId(pushId);
                                        sensorData.setTimestamp(timestamp);
                                        
                                        irrigationData.add(sensorData);
                                        pumpOnRecords++;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing sensor data: " + e.getMessage(), e);
                        }
                    }
                    
                    Log.d(TAG, String.format("Processed %d total records, found %d pump records", 
                        totalRecords, pumpOnRecords));
                    
                    if (irrigationData.isEmpty()) {
                        Log.d(TAG, "No irrigation data found after filtering");
                        showNoDataMessage("No irrigation data found");
                    } else {
                        Log.d(TAG, "Found " + irrigationData.size() + " irrigation records to display");
                        // Sort by timestamp in descending order
                        Collections.sort(irrigationData, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        populateTable(irrigationData);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Error loading irrigation data", databaseError.toException());
                    showNoDataMessage("Error loading irrigation data: " + databaseError.getMessage());
                }
            });
    }
    
    // Helper method to extract timestamp from Firebase push ID
    private long extractTimestampFromPushId(String pushId) {
        // Firebase push IDs contain a timestamp component
        // The first 8 characters represent the timestamp in seconds
        try {
            String timestampStr = pushId.substring(0, 8);
            return Long.parseLong(timestampStr, 16) * 1000; // Convert to milliseconds
        } catch (Exception e) {
            Log.e(TAG, "Error extracting timestamp from push ID: " + e.getMessage());
            return System.currentTimeMillis(); // Fallback to current time
        }
    }
    
    private void populateTable(List<SensorData> sensorDataList) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot populate table");
            return;
        }
        
        Log.d(TAG, "Populating table with " + sensorDataList.size() + " records");
        
        // Show table, hide no data message
        tableLayout.setVisibility(View.VISIBLE);
        noDataMessage.setVisibility(View.GONE);
        
        // Clear existing rows except header
        int childCount = tableLayout.getChildCount();
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
        
        // Add data rows
        for (SensorData data : sensorDataList) {
            addTableRow(data);
        }
    }
    
    private void addTableRow(SensorData data) {
        TableRow row = new TableRow(getContext());
        
        // Farm number
        TextView farmText = createTextView(String.valueOf(data.getFarm()), 110);
        row.addView(farmText);
        
        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(new Date(data.getTimestamp()));
        TextView dateText = createTextView(formattedDate, 170);
        row.addView(dateText);
        
        // Get water pump status and runtime
        String pumpStatus = "Unknown";
        String pumpRuntime = "0.00";
        
        if (data.getWaterPumpStatus() != null) {
            // Status
            Object statusObj = data.getWaterPumpStatus().get("status");
            if (statusObj != null) {
                pumpStatus = statusObj.toString();
            }
            
            // Runtime
            Object runtimeObj = data.getWaterPumpStatus().get("pump_runtime");
            if (runtimeObj != null) {
                pumpRuntime = runtimeObj.toString();
            }
        }
        
        // Description - based on status
        String description = "Water Pump " + pumpStatus.toUpperCase();
        TextView descText = createTextView(description, 170);
        
        // Set color based on status
        if ("on".equalsIgnoreCase(pumpStatus)) {
            descText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            descText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        
        row.addView(descText);
        
        // Runtime
        TextView runtimeText = createTextView(pumpRuntime + " seconds", 110);
        row.addView(runtimeText);
        
        // Add row to table
        tableLayout.addView(row);
    }
    
    private TextView createTextView(String text, int widthDp) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setPadding(16, 12, 16, 12);
        
        // Let text wrap to prevent cutting off
        textView.setSingleLine(false);
        textView.setMaxLines(2);
        
        // Set width in dp but allow wrapping
        int width = (int) (widthDp * getResources().getDisplayMetrics().density);
        TableRow.LayoutParams params = new TableRow.LayoutParams(width, TableRow.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(params);
        
        // Better text ellipsizing
        textView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        // Adjust text size to fit available space
        textView.setTextSize(13);  // Match header text size
        
        return textView;
    }
    
    private void showNoDataMessage(String message) {
        if (getContext() == null) return;
        
        tableLayout.setVisibility(View.GONE);
        noDataMessage.setVisibility(View.VISIBLE);
        noDataMessage.setText(message);
        
        Log.d(TAG, "Showing no data message: " + message);
    }
    
    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "Toast message: " + message);
    }
} 