package com.example.finalproject_vertigrow.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.SensorData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IrrigationFragment extends Fragment {
    private static final String TAG = "IrrigationFragment";
    
    private RecyclerView recyclerIrrigation;
    private TextView titleTextView;
    private TextView noDataMessage;
    private ProgressBar progressBar;
    
    private DatabaseReference sensorsRef;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private Set<Integer> userFarmIds = new HashSet<>();
    private IrrigationAdapter adapter;
    
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
        db = FirebaseFirestore.getInstance();
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
        recyclerIrrigation = view.findViewById(R.id.recycler_irrigation);
        titleTextView = view.findViewById(R.id.farm_title);
        noDataMessage = view.findViewById(R.id.no_data_message);
        progressBar = view.findViewById(R.id.progress_bar);
        
        // Setup RecyclerView
        recyclerIrrigation.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new IrrigationAdapter();
        recyclerIrrigation.setAdapter(adapter);
        
        // First fetch user's farms, then load irrigation data
        fetchUserFarms();
    }
    
    private void fetchUserFarms() {
        if (currentUserId == null) {
            Log.d(TAG, "No current user found");
            showMessage("Please sign in to view irrigation data");
            return;
        }
        
        Log.d(TAG, "Fetching farms for user: " + currentUserId);
        
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        noDataMessage.setVisibility(View.GONE);
        recyclerIrrigation.setVisibility(View.GONE);
        
        // Query Firestore for farms belonging to current user
        db.collection("farms")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userFarmIds.clear();
                    
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            // Get farm number/ID
                            Object farmObj = document.get("farm");
                            if (farmObj != null) {
                                int farmId;
                                if (farmObj instanceof Long) {
                                    farmId = ((Long) farmObj).intValue();
                                } else if (farmObj instanceof Integer) {
                                    farmId = (Integer) farmObj;
                                } else {
                                    farmId = Integer.parseInt(farmObj.toString());
                                }
                                userFarmIds.add(farmId);
                                Log.d(TAG, "Added farm ID to user's farms: " + farmId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing farm data: " + e.getMessage(), e);
                        }
                    }
                    
                    Log.d(TAG, "User has " + userFarmIds.size() + " farms");
                    
                    // Now load irrigation data filtered by user's farms
                    loadIrrigationData();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error fetching user farms", task.getException());
                    showNoDataMessage("Error loading farms: " + task.getException().getMessage());
                }
            });
    }
    
    private void loadIrrigationData() {
        if (currentUserId == null) {
            Log.d(TAG, "No current user found");
            showMessage("Please sign in to view irrigation data");
            return;
        }
        
        if (userFarmIds.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Log.d(TAG, "User has no farms");
            showNoDataMessage("You don't have any farms set up yet");
            return;
        }
        
        Log.d(TAG, "Loading irrigation data for user's farms");
        
        // Get the latest 100 irrigation records
        sensorsRef.orderByKey().limitToLast(100)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Data snapshot received");
                    
                    if (!dataSnapshot.exists()) {
                        Log.d(TAG, "No data exists in snapshot");
                        showNoDataMessage("No irrigation data available");
                        return;
                    }
                    
                    List<SensorData> irrigationData = new ArrayList<>();
                    int totalRecords = 0;
                    int filteredRecords = 0;
                    
                    // Process each document
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        totalRecords++;
                        try {
                            // Get the push ID
                            String pushId = snapshot.getKey();
                            
                            // Get the data
                            Map<String, Object> data = (Map<String, Object>) snapshot.getValue();
                            if (data != null) {
                                // Check if farm ID is in user's farms
                                Object farmValue = data.get("farm");
                                if (farmValue != null) {
                                    int farmId;
                                    try {
                                        farmId = Integer.parseInt(farmValue.toString());
                                        
                                        // Skip if this farm doesn't belong to user
                                        if (!userFarmIds.contains(farmId)) {
                                            continue;
                                        }
                                        
                                        Log.d(TAG, "Found record for user's farm: " + farmId);
                                        filteredRecords++;
                                        
                                        // Check if it has water pump data
                                        if (data.containsKey("water_pump_status")) {
                                            Map<String, Object> pumpStatus = (Map<String, Object>) data.get("water_pump_status");
                                            if (pumpStatus != null) {
                                                // Create sensor data
                                                SensorData sensorData = new SensorData();
                                                sensorData.setId(pushId);
                                                sensorData.setFarm(farmId);
                                                sensorData.setWaterPumpStatus(pumpStatus);
                                                
                                                // Add timestamp from push ID
                                                long timestamp = extractTimestampFromPushId(pushId);
                                                sensorData.setTimestamp(timestamp);
                                                
                                                irrigationData.add(sensorData);
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "Error parsing farm ID: " + e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing sensor data: " + e.getMessage(), e);
                        }
                    }
                    
                    Log.d(TAG, String.format("Processed %d total records, found %d records for user's farms",
                        totalRecords, filteredRecords));
                    
                    if (irrigationData.isEmpty()) {
                        Log.d(TAG, "No irrigation data found for user's farms");
                        showNoDataMessage("No irrigation data available for your farms");
                    } else {
                        Log.d(TAG, "Found " + irrigationData.size() + " irrigation records to display");
                        // Sort by timestamp in descending order
                        Collections.sort(irrigationData, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        updateRecyclerView(irrigationData);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    progressBar.setVisibility(View.GONE);
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
    
    private void updateRecyclerView(List<SensorData> sensorDataList) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot update RecyclerView");
            return;
        }
        
        recyclerIrrigation.setVisibility(View.VISIBLE);
        noDataMessage.setVisibility(View.GONE);
        
        adapter.setIrrigationData(sensorDataList);
    }
    
    private void showNoDataMessage(String message) {
        if (getContext() == null) return;
        
        recyclerIrrigation.setVisibility(View.GONE);
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
    
    private class IrrigationAdapter extends RecyclerView.Adapter<IrrigationAdapter.IrrigationViewHolder> {
        private List<SensorData> irrigationDataList = new ArrayList<>();
        
        public void setIrrigationData(List<SensorData> irrigationData) {
            this.irrigationDataList = irrigationData;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public IrrigationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_irrigation, parent, false);
            return new IrrigationViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull IrrigationViewHolder holder, int position) {
            SensorData data = irrigationDataList.get(position);
            
            // Set farm number
            holder.textFarmId.setText("Farm #" + data.getFarm());
            
            // Format and set timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String formattedDate = sdf.format(new Date(data.getTimestamp()));
            holder.textDateTime.setText(formattedDate);
            
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
            
            // Set status text and color
            String statusText = "Water Pump " + pumpStatus.toUpperCase();
            holder.textStatus.setText(statusText);
            
            if ("on".equalsIgnoreCase(pumpStatus)) {
                holder.textStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            } else {
                holder.textStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            }
            
            // Set runtime
            holder.textRuntime.setText(pumpRuntime + " seconds");
        }
        
        @Override
        public int getItemCount() {
            return irrigationDataList.size();
        }
        
        class IrrigationViewHolder extends RecyclerView.ViewHolder {
            TextView textFarmId, textDateTime, textStatus, textRuntime, textRuntimeLabel;
            ImageView imageStatus;
            
            IrrigationViewHolder(@NonNull View itemView) {
                super(itemView);
                textFarmId = itemView.findViewById(R.id.text_farm_id);
                textDateTime = itemView.findViewById(R.id.text_date_time);
                textStatus = itemView.findViewById(R.id.text_status);
                textRuntime = itemView.findViewById(R.id.text_runtime);
                textRuntimeLabel = itemView.findViewById(R.id.text_runtime_label);
                imageStatus = itemView.findViewById(R.id.image_status);
            }
        }
    }
} 