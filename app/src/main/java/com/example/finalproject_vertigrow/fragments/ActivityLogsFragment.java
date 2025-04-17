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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.adapters.ActivityAdapter;
import com.example.finalproject_vertigrow.models.LogEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ActivityLogsFragment extends Fragment {
    private static final String TAG = "ActivityLogsFragment";

    private RecyclerView recyclerView;
    private ActivityAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<LogEntry> allLogs = new ArrayList<>();

    public ActivityLogsFragment() {
        // Required empty public constructor
    }

    public static ActivityLogsFragment newInstance() {
        return new ActivityLogsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_activity_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_logs);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.text_empty);

        // Setup RecyclerView with dividers
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        
        // Initialize adapter
        adapter = new ActivityAdapter();
        recyclerView.setAdapter(adapter);

        // Load activity logs
        loadActivityLogs();
    }
    

    private void loadActivityLogs() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        db.collection("logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(200) // Limit to most recent 200 entries
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    
                    allLogs.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            LogEntry logEntry = document.toObject(LogEntry.class);
                            logEntry.setId(document.getId());
                            allLogs.add(logEntry);
                            Log.d(TAG, "Loaded log: " + logEntry.getDescription() + ", type: " + logEntry.getType() + ", user: " + logEntry.getUserId());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing log entry: " + document.getId(), e);
                        }
                    }

                    if (allLogs.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setActivities(allLogs);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    
                    Log.e(TAG, "Error loading activity logs", e);
                    Toast.makeText(requireContext(), "Error loading activity logs: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
} 