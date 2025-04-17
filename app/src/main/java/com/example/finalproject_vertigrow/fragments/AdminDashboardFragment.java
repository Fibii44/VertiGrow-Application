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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class AdminDashboardFragment extends Fragment {
    
    private static final String TAG = "AdminDashboardFragment";
    
    // UI components
    private TextView textUsersCount;
    private TextView textFarmsCount;
    private TextView textSensorsCount;
    private ProgressBar progressLoading;
    
    // Firebase references
    private FirebaseFirestore db;
    private DatabaseReference sensorsRef;

    public AdminDashboardFragment() {
        // Required empty public constructor
    }

    public static AdminDashboardFragment newInstance() {
        return new AdminDashboardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");
        
        // Initialize UI components
        textUsersCount = view.findViewById(R.id.text_users_count);
        textFarmsCount = view.findViewById(R.id.text_farms_count);
        textSensorsCount = view.findViewById(R.id.text_sensors_count);
        progressLoading = view.findViewById(R.id.progress_loading);
        
        // Load initial data
        loadDashboardData();
    }
    
    private void loadDashboardData() {
        // Show loading indicator
        progressLoading.setVisibility(View.VISIBLE);
        
        // Reset counts to ensure a clean slate
        textUsersCount.setText("0");
        textFarmsCount.setText("0");
        textSensorsCount.setText("0");
        
        // Load count of users with role 'user'
        db.collection("users")
            .whereEqualTo("role", "user")
            .get()
            .addOnSuccessListener(userSnapshot -> {
                int userCount = userSnapshot.size();
                textUsersCount.setText(String.valueOf(userCount));
                
                // Log the count for debugging
                Log.d(TAG, "Found " + userCount + " users");
                
                // Continue with loading farms
                loadFarmsCount();
            })
            .addOnFailureListener(e -> {
                progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Error loading users count", e);
                Toast.makeText(requireContext(), "Error loading users data", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadFarmsCount() {
        db.collection("farms")
            .get()
            .addOnSuccessListener(farmSnapshot -> {
                int farmCount = farmSnapshot.size();
                textFarmsCount.setText(String.valueOf(farmCount));
                
                // Log the count for debugging
                Log.d(TAG, "Found " + farmCount + " farms");
                
                // Continue with loading active sensors
                loadActiveSensorsCount();
            })
            .addOnFailureListener(e -> {
                progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Error loading farms count", e);
                Toast.makeText(requireContext(), "Error loading farms data", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadActiveSensorsCount() {
        sensorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // We'll count unique farm IDs with active sensors
                Set<Integer> activeFarms = new HashSet<>();
                
                for (DataSnapshot sensorSnapshot : dataSnapshot.getChildren()) {
                    // Check if this record has farm data
                    Object farmObj = sensorSnapshot.child("farm").getValue();
                    if (farmObj != null) {
                        try {
                            int farmId = Integer.parseInt(farmObj.toString());
                            activeFarms.add(farmId);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing farm ID", e);
                        }
                    }
                }
                
                int sensorCount = activeFarms.size();
                textSensorsCount.setText(String.valueOf(sensorCount));
                
                // Log the count for debugging
                Log.d(TAG, "Found " + sensorCount + " farms with sensors");
                
                // Hide loading indicator
                progressLoading.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressLoading.setVisibility(View.GONE);
                Log.e(TAG, "Error loading sensor data", databaseError.toException());
                Toast.makeText(requireContext(), "Error loading sensor data", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 