package com.example.finalproject_vertigrow.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.fragments.SensorFragment;
import com.example.finalproject_vertigrow.fragments.NoSensorsFragment;
import com.example.finalproject_vertigrow.fragments.IrrigationFragment;
import com.example.finalproject_vertigrow.models.Farm;
import com.example.finalproject_vertigrow.models.LogEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {
    private static final String TAG = "FarmAdapter";
    private List<Farm> farms = new ArrayList<>();
    private FragmentActivity activity;
    private CollectionReference farmsRef;
    private DatabaseReference sensorsRef;
    private String currentUserId;
    private OnFarmMoreOptionsListener listener;
    private OnEditClickListener editClickListener;

    // Interface for handling farm options
    public interface OnFarmMoreOptionsListener {
        void onFarmMoreOptions(Farm farm);
    }
    
    // Interface for handling edit button clicks
    public interface OnEditClickListener {
        void onEditClick(Farm farm);
    }

    public FarmAdapter(FragmentActivity activity) {
        this.activity = activity;
        this.farmsRef = FirebaseFirestore.getInstance().collection("farms");
        this.sensorsRef = FirebaseDatabase.getInstance().getReference("sensors");
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setOnFarmMoreOptionsListener(OnFarmMoreOptionsListener listener) {
        this.listener = listener;
    }
    
    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setFarms(List<Farm> farms) {
        this.farms = farms;
        notifyDataSetChanged();
    }

    public void addFarm(Farm farm) {
        farms.add(farm);
        notifyItemInserted(farms.size() - 1);
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
        
        // Set farm data
        holder.farmId.setText("Farm #" + farm.getFarm());
        holder.plantName.setText(farm.getPlantName());
        holder.plantType.setText(farm.getPlantType());
        
        // Update UI based on online status
        if (farm.isOnline()) {
            // Update UI to show online status
            holder.onlineStatus.setImageResource(R.drawable.ic_online);
            holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                android.R.color.holo_green_dark));
            holder.onlineStatusText.setText("Online");
            holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                android.R.color.holo_green_dark));
            Log.d(TAG, "Farm " + farm.getFarm() + " is online (has sensor data)");
        } else {
            // Update UI to show offline status
            holder.onlineStatus.setImageResource(R.drawable.ic_offline);
            holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                android.R.color.holo_red_dark));
            holder.onlineStatusText.setText("Offline");
            holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                android.R.color.holo_red_dark));
            Log.d(TAG, "Farm " + farm.getFarm() + " is offline (no sensor data)");
        }
        
        // Set click listener for more options
        holder.buttonMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFarmMoreOptions(farm);
            } else {
                // Show popup menu if no listener is set
                showPopupMenu(holder.buttonMore, farm);
            }
        });

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            int farmNumber = farm.getFarm();
            checkFarmSensorData(farmNumber);
        });
    }

    private void checkSensorDataAndUpdateStatus(Farm farm, FarmViewHolder holder) {
        Log.d(TAG, "Checking online status for farm: " + farm.getFarm());
        
        // Query Realtime Database for sensor data for this farm
        sensorsRef.orderByChild("farm").equalTo(farm.getFarm())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "Received sensor data snapshot for farm " + farm.getFarm() + 
                          ", exists: " + dataSnapshot.exists() + 
                          ", children count: " + dataSnapshot.getChildrenCount());
                    
                    boolean isOnline = dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0;
                    
                    // Update the farm's online status
                    farm.setOnline(isOnline);
                    
                    // Update UI based on online status
                    if (isOnline) {
                        // Update UI to show online status
                        holder.onlineStatus.setImageResource(R.drawable.ic_online);
                        holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_green_dark));
                        holder.onlineStatusText.setText("Online");
                        holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_green_dark));
                        Log.d(TAG, "Farm " + farm.getFarm() + " is online (has sensor data)");
                    } else {
                        // Update UI to show offline status
                        holder.onlineStatus.setImageResource(R.drawable.ic_offline);
                        holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_red_dark));
                        holder.onlineStatusText.setText("Offline");
                        holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_red_dark));
                        Log.d(TAG, "Farm " + farm.getFarm() + " is offline (no sensor data)");
                    }
                    
                    // Update farm document in Firestore
                    farmsRef.document(farm.getId()).update("isOnline", isOnline)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully updated online status in Firestore for farm " + farm.getFarm());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating farm online status in Firestore: " + e.getMessage());
                        });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Query failed, default to using stored online status
                    Log.e(TAG, "Error checking sensors for farm " + farm.getFarm() + ": " + databaseError.getMessage());
                    
                    // Set online status icon based on stored value
                    if (farm.isOnline()) {
                        holder.onlineStatus.setImageResource(R.drawable.ic_online);
                        holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_green_dark));
                        holder.onlineStatusText.setText("Online");
                        holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_green_dark));
                        Log.d(TAG, "Farm " + farm.getFarm() + " is using stored online status: online");
                    } else {
                        holder.onlineStatus.setImageResource(R.drawable.ic_offline);
                        holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_red_dark));
                        holder.onlineStatusText.setText("Offline");
                        holder.onlineStatusText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                            android.R.color.holo_red_dark));
                        Log.d(TAG, "Farm " + farm.getFarm() + " is using stored online status: offline");
                    }
                }
            });
    }

    private void checkFarmSensorData(int farmNumber) {
        // Show progress if needed
        // activity.showProgress(); // Uncomment if you have a progress indicator
        
        // Query Realtime Database for sensor data for this farm
        sensorsRef.orderByChild("farm").equalTo(farmNumber)
            .limitToFirst(1)  // We only need to know if at least one sensor document exists
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // activity.hideProgress(); // Uncomment if you have a progress indicator
                    
                    if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        // Sensor data exists for this farm
                        Log.d(TAG, "Navigating to SensorFragment for farm " + farmNumber);
                        
                        // Create and navigate to the sensor fragment
                        SensorFragment sensorFragment = SensorFragment.newInstance(farmNumber);
                        activity.getSupportFragmentManager()
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
                        activity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, noSensorsFragment)
                            .addToBackStack(null)
                            .commit();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // activity.hideProgress(); // Uncomment if you have a progress indicator
                    Log.e(TAG, "Error checking sensor data: " + databaseError.getMessage());
                    Toast.makeText(activity, "Error checking sensors: " + databaseError.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showEditDialog(Farm farm) {
        if (editClickListener != null) {
            editClickListener.onEditClick(farm);
        } else {
            // Fallback if no listener is set
            Toast.makeText(activity, "Edit farm: " + farm.getPlantName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFarm(Farm farm) {
        // Delete from Firestore using document ID
        farmsRef.document(farm.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity, "Farm deleted successfully", Toast.LENGTH_SHORT).show();
                    // Remove from local list
                    farms.remove(farm);
                    notifyDataSetChanged();
                    
                    // Create a detailed log message for the farm deletion
                    String description = "Deleted Farm #" + farm.getFarm() + ": '" + 
                            farm.getPlantName() + "' (Type: " + farm.getPlantType() + ")";
                    
                    // Log the farm deletion
                    logFarmAction("FARM_DELETE", description);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity, "Failed to delete farm: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showIrrigationData(int farmNumber) {
        // Create and navigate to the irrigation fragment without passing farm number
        // since it will show data for all farms belonging to the current user
        IrrigationFragment irrigationFragment = IrrigationFragment.newInstance();
        activity.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, irrigationFragment)
            .addToBackStack(null)
            .commit();
    }

    private void showPopupMenu(View view, Farm farm) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.inflate(R.menu.menu_farm_item);
        
        // Set up click handler for menu items
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.action_edit) {
                showEditDialog(farm);
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
        private TextView farmId;
        private TextView plantName;
        private TextView plantType;
        private TextView onlineStatusText;
        private ImageView onlineStatus;
        private ImageView plantIcon;
        private ImageButton buttonMore;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            farmId = itemView.findViewById(R.id.text_farm_id);
            plantName = itemView.findViewById(R.id.text_plant_name);
            plantType = itemView.findViewById(R.id.text_plant_type);
            onlineStatusText = itemView.findViewById(R.id.text_online_status);
            onlineStatus = itemView.findViewById(R.id.image_online_status);
            plantIcon = itemView.findViewById(R.id.image_plant_icon);
            buttonMore = itemView.findViewById(R.id.button_more);
        }
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
                    Log.d("FarmAdapter", "Log entry added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("FarmAdapter", "Error adding log entry: " + e.getMessage());
                });
    }
} 