package com.sernlabs.vertigrow.ui.menu;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class ConfirmLogoutDialogFragment extends DialogFragment {

    public interface LogoutListener {
        void onLogoutConfirmed();
    }

    private LogoutListener logoutListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        
        // Verify that the host fragment implements the callback interface
        try {
            // First try with parent fragment
            Fragment parentFragment = getParentFragment();
            if (parentFragment instanceof LogoutListener) {
                logoutListener = (LogoutListener) parentFragment;
            } else {
                // If not found, try with activity
                if (context instanceof LogoutListener) {
                    logoutListener = (LogoutListener) context;
                } else {
                    throw new ClassCastException("Host must implement LogoutListener");
                }
            }
        } catch (ClassCastException e) {
            throw new ClassCastException("Host must implement LogoutListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    if (logoutListener != null) {
                        logoutListener.onLogoutConfirmed();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dismiss())
                .create();
    }
} 