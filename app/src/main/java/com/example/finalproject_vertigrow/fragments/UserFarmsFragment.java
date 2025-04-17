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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.Farm;
import com.example.finalproject_vertigrow.models.LogEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFarmsFragment extends Fragment {
    private static final String TAG = "UserFarmsFragment";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_USER_NAME = "userName";

    private String userId;
    private String userName;
    private RecyclerView recyclerFarms;
    private ProgressBar progressBar;
    private TextView textNoFarms;
    private TextView textUserName;
    private ImageButton buttonBack;
    private FloatingActionButton fabAddFarm;
    private FirebaseFirestore db;
    private DatabaseReference sensorsRef;
    private FarmAdapter adapter;

    public UserFarmsFragment() {
        // Required empty public constructor
    }

    public static UserFarmsFragment newInstance(String userId, String userName) {
        UserFarmsFragment fragment = new UserFarmsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_USER_NAME, userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            userName = getArguments().getString(ARG_USER_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_farms, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");

        // Initialize views
        recyclerFarms = view.findViewById(R.id.recycler_farms);
        progressBar = view.findViewById(R.id.progress_bar);
        textNoFarms = view.findViewById(R.id.text_no_farms);
        textUserName = view.findViewById(R.id.text_user_name);
        buttonBack = view.findViewById(R.id.button_back);
        fabAddFarm = view.findViewById(R.id.fab_add_farm);

        // Set user name
        textUserName.setText(userName + "'s Farms");

        // Setup RecyclerView
        recyclerFarms.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FarmAdapter();
        recyclerFarms.setAdapter(adapter);

        // Setup back button
        buttonBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        
        // Setup add farm button
        fabAddFarm.setOnClickListener(v -> showAddEditFarmDialog(null));

        // Load farms
        loadFarmsForUser();
    }

    private void loadFarmsForUser() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerFarms.setVisibility(View.GONE);
        textNoFarms.setVisibility(View.GONE);

        db.collection("farms")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        List<Farm> farmsList = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Farm farm = new Farm();
                            farm.setId(document.getId());
                            farm.setUserId(document.getString("userId"));
                            farm.setPlantName(document.getString("plantName"));
                            farm.setPlantType(document.getString("plantType"));
                            
                            // Get the farm number from Firestore
                            Object farmNumberObj = document.get("farm");
                            if (farmNumberObj instanceof Long) {
                                farm.setFarm(((Long) farmNumberObj).intValue());
                            } else if (farmNumberObj instanceof String) {
                                farm.setFarm((String) farmNumberObj);
                            }
                            
                            // We'll check online status by querying Realtime Database later
                            farm.setOnline(false);
                            
                            farmsList.add(farm);
                            Log.d(TAG, "Loaded farm: " + farm.getPlantName() + ", ID: " + farm.getId() + ", Farm: " + farm.getFarm());
                        }

                        // First update UI with all farms
                        adapter.setFarms(farmsList);
                        updateEmptyView(farmsList.isEmpty());
                        
                        // Then check online status for each farm
                        for (Farm farm : farmsList) {
                            checkFarmOnlineStatus(farm);
                        }
                    } else {
                        Log.e(TAG, "Error loading farms: " + task.getException().getMessage());
                        Toast.makeText(requireContext(), "Error loading farms: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateEmptyView(true);
                    }
                });
    }
    
    private void checkFarmOnlineStatus(Farm farm) {
        Log.d(TAG, "Checking online status for farm: " + farm.getFarm());
        
        // Log the database reference to confirm we're looking at the right path
        Log.d(TAG, "Database reference path: " + sensorsRef.toString());
        
        // Query for any sensor data for this farm in Realtime Database
        sensorsRef.orderByChild("farm").equalTo(farm.getFarm())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Received sensor data snapshot for farm " + farm.getFarm() + 
                                ", exists: " + dataSnapshot.exists() + 
                                ", children count: " + dataSnapshot.getChildrenCount());
                        
                        if (dataSnapshot.exists()) {
                            // Log each child to see what's in the snapshot
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                Log.d(TAG, "Sensor data key: " + child.getKey());
                                Log.d(TAG, "Farm value: " + child.child("farm").getValue());
                            }
                        }
                        
                        boolean isOnline = dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0;
                        Log.d(TAG, "Setting farm " + farm.getFarm() + " online status to: " + isOnline);
                        
                        // Update the farm's online status
                        farm.setOnline(isOnline);
                        
                        // Update the farm in Firestore
                        updateFarmOnlineStatus(farm);
                        
                        // Update the UI
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error checking farm online status: " + databaseError.getMessage());
                    }
                });
    }
    
    private void updateFarmOnlineStatus(Farm farm) {
        // Update online status in Firestore
        db.collection("farms").document(farm.getId())
                .update("isOnline", farm.isOnline())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully updated online status for farm " + farm.getFarm() + ": " + farm.isOnline());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating farm online status: " + e.getMessage());
                });
    }

    private void updateEmptyView(boolean isEmpty) {
        if (isEmpty) {
            textNoFarms.setVisibility(View.VISIBLE);
            recyclerFarms.setVisibility(View.GONE);
        } else {
            textNoFarms.setVisibility(View.GONE);
            recyclerFarms.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddEditFarmDialog(Farm farm) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_farm);
        
        // Initialize dialog views
        TextInputEditText plantNameEditText = dialog.findViewById(R.id.edit_plant_name);
        AutoCompleteTextView plantTypeDropdown = dialog.findViewById(R.id.dropdown_plant_type);
        Button cancelButton = dialog.findViewById(R.id.button_cancel);
        Button saveButton = dialog.findViewById(R.id.button_save);
        
        // Set existing values if editing
        if (farm != null) {
            plantNameEditText.setText(farm.getPlantName());
            plantTypeDropdown.setText(farm.getPlantType());
        }
        
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
            
            // Create farm data for Firestore
            Map<String, Object> farmData = new HashMap<>();
            farmData.put("userId", userId); // Important: Use the user's ID, not admin's
            farmData.put("plantName", plantName);
            farmData.put("plantType", plantType);
            farmData.put("isOnline", false);
            
            if (farm == null) {
                // Adding a new farm
                saveFarmToFirestore(farmData);
            } else {
                // Updating an existing farm
                updateFarmInFirestore(farm.getId(), farmData);
            }
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void saveFarmToFirestore(Map<String, Object> farmData) {
        progressBar.setVisibility(View.VISIBLE);
        
        // Query to get the highest farm number
        db.collection("farms")
                .orderBy("farm", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextFarm = 1; // Default start value
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the highest farm number and add 1
                        Object currentHighest = queryDocumentSnapshots.getDocuments().get(0).get("farm");
                        if (currentHighest instanceof Long) {
                            nextFarm = ((Long) currentHighest).intValue() + 1;
                        } else if (currentHighest instanceof Integer) {
                            nextFarm = (Integer) currentHighest + 1;
                        } else if (currentHighest instanceof String) {
                            try {
                                nextFarm = Integer.parseInt((String) currentHighest) + 1;
                            } catch (NumberFormatException e) {
                                // Keep default value
                            }
                        }
                    }
                    
                    // Add farm number to the farm data
                    farmData.put("farm", nextFarm);
                    
                    // Now save the farm with the new farm number
                    db.collection("farms").add(farmData)
                            .addOnSuccessListener(documentReference -> {
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "Farm added successfully with ID: " + documentReference.getId());
                                Toast.makeText(requireContext(), "Farm added successfully", Toast.LENGTH_SHORT).show();
                                
                                // Log the action
                                logFarmAction("FARM_ADD", "Added farm " + farmData.get("plantName") + " for user " + userName);
                                
                                // Reload farms to show the new one
                                loadFarmsForUser();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Failed to add farm: " + e.getMessage());
                                Toast.makeText(requireContext(), "Failed to add farm: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error getting highest farm number: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error creating farm: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateFarmInFirestore(String farmId, Map<String, Object> farmData) {
        progressBar.setVisibility(View.VISIBLE);
        
        db.collection("farms").document(farmId)
                .update(farmData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Farm updated successfully");
                    Toast.makeText(requireContext(), "Farm updated successfully", Toast.LENGTH_SHORT).show();
                    
                    // Log the action
                    logFarmAction("FARM_UPDATE", "Updated farm " + farmData.get("plantName") + " for user " + userName);
                    
                    // Reload farms to show the changes
                    loadFarmsForUser();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to update farm: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to update farm: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
    
    private void deleteFarm(Farm farm) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Farm")
                .setMessage("Are you sure you want to delete " + farm.getPlantName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    
                    db.collection("farms").document(farm.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Log.d(TAG, "Farm deleted successfully");
                                Toast.makeText(requireContext(), "Farm deleted successfully", Toast.LENGTH_SHORT).show();
                                
                                // Log the action
                                logFarmAction("FARM_DELETE", "Deleted farm " + farm.getPlantName() + " for user " + userName);
                                
                                // Reload farms to reflect the deletion
                                loadFarmsForUser();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Failed to delete farm: " + e.getMessage());
                                Toast.makeText(requireContext(), "Failed to delete farm: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void logFarmAction(String action, String description) {
        // Get the current user's ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        
        LogEntry logEntry = new LogEntry(
                null,               // id - will be assigned by Firestore
                currentUserId,      // userId
                null,               // farmId - could be set if specific to a farm
                description,        // description
                System.currentTimeMillis(), // timestamp
                action              // type
        );
        
        db.collection("logs")
                .add(logEntry)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Log entry added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding log entry: " + e.getMessage());
                });
    }

    private class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {
        private List<Farm> farms = new ArrayList<>();

        public void setFarms(List<Farm> farms) {
            this.farms = farms;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_farm, parent, false);
            return new FarmViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FarmViewHolder holder, int position) {
            Farm farm = farms.get(position);

            // Set farm details
            holder.textFarmId.setText("Farm #" + farm.getFarm());
            holder.textPlantName.setText(farm.getPlantName());
            holder.textPlantType.setText(farm.getPlantType());

            // Set online status icon, text and color
            if (farm.isOnline()) {
                holder.imageOnlineStatus.setImageResource(R.drawable.ic_online);
                holder.imageOnlineStatus.setColorFilter(ContextCompat.getColor(requireContext(), 
                    android.R.color.holo_green_dark));
                holder.textOnlineStatus.setText("Online");
                holder.textOnlineStatus.setTextColor(ContextCompat.getColor(requireContext(), 
                    android.R.color.holo_green_dark));
            } else {
                holder.imageOnlineStatus.setImageResource(R.drawable.ic_offline);
                holder.imageOnlineStatus.setColorFilter(ContextCompat.getColor(requireContext(), 
                    android.R.color.holo_red_dark));
                holder.textOnlineStatus.setText("Offline");
                holder.textOnlineStatus.setTextColor(ContextCompat.getColor(requireContext(), 
                    android.R.color.holo_red_dark));
            }
            
            // Setup more options button
            holder.buttonMore.setOnClickListener(v -> {
                showFarmOptions(v, farm);
            });
            
            // Add click listener to open sensor data
            holder.itemView.setOnClickListener(v -> {
                checkFarmSensorDataAndNavigate(farm.getFarm());
            });
        }
        
        private void showFarmOptions(View view, Farm farm) {
            PopupMenu popup = new PopupMenu(requireContext(), view);
            popup.inflate(R.menu.menu_farm_item);
            
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    showAddEditFarmDialog(farm);
                    return true;
                } else if (id == R.id.action_delete) {
                    deleteFarm(farm);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }

        @Override
        public int getItemCount() {
            return farms.size();
        }

        class FarmViewHolder extends RecyclerView.ViewHolder {
            TextView textFarmId, textPlantName, textPlantType, textOnlineStatus;
            ImageView imagePlantIcon, imageOnlineStatus;
            ImageButton buttonMore;

            FarmViewHolder(@NonNull View itemView) {
                super(itemView);
                textFarmId = itemView.findViewById(R.id.text_farm_id);
                textPlantName = itemView.findViewById(R.id.text_plant_name);
                textPlantType = itemView.findViewById(R.id.text_plant_type);
                textOnlineStatus = itemView.findViewById(R.id.text_online_status);
                imagePlantIcon = itemView.findViewById(R.id.image_plant_icon);
                imageOnlineStatus = itemView.findViewById(R.id.image_online_status);
                buttonMore = itemView.findViewById(R.id.button_more);
            }
        }
    }

    // Method to check for sensor data and navigate to appropriate fragment
    private void checkFarmSensorDataAndNavigate(int farmNumber) {
        // Show progress while checking
        progressBar.setVisibility(View.VISIBLE);
        
        // Query Realtime Database for sensor data for this farm
        sensorsRef.orderByChild("farm").equalTo(farmNumber)
                .limitToFirst(1)  // We only need to know if at least one sensor document exists
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        progressBar.setVisibility(View.GONE);
                        
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            // Sensor data exists for this farm
                            Log.d(TAG, "Navigating to SensorFragment for farm " + farmNumber);
                            
                            // Create and navigate to the sensor fragment
                            SensorFragment sensorFragment = SensorFragment.newInstance(farmNumber);
                            requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, sensorFragment)
                                .addToBackStack(null)
                                .commit();
                        } else {
                            // No sensor data exists for this farm
                            Log.d(TAG, "Navigating to NoSensorsFragment for farm " + farmNumber);
                            
                            // Create and navigate to the no sensors fragment
                            String title = "No Sensor Data Available";
                            String description = "Farm #" + farmNumber + " does not have any sensors connected or the sensors are not sending data currently.";
                            
                            NoSensorsFragment noSensorsFragment = NoSensorsFragment.newInstance(title, description);
                            requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, noSensorsFragment)
                                .addToBackStack(null)
                                .commit();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error checking sensor data: " + databaseError.getMessage());
                        Toast.makeText(requireContext(), 
                                "Error checking sensors: " + databaseError.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 