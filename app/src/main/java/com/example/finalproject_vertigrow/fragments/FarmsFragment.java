package com.example.finalproject_vertigrow.fragments;

import android.app.Dialog;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FarmsFragment extends Fragment {
    private RecyclerView recyclerView;
    private FarmAdapter adapter;
    private FloatingActionButton addButton;
    private FirebaseDatabase database;
    private DatabaseReference farmsRef;
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
        database = FirebaseDatabase.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        
        if (currentUser != null) {
            String userId = currentUser.getUid();
            farmsRef = database.getReference("users").child(userId).child("farms");
        }
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView_farms);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FarmAdapter();
        recyclerView.setAdapter(adapter);
        
        // Initialize Add Farm button
        addButton = view.findViewById(R.id.button_add_farm);
        addButton.setOnClickListener(v -> showAddFarmDialog());
        
        // Load farms from Firebase
        loadFarms();
    }
    
    private void loadFarms() {
        if (farmsRef == null) return;
        
        farmsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Farm> farmsList = new ArrayList<>();
                
                for (DataSnapshot farmSnapshot : snapshot.getChildren()) {
                    Farm farm = farmSnapshot.getValue(Farm.class);
                    if (farm != null) {
                        farm.setId(farmSnapshot.getKey());
                        farmsList.add(farm);
                    }
                }
                
                adapter.setFarms(farmsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error loading farms: " + error.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
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
            
            // Create new farm
            Farm newFarm = new Farm(plantName, plantType);
            
            // Save to Firebase
            saveFarmToFirebase(newFarm);
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void saveFarmToFirebase(Farm farm) {
        if (farmsRef == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Generate a new unique key for the farm
        String farmId = farmsRef.push().getKey();
        
        if (farmId != null) {
            farm.setId(farmId);
            farmsRef.child(farmId).setValue(farm)
                    .addOnSuccessListener(aVoid -> 
                        Toast.makeText(getContext(), "Farm added successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> 
                        Toast.makeText(getContext(), "Failed to add farm: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show());
        }
    }
} 