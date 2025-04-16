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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {
    private List<Farm> farms = new ArrayList<>();
    private FragmentActivity activity;
    private CollectionReference farmsRef;
    private String currentUserId;
    private OnFarmMoreOptionsListener listener;

    // Interface for handling farm options
    public interface OnFarmMoreOptionsListener {
        void onFarmMoreOptions(Farm farm);
    }

    public FarmAdapter(FragmentActivity activity) {
        this.activity = activity;
        this.farmsRef = FirebaseFirestore.getInstance().collection("farms");
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void setOnFarmMoreOptionsListener(OnFarmMoreOptionsListener listener) {
        this.listener = listener;
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
        
        // Set farm ID
        holder.farmId.setText("Farm #" + farm.getFarm());
        
        // Set plant name and type
        holder.plantName.setText(farm.getPlantName());
        holder.plantType.setText(farm.getPlantType());
        
        // Check for sensor data existence and update farm online status
        checkSensorDataAndUpdateStatus(farm, holder);
        
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
        // Query Firestore for sensor data for this farm
        FirebaseFirestore.getInstance()
            .collection("sensors")
            .whereEqualTo("farm", farm.getFarm())
            .limit(1)  // We only need to know if at least one sensor document exists
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    // Sensor data exists, update online status to true
                    farm.setOnline(true);
                    
                    // Update UI to show online status
                    holder.onlineStatus.setImageResource(R.drawable.ic_online);
                    holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                        android.R.color.holo_green_dark));
                    Log.d("FarmAdapter", "Farm " + farm.getFarm() + " is online (has sensor data)");
                    
                    // Update farm document in Firestore
                    farmsRef.document(farm.getId()).update("online", true);
                } else {
                    // No sensor data exists
                    farm.setOnline(false);
                    
                    // Update UI to show offline status
                    holder.onlineStatus.setImageResource(R.drawable.ic_offline);
                    holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                        android.R.color.holo_red_dark));
                    Log.d("FarmAdapter", "Farm " + farm.getFarm() + " is offline (no sensor data)");
                    
                    // Update farm document in Firestore
                    farmsRef.document(farm.getId()).update("online", false);
                }
            })
            .addOnFailureListener(e -> {
                // Query failed, default to using stored online status
                Log.e("FarmAdapter", "Error checking sensors for farm " + farm.getFarm() + ": " + e.getMessage());
                
                // Set online status icon based on stored value
                if (farm.isOnline()) {
                    holder.onlineStatus.setImageResource(R.drawable.ic_online);
                    holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                        android.R.color.holo_green_dark));
                    Log.d("FarmAdapter", "Farm " + farm.getFarm() + " is using stored online status: online");
                } else {
                    holder.onlineStatus.setImageResource(R.drawable.ic_offline);
                    holder.onlineStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), 
                        android.R.color.holo_red_dark));
                    Log.d("FarmAdapter", "Farm " + farm.getFarm() + " is using stored online status: offline");
                }
            });
    }

    private void checkFarmSensorData(int farmNumber) {
        // Query Firestore for sensor data for this farm
        FirebaseFirestore.getInstance()
            .collection("sensors")
            .whereEqualTo("farm", farmNumber)
            .limit(1)  // We only need to know if at least one sensor document exists
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    // Sensor data exists for this farm
                    // Create and navigate to the sensor fragment
                    SensorFragment sensorFragment = SensorFragment.newInstance(farmNumber);
                    activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, sensorFragment)
                        .addToBackStack(null)
                        .commit();
                } else {
                    // No sensor data exists for this farm
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
            })
            .addOnFailureListener(e -> {
                // Query failed
                Toast.makeText(activity, "Error checking sensors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showEditDialog(Farm farm) {
        // TODO: Implement edit dialog
        Toast.makeText(activity, "Edit farm: " + farm.getPlantName(), Toast.LENGTH_SHORT).show();
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
        PopupMenu popup = new PopupMenu(activity, view);
        popup.inflate(R.menu.menu_farm_item);
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                showEditDialog(farm);
                return true;
            } else if (id == R.id.action_delete) {
                deleteFarm(farm);
                return true;
            } else if (id == R.id.action_irrigation) {
                showIrrigationData(farm.getFarm());
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
        private ImageView onlineStatus;
        private ImageButton buttonMore;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            farmId = itemView.findViewById(R.id.text_farm_id);
            plantName = itemView.findViewById(R.id.text_plant_name);
            plantType = itemView.findViewById(R.id.text_plant_type);
            onlineStatus = itemView.findViewById(R.id.image_online_status);
            buttonMore = itemView.findViewById(R.id.button_more);
        }
    }
} 