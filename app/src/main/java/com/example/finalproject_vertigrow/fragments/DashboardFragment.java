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
import com.example.finalproject_vertigrow.fragments.ActivityLogsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    
    private TextView textRacksCount;
    private TextView textActiveFarmsCount;
    private ProgressBar progressLoading;
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DatabaseReference sensorsRef;
    
    private String currentUserId;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");
        
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Initialize views
        textRacksCount = view.findViewById(R.id.text_racks_count);
        textActiveFarmsCount = view.findViewById(R.id.text_active_farms_count);
        progressLoading = view.findViewById(R.id.progress_loading);
        
        // Hide recent activities section
        TextView recentActivitiesTitle = view.findViewById(R.id.text_recent_activities);
        if (recentActivitiesTitle != null) {
            recentActivitiesTitle.setVisibility(View.GONE);
        }
        
        View recyclerActivities = view.findViewById(R.id.recycler_activities);
        if (recyclerActivities != null) {
            recyclerActivities.setVisibility(View.GONE);
        }
        
        // Load dashboard data
        loadDashboardData();
    }
    
    private void loadDashboardData() {
        progressLoading.setVisibility(View.VISIBLE);
        
        // Load total farms count for current user
        loadTotalFarmsCount();
        
        // Load active farms count
        loadActiveFarmsCount();
    }
    
    private void loadTotalFarmsCount() {
        db.collection("farms")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalFarms = queryDocumentSnapshots.size();
                    textRacksCount.setText(String.valueOf(totalFarms));
                    Log.d(TAG, "Total farms: " + totalFarms);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading total farms: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading farm data", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadActiveFarmsCount() {
        // First get all farms for the current user
        db.collection("farms")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Get all farm IDs
                    Set<Integer> farmIds = new HashSet<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        if (document.contains("farm")) {
                            Object farmObj = document.get("farm");
                            if (farmObj instanceof Long) {
                                farmIds.add(((Long) farmObj).intValue());
                            } else if (farmObj instanceof Integer) {
                                farmIds.add((Integer) farmObj);
                            }
                        }
                    }
                    
                    // If there are no farms, set count to 0
                    if (farmIds.isEmpty()) {
                        textActiveFarmsCount.setText("0");
                        progressLoading.setVisibility(View.GONE);
                        return;
                    }
                    
                    // Check which farms have sensor data (are active)
                    checkActiveFarms(farmIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading farms: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error loading farm data", Toast.LENGTH_SHORT).show();
                    progressLoading.setVisibility(View.GONE);
                });
    }
    
    private void checkActiveFarms(Set<Integer> farmIds) {
        final int[] activeCount = {0};
        final int[] checkedCount = {0};
        
        for (Integer farmId : farmIds) {
            sensorsRef.orderByChild("farm").equalTo(farmId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            checkedCount[0]++;
                            
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                // This farm is active
                                activeCount[0]++;
                            }
                            
                            // If all farms have been checked, update the UI
                            if (checkedCount[0] == farmIds.size()) {
                                textActiveFarmsCount.setText(String.valueOf(activeCount[0]));
                                progressLoading.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error checking farm activity: " + databaseError.getMessage());
                            checkedCount[0]++;
                            
                            // If all farms have been checked, update the UI
                            if (checkedCount[0] == farmIds.size()) {
                                textActiveFarmsCount.setText(String.valueOf(activeCount[0]));
                                progressLoading.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }
} 