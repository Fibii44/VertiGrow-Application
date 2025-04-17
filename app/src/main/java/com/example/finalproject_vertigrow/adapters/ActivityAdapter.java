package com.example.finalproject_vertigrow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.LogEntry;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {
    private List<LogEntry> activities = new ArrayList<>();
    private Map<String, String> userCache = new HashMap<>();
    private FirebaseFirestore db;
    
    public ActivityAdapter() {
        db = FirebaseFirestore.getInstance();
    }
    
    public void setActivities(List<LogEntry> activities) {
        this.activities = activities;
        notifyDataSetChanged();
        
        // Preload user names
        for (LogEntry log : activities) {
            if (log.getUserId() != null && !log.getUserId().isEmpty() && !userCache.containsKey(log.getUserId())) {
                fetchUserName(log.getUserId());
            }
        }
    }
    
    private void fetchUserName(String userId) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("name")) {
                    String userName = documentSnapshot.getString("name");
                    String email = documentSnapshot.getString("email");
                    
                    // Cache the user name with email
                    if (userName != null && email != null) {
                        userCache.put(userId, userName + " (" + email + ")");
                        notifyDataSetChanged();
                    } else if (email != null) {
                        userCache.put(userId, email);
                        notifyDataSetChanged();
                    } else if (userName != null) {
                        userCache.put(userId, userName);
                        notifyDataSetChanged();
                    }
                }
            });
    }
    
    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new ActivityViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        LogEntry activity = activities.get(position);
        
        // Set action type
        holder.textAction.setText(formatActionType(activity.getType()));
        
        // Set description
        holder.textDescription.setText(activity.getDescription());
        
        // Set timestamp
        holder.textTimestamp.setText(formatTimestamp(activity.getTimestamp()));
        
        // Set user name (if available)
        if (activity.getUserId() != null && !activity.getUserId().isEmpty()) {
            String displayText = userCache.getOrDefault(activity.getUserId(), "User ID: " + activity.getUserId());
            holder.textUser.setText(displayText);
            holder.textUser.setVisibility(View.VISIBLE);
            holder.iconUser.setVisibility(View.VISIBLE);
        } else {
            holder.textUser.setVisibility(View.GONE);
            holder.iconUser.setVisibility(View.GONE);
        }
        
        // Set color and icon for action type
        int color = getColorForActionType(activity.getType());
        holder.actionIndicator.setBackgroundColor(color);
        holder.textAction.setTextColor(color);
    }
    
    private String formatActionType(String type) {
        if (type == null || type.isEmpty()) {
            return "ACTION";
        }
        
        // Split by underscore and capitalize each word
        String[] parts = type.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            if (part.length() > 0) {
                formatted.append(part.charAt(0))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    private int getColorForActionType(String type) {
        if (type == null) {
            return 0xFF2196F3; // Default blue
        }
        
        switch (type.toUpperCase()) {
            // User management actions
            case "USER_CREATE":
            case "USER_ADD":
                return 0xFF4CAF50; // Green
            case "USER_DELETE":
                return 0xFFF44336; // Red
            case "ROLE_CHANGE":
                return 0xFF9C27B0; // Purple
            
            // Farm actions
            case "FARM_ADD":
                return 0xFF8BC34A; // Light Green  
            case "FARM_UPDATE":
            case "FARM_EDIT":
                return 0xFFFFEB3B; // Yellow
            case "FARM_DELETE":
                return 0xFFFF9800; // Orange
                
            // Plant actions
            case "PLANT_ADD":
                return 0xFF009688; // Teal
            case "PLANT_UPDATE":
            case "PLANT_EDIT":
                return 0xFF00BCD4; // Cyan  
            case "PLANT_DELETE":
                return 0xFFE91E63; // Pink
                
            // Sensor actions
            case "SENSOR_UPDATE":
            case "SENSOR_DATA":
                return 0xFF3F51B5; // Indigo
                
            // System actions
            case "SYSTEM":
            case "LOGIN":
            case "LOGOUT":
                return 0xFF607D8B; // Blue Gray
                
            default:
                return 0xFF2196F3; // Blue
        }
    }
    
    @Override
    public int getItemCount() {
        return activities.size();
    }
    
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        View actionIndicator;
        TextView textAction, textDescription, textTimestamp, textUser;
        ImageView iconUser;
        
        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            actionIndicator = itemView.findViewById(R.id.action_indicator);
            textAction = itemView.findViewById(R.id.text_log_action);
            textDescription = itemView.findViewById(R.id.text_log_description);
            textTimestamp = itemView.findViewById(R.id.text_log_timestamp);
            textUser = itemView.findViewById(R.id.text_log_user);
            iconUser = itemView.findViewById(R.id.icon_user);
        }
    }
} 