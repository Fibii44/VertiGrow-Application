package com.example.finalproject_vertigrow.adapters;

import android.content.Context;
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
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.fragments.SensorFragment;
import com.example.finalproject_vertigrow.fragments.NoSensorsFragment;
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

    public FarmAdapter(FragmentActivity activity) {
        this.activity = activity;
        this.farmsRef = FirebaseFirestore.getInstance().collection("farms");
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        holder.bind(farm, position + 1);
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            // Get the farm number directly from the Farm object
            int farmNumber = farm.getFarm();
            
            // Check if this farm has sensor data
            checkFarmSensorData(farmNumber);
        });

        // Set up the more options menu
        holder.buttonMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, holder.buttonMore);
            popup.inflate(R.menu.menu_farm_item);
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

    @Override
    public int getItemCount() {
        return farms.size();
    }

    class FarmViewHolder extends RecyclerView.ViewHolder {
        private TextView farmIdTextView;
        private TextView plantNameTextView;
        private TextView plantTypeTextView;
        private ImageView onlineStatusIcon;
        private ImageButton buttonMore;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            farmIdTextView = itemView.findViewById(R.id.text_farm_id);
            plantNameTextView = itemView.findViewById(R.id.text_plant_name);
            plantTypeTextView = itemView.findViewById(R.id.text_plant_type);
            onlineStatusIcon = itemView.findViewById(R.id.image_online_status);
            buttonMore = itemView.findViewById(R.id.button_more);
        }

        public void bind(Farm farm, int position) {
            // Display the farm number from the database
            farmIdTextView.setText("Farm #" + farm.getFarm());
            plantNameTextView.setText(farm.getPlantName());
            plantTypeTextView.setText(farm.getPlantType());
            
            // Set online/offline status icon and color
            if (farm.isOnline()) {
                onlineStatusIcon.setImageResource(R.drawable.ic_online);
                onlineStatusIcon.setColorFilter(activity.getResources().getColor(android.R.color.holo_green_dark));
                onlineStatusIcon.setContentDescription("Online");
            } else {
                onlineStatusIcon.setImageResource(R.drawable.ic_offline);
                onlineStatusIcon.setColorFilter(activity.getResources().getColor(android.R.color.holo_red_dark));
                onlineStatusIcon.setContentDescription("Offline");
            }
        }
    }
} 