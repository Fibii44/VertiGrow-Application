package com.example.finalproject_vertigrow.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject_vertigrow.R;

public class AboutUsFragment extends Fragment {

    public AboutUsFragment() {
        // Required empty public constructor
    }

    public static AboutUsFragment newInstance() {
        return new AboutUsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up email contact click listener
        LinearLayout emailLayout = view.findViewById(R.id.layout_email_contact);
        if (emailLayout != null) {
            emailLayout.setOnClickListener(v -> {
                try {
                    String email = ((TextView) view.findViewById(R.id.text_email)).getText().toString();
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:" + email));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry from VertiGrow App");
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Set up phone contact click listener
        LinearLayout phoneLayout = view.findViewById(R.id.layout_phone_contact);
        if (phoneLayout != null) {
            phoneLayout.setOnClickListener(v -> {
                try {
                    String phone = ((TextView) view.findViewById(R.id.text_phone)).getText().toString();
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + phone.replaceAll("\\s+", "")));
                    startActivity(dialIntent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Could not initiate call", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
} 