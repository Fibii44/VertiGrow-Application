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

public class NoSensorsFragment extends Fragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";

    private String title;
    private String description;

    public NoSensorsFragment() {
        // Required empty constructor
    }

    public static NoSensorsFragment newInstance(String title, String description) {
        NoSensorsFragment fragment = new NoSensorsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE, "No sensor data available");
            description = getArguments().getString(ARG_DESCRIPTION, 
                "There are no sensors connected to this farm, or the sensors are not sending data currently.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_no_sensors, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView titleTextView = view.findViewById(R.id.text_no_data);
        TextView descriptionTextView = view.findViewById(R.id.text_no_data_description);
        
        if (titleTextView != null && title != null) {
            titleTextView.setText(title);
        }
        
        if (descriptionTextView != null && description != null) {
            descriptionTextView.setText(description);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove this view from its parent to prevent it from staying in the background
        View view = getView();
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null) {
                parent.removeView(view);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Additional cleanup to ensure fragment is fully destroyed
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        // Additional cleanup when fragment is detached
        if (getView() != null) {
            ((ViewGroup) getView().getParent()).removeView(getView());
        }
    }
} 