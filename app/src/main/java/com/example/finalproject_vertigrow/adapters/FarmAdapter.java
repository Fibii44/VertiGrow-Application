package com.example.finalproject_vertigrow.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.models.Farm;

import java.util.ArrayList;
import java.util.List;

public class FarmAdapter extends RecyclerView.Adapter<FarmAdapter.FarmViewHolder> {
    private List<Farm> farms = new ArrayList<>();

    public FarmAdapter() {
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
        holder.bind(farm);
    }

    @Override
    public int getItemCount() {
        return farms.size();
    }

    public static class FarmViewHolder extends RecyclerView.ViewHolder {
        private final TextView plantNameTextView;
        private final TextView plantTypeTextView;

        public FarmViewHolder(@NonNull View itemView) {
            super(itemView);
            plantNameTextView = itemView.findViewById(R.id.text_plant_name);
            plantTypeTextView = itemView.findViewById(R.id.text_plant_type);
        }

        public void bind(Farm farm) {
            plantNameTextView.setText(farm.getPlantName());
            plantTypeTextView.setText(farm.getPlantType());
        }
    }
} 