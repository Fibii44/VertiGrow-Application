package com.example.finalproject_vertigrow.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.finalproject_vertigrow.R;
import com.example.finalproject_vertigrow.fragments.DashboardFragment;
import com.example.finalproject_vertigrow.fragments.FarmsFragment;
import com.example.finalproject_vertigrow.fragments.IrrigationFragment;
import com.example.finalproject_vertigrow.fragments.MenuFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class UserActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        // Setup bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
            bottomNavigationView.setSelectedItemId(R.id.navigation_dashboard);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.navigation_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.navigation_farms) {
            fragment = new FarmsFragment();
        } else if (itemId == R.id.navigation_irrigation) {
            fragment = new IrrigationFragment();
        } else if (itemId == R.id.navigation_menu) {
            fragment = new MenuFragment();
        }

        return loadFragment(fragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commit();
            return true;
        }
        return false;
    }
}