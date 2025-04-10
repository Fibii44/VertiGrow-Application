package com.example.finalproject_vertigrow;

import android.os.Bundle;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://vertigrow-ae776-default-rtdb.asia-southeast1.firebasedatabase.app/");
    DatabaseReference databaseReference = database.getReference("vertigrow/data");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a reference to your Firebase Realtime Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("vertigrow/data");

        // Example: Write data to Firebase
        databaseReference.setValue("Hello from VertiGrow!", (error, ref) -> {
            if (error != null) {
                System.out.println("❌ Write failed: " + error.getMessage());
            } else {
                System.out.println("✅ Write successful!");
            }
        });


        // You can also write objects like:
        // MySensorData data = new MySensorData(25.5, 70);
        // databaseReference.child("sensor1").setValue(data);
    }
}