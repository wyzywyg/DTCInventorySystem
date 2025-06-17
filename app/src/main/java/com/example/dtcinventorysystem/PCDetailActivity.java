package com.example.dtcinventorysystem;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PCDetailActivity extends AppCompatActivity {
    CheckBox virusCheck, updateSoftware, uninstallPrograms;
    Button submitBtn;
    TextView pcNameText, statusText, lastUpdatedText;
    DatabaseReference pcRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcdetail);

        virusCheck = findViewById(R.id.checkbox_virus);
        updateSoftware = findViewById(R.id.checkbox_update);
        uninstallPrograms = findViewById(R.id.checkbox_uninstall);
        submitBtn = findViewById(R.id.submit_btn);
        pcNameText = findViewById(R.id.pc_name);
        statusText = findViewById(R.id.status_text);
        lastUpdatedText = findViewById(R.id.last_updated_text);

        String pcId = getIntent().getStringExtra("pc_id");
        String role = getIntent().getStringExtra("role");
        pcRef = FirebaseDatabase.getInstance().getReference("pcs").child(pcId);

        pcNameText.setText("PC ID: " + pcId);

        // Load existing data
        pcRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Boolean virus = snapshot.child("virusCheck").getValue(Boolean.class);
                Boolean update = snapshot.child("updateSoftware").getValue(Boolean.class);
                Boolean uninstall = snapshot.child("uninstallPrograms").getValue(Boolean.class);
                String status = snapshot.child("status").getValue(String.class);
                Long lastUpdated = snapshot.child("lastUpdated").getValue(Long.class);

                virusCheck.setChecked(Boolean.TRUE.equals(virus));
                updateSoftware.setChecked(Boolean.TRUE.equals(update));
                uninstallPrograms.setChecked(Boolean.TRUE.equals(uninstall));

                if (status != null) {
                    statusText.setText("Status: " + status);
                } else {
                    statusText.setText("Status: Unknown");
                }

                if (lastUpdated != null) {
                    String formattedTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(lastUpdated));
                    lastUpdatedText.setText("Last Updated: " + formattedTime);
                } else {
                    lastUpdatedText.setText("Last Updated: Not available");
                }
            }
        });

        // Disable for admin
        if ("admin".equalsIgnoreCase(role)) {
            virusCheck.setEnabled(false);
            updateSoftware.setEnabled(false);
            uninstallPrograms.setEnabled(false);
            submitBtn.setEnabled(false);
        }

        submitBtn.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            boolean virus = virusCheck.isChecked();
            boolean update = updateSoftware.isChecked();
            boolean uninstall = uninstallPrograms.isChecked();

            updates.put("virusCheck", virus);
            updates.put("updateSoftware", update);
            updates.put("uninstallPrograms", uninstall);

            // Compute status based on checks
            String newStatus;
            if (virus && update && uninstall) {
                newStatus = "Done";
            } else {
                newStatus = "Under Maintenance";
            }

            updates.put("status", newStatus);
            updates.put("lastUpdated", System.currentTimeMillis());

            pcRef.updateChildren(updates).addOnSuccessListener(task -> {
                Toast.makeText(this, "Maintenance status updated.", Toast.LENGTH_SHORT).show();
                statusText.setText("Status: " + newStatus);
                String formattedTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
                lastUpdatedText.setText("Last Updated: " + formattedTime);
            });
        });
    }
}
