package com.example.finalproject_vertigrow.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.finalproject_vertigrow.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SensorFragment extends Fragment {
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_FARM_NAME = "farm_name";
    
    private String farmId;
    private String farmName;
    private TextView titleTextView;
    private DatabaseReference sensorRef;

    public static SensorFragment newInstance(String farmId, String farmName) {
        SensorFragment fragment = new SensorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FARM_ID, farmId);
        args.putString(ARG_FARM_NAME, farmName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            farmId = getArguments().getString(ARG_FARM_ID);
            farmName = getArguments().getString(ARG_FARM_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        titleTextView = view.findViewById(R.id.text_sensor_title);
        titleTextView.setText(farmName + " Sensors");

        // Initialize Firebase reference for this farm's sensors
        sensorRef = FirebaseDatabase.getInstance()
                .getReference("farms")
                .child(farmId)
                .child("sensors");

        // Load sensor data
        loadSensorData();
    }

    private void loadSensorData() {
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // TODO: Update UI with sensor data
                // This will be implemented based on your sensor data structure
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
} 