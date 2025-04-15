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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.adapters.FarmAdapter;
import com.example.finalproject_vertigrow.models.Farm;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
                        farm.setOnline(document.getBoolean("isOnline") != null ? 
                                document.getBoolean("isOnline") : false);
                        
                        // Get the farm number from Firestore
                        Long farmNumber = document.getLong("farm");
                        if (farmNumber != null) {
                            farm.setFarm(farmNumber.intValue());
                        }
                        
                        farmsList.add(farm);
                        Log.d(TAG, "Loaded farm: " + farm.getPlantName() + ", ID: " + farm.getId() + ", Farm: " + farm.getFarm());
                    }
                    
                    adapter.setFarms(farmsList);
                });
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
                    
                    // Now save the farm with the new farm number
                    farmsCollection.add(farmData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Farm added successfully with ID: " + documentReference.getId());
                                Toast.makeText(getContext(), "Farm added successfully", Toast.LENGTH_SHORT).show();
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
} 