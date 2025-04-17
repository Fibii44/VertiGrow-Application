package com.example.finalproject_vertigrow.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.adapters.FarmAdapter;
import com.example.finalproject_vertigrow.models.Farm;
import com.example.finalproject_vertigrow.models.LogEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmsFragment extends Fragment {
    private static final String TAG = "FarmsFragment";
    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private FloatingActionButton addButton;
    private FirebaseFirestore db;
    private CollectionReference farmsCollection;
    private FirebaseAuth auth;

    public FarmsFragment() {
        // Required empty public constructor
    }

    public static FarmsFragment newInstance() {
        return new FarmsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_farms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + userId);
            
            // Use the 'farms' collection in Firestore
            farmsCollection = db.collection("farms");
            Log.d(TAG, "Firestore collection path: farms");
        } else {
            Log.e(TAG, "User is not authenticated");
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
        }
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView_farms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FarmAdapter(requireActivity());
        
        // Set the edit click listener for farms
        adapter.setOnEditClickListener(farm -> showEditFarmDialog(farm));
        
        recyclerView.setAdapter(adapter);
        
        // Initialize Add Farm button
        addButton = view.findViewById(R.id.button_add_farm);
        addButton.setOnClickListener(v -> showAddFarmDialog());
        
        // Load farms from Firestore
        loadFarms();
    }
    
    private void loadFarms() {
        if (farmsCollection == null) {
            Log.e(TAG, "farmsCollection is null, cannot load farms");
            return;
        }
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User is not authenticated, cannot load farms");
            return;
        }
        
        String userId = currentUser.getUid();
        Log.d(TAG, "Loading farms for user: " + userId);
        
        // Query farms where userId matches the current user
        farmsCollection.whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading farms: " + error.getMessage());
                        Toast.makeText(getContext(), "Error loading farms: " + error.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (value == null) {
                        Log.d(TAG, "No farms found");
                        adapter.setFarms(new ArrayList<>());
                        return;
                    }
                    
                    Log.d(TAG, "Data changed, number of farms: " + value.size());
                    
                    List<Farm> farmsList = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : value) {
                        Farm farm = new Farm();
                        farm.setId(document.getId());
                        farm.setUserId(document.getString("userId"));
                        farm.setPlantName(document.getString("plantName"));
                        farm.setPlantType(document.getString("plantType"));
                        
                        // Get the farm number from Firestore
                        Long farmNumber = document.getLong("farm");
                        if (farmNumber != null) {
                            farm.setFarm(farmNumber.intValue());
                            
                            // Check if this farm has recent sensor data
                            checkFarmOnlineStatus(farm);
                        }
                        
                        farmsList.add(farm);
                        Log.d(TAG, "Loaded farm: " + farm.getPlantName() + ", ID: " + farm.getId() + ", Farm: " + farm.getFarm());
                    }
                    
                    adapter.setFarms(farmsList);
                });
    }
    
    private void checkFarmOnlineStatus(Farm farm) {
        Log.d(TAG, "Checking online status for farm: " + farm.getFarm());
        
        // Get reference to Firebase Realtime Database
        DatabaseReference sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");
        
        // Query for any sensor data for this farm
        sensorsRef.orderByChild("farm").equalTo(farm.getFarm())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Received sensor data snapshot for farm " + farm.getFarm() + ", exists: " + dataSnapshot.exists());
                        Log.d(TAG, "Number of children: " + dataSnapshot.getChildrenCount());
                        
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            Log.d(TAG, "Sensor data exists for farm " + farm.getFarm() + ", setting online=true");
                            // If any sensor data exists for this farm, it's online
                            farm.setOnline(true);
                        } else {
                            Log.d(TAG, "No sensor data exists for farm " + farm.getFarm() + ", setting online=false");
                            farm.setOnline(false);
                        }
                        
                        // Update the farm's online status in Firestore
                        updateFarmOnlineStatus(farm);
                        
                        // Also update the adapter directly if needed
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error checking farm online status: " + databaseError.getMessage());
                        farm.setOnline(false);
                        updateFarmOnlineStatus(farm);
                    }
                });
    }
    
    private void updateFarmOnlineStatus(Farm farm) {
        if (farmsCollection != null) {
            Log.d(TAG, "Updating online status for farm " + farm.getFarm() + " to: " + farm.isOnline());
            
            farmsCollection.document(farm.getId())
                    .update("isOnline", farm.isOnline())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully updated online status for farm " + farm.getFarm());
                        // Refresh the adapter to update the UI
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating farm online status: " + e.getMessage());
                    });
        } else {
            Log.e(TAG, "farmsCollection is null, cannot update online status");
        }
    }
    
    private void showAddFarmDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_farm);
        
        // Initialize dialog views
        TextInputEditText plantNameEditText = dialog.findViewById(R.id.edit_plant_name);
        AutoCompleteTextView plantTypeDropdown = dialog.findViewById(R.id.dropdown_plant_type);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);
        Button saveButton = dialog.findViewById(R.id.button_save);
        
        // Setup plant type dropdown
        String[] plantTypes = getResources().getStringArray(R.array.plant_types);
        ArrayAdapter<String> plantTypeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                plantTypes
        );
        plantTypeDropdown.setAdapter(plantTypeAdapter);
        
        // Setup button click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            String plantName = plantNameEditText.getText() != null ? 
                    plantNameEditText.getText().toString().trim() : "";
            String plantType = plantTypeDropdown.getText() != null ? 
                    plantTypeDropdown.getText().toString().trim() : "";
            
            // Validate inputs
            if (plantName.isEmpty()) {
                plantNameEditText.setError("Please enter a plant name");
                return;
            }
            
            if (plantType.isEmpty()) {
                plantTypeDropdown.setError("Please select a plant type");
                return;
            }
            
            // Get current user ID
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String userId = currentUser.getUid();
            Log.d(TAG, "Creating new farm for user: " + userId);
            
            // Create farm data for Firestore
            Map<String, Object> farmData = new HashMap<>();
            farmData.put("userId", userId);
            farmData.put("plantName", plantName);
            farmData.put("plantType", plantType);
            farmData.put("isOnline", false);
            
            // Save to Firestore
            saveFarmToFirestore(farmData);
            
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void showEditFarmDialog(Farm farm) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_farm);
        
        // Change title to indicate we're editing
        TextView titleTextView = dialog.findViewById(R.id.dialog_title);
        if (titleTextView != null) {
            titleTextView.setText("Edit Farm");
        }
        
        // Initialize dialog views
        TextInputEditText plantNameEditText = dialog.findViewById(R.id.edit_plant_name);
        AutoCompleteTextView plantTypeDropdown = dialog.findViewById(R.id.dropdown_plant_type);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);
        Button saveButton = dialog.findViewById(R.id.button_save);
        
        // Pre-fill with existing farm data
        plantNameEditText.setText(farm.getPlantName());
        plantTypeDropdown.setText(farm.getPlantType());
        
        // Setup plant type dropdown
        String[] plantTypes = getResources().getStringArray(R.array.plant_types);
        ArrayAdapter<String> plantTypeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                plantTypes
        );
        plantTypeDropdown.setAdapter(plantTypeAdapter);
        
        // Setup button click listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        saveButton.setOnClickListener(v -> {
            String plantName = plantNameEditText.getText() != null ? 
                    plantNameEditText.getText().toString().trim() : "";
            String plantType = plantTypeDropdown.getText() != null ? 
                    plantTypeDropdown.getText().toString().trim() : "";
            
            // Validate inputs
            if (plantName.isEmpty()) {
                plantNameEditText.setError("Please enter a plant name");
                return;
            }
            
            if (plantType.isEmpty()) {
                plantTypeDropdown.setError("Please select a plant type");
                return;
            }
            
            // Update farm data
            Map<String, Object> updates = new HashMap<>();
            updates.put("plantName", plantName);
            updates.put("plantType", plantType);
            
            // Update in Firestore
            updateFarm(farm.getId(), updates);
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void updateFarm(String farmId, Map<String, Object> updates) {
        if (farmsCollection == null) {
            Log.e(TAG, "farmsCollection is null, cannot update farm");
            return;
        }
        
        Log.d(TAG, "Updating farm with ID: " + farmId);
        
        // Get the plant name for the log entry
        String plantName = updates.containsKey("plantName") ? 
                updates.get("plantName").toString() : "unknown";
        
        farmsCollection.document(farmId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Farm updated successfully");
                    Toast.makeText(getContext(), "Farm updated successfully", Toast.LENGTH_SHORT).show();
                    
                    // Log the farm update action
                    logFarmAction("FARM_UPDATE", "Updated farm " + plantName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating farm: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to update farm: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void saveFarmToFirestore(Map<String, Object> farmData) {
        if (farmsCollection == null) {
            Log.e(TAG, "farmsCollection is null, cannot save farm");
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Saving farm data: " + farmData.toString());

        // Query to get the highest farm number
        farmsCollection
                .orderBy("farm", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextFarm = 1; // Default start value
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the highest farm number and add 1
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        Long currentHighest = documentSnapshot.getLong("farm");
                        if (currentHighest != null) {
                            nextFarm = currentHighest.intValue() + 1;
                        }
                    }
                    
                    // Add farm number to the farm data
                    farmData.put("farm", nextFarm);
                    
                    // Get the plant name for the log entry
                    String plantName = farmData.containsKey("plantName") ? 
                            farmData.get("plantName").toString() : "unknown";
                    
                    // Now save the farm with the new farm number
                    farmsCollection.add(farmData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Farm added successfully with ID: " + documentReference.getId());
                                Toast.makeText(getContext(), "Farm added successfully", Toast.LENGTH_SHORT).show();
                                
                                // Log the farm add action
                                logFarmAction("FARM_ADD", "Added new farm " + plantName);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to add farm: " + e.getMessage());
                                Log.e(TAG, "Exception: " + e.getClass().getName());
                                Toast.makeText(getContext(), "Failed to add farm: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting highest farm number: " + e.getMessage());
                    Toast.makeText(getContext(), "Error creating farm: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Log farm-related actions to Firestore
     * 
     * @param action The type of action (e.g., "FARM_ADD", "FARM_UPDATE", "FARM_DELETE")
     * @param description A description of the action
     */
    private void logFarmAction(String action, String description) {
        // Get the current user's ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        
        // Get the current user's email for better identification
        String userEmail = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        
        // Add user email to description for better context
        if (userEmail != null && !userEmail.isEmpty()) {
            description += " by " + userEmail;
        }
        
        LogEntry logEntry = new LogEntry(
                null,               // id - will be assigned by Firestore
                currentUserId,      // userId
                null,               // farmId - could be set if specific to a farm
                description,        // description
                System.currentTimeMillis(), // timestamp
                action              // type
        );
        
        // Add log entry to Firestore
        FirebaseFirestore.getInstance().collection("logs")
                .add(logEntry)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Log entry added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding log entry: " + e.getMessage());
                });
    }
} 