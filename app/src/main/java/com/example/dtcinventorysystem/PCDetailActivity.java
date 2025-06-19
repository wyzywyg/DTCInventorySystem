package com.example.dtcinventorysystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class PCDetailActivity extends AppCompatActivity {

    // UI Components
    private TextView pcIdText;
    private EditText pcNameInput, pcSpecsInput, dateAcquiredInput, endUserInput, amountInput;
    private TextView lastMaintainedDisplay;
    private CheckBox checkboxVirus, checkboxUninstall, checkboxUpdate;
    private ImageView virusCheckIcon, uninstallIcon, updateIcon;
    private TextView performedByText, maintenanceDateText;
    private TextView statusText, lastUpdatedText, maintainerText, diagnosticsText;
    private Button updateBtn;

    // Data variables
    private String currentUser = "", userRole = "", pcId = "", mode = "";
    private boolean isEditMode = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcdetail);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupUserData();
        loadDataFromIntent();
        setupDatePickers();
        setupCheckboxListeners();
        setupButtons();
        updateUIBasedOnMode();
        updateMaintenanceStatus();
    }

    private void initializeViews() {
        pcIdText = findViewById(R.id.pc_id_text);
        pcNameInput = findViewById(R.id.pc_name_input);
        pcSpecsInput = findViewById(R.id.pc_specs_input);
        dateAcquiredInput = findViewById(R.id.date_acquired_input);
        endUserInput = findViewById(R.id.end_user_input);
        amountInput = findViewById(R.id.amount_input);
        lastMaintainedDisplay = findViewById(R.id.last_maintained_display);
        checkboxVirus = findViewById(R.id.checkbox_virus);
        checkboxUninstall = findViewById(R.id.checkbox_uninstall);
        checkboxUpdate = findViewById(R.id.checkbox_update);
        virusCheckIcon = findViewById(R.id.virus_check_icon);
        uninstallIcon = findViewById(R.id.uninstall_icon);
        updateIcon = findViewById(R.id.update_icon);
        performedByText = findViewById(R.id.performed_by_text);
        maintenanceDateText = findViewById(R.id.maintenance_date_text);
        statusText = findViewById(R.id.status_text);
        lastUpdatedText = findViewById(R.id.last_updated_text);
        maintainerText = findViewById(R.id.maintainer_text);
        diagnosticsText = findViewById(R.id.diagnostics_text);
        updateBtn = findViewById(R.id.update_btn);
    }

    private void setupUserData() {
        currentUser = getIntent().getStringExtra("username");
        userRole = getIntent().getStringExtra("user_role");
        pcId = getIntent().getStringExtra("pc_id");
        mode = getIntent().getStringExtra("mode");

        if (currentUser == null) currentUser = "Unknown User";
        if (userRole == null) userRole = "maintainer";
        if (pcId == null) pcId = "UNKNOWN-PC";
        if (mode == null) mode = "view_existing";
    }

    private void loadDataFromIntent() {
        pcIdText.setText(pcId);
        pcNameInput.setText(getIntent().getStringExtra("property_number"));
        pcSpecsInput.setText(getIntent().getStringExtra("specifications"));
        dateAcquiredInput.setText(getIntent().getStringExtra("date_acquired"));
        endUserInput.setText(getIntent().getStringExtra("end_user"));
        amountInput.setText(getIntent().getStringExtra("amount"));
        loadMaintenanceStatus();
    }

    private void loadMaintenanceStatus() {
        db.collection("maintenance").document(pcId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            checkboxVirus.setChecked(Boolean.TRUE.equals(document.getBoolean("virus_check")));
                            checkboxUninstall.setChecked(Boolean.TRUE.equals(document.getBoolean("uninstall_programs")));
                            checkboxUpdate.setChecked(Boolean.TRUE.equals(document.getBoolean("update_software")));
                            String lastMaintainer = document.getString("last_maintainer");
                            String lastMaintenanceDate = document.getString("last_maintenance_date");

                            if (lastMaintenanceDate != null)
                                lastMaintainedDisplay.setText(lastMaintenanceDate);
                            if (lastMaintainer != null)
                                maintainerText.setText("Last maintained by: " + lastMaintainer);
                            if (lastMaintenanceDate != null)
                                lastUpdatedText.setText("Last Updated: " + lastMaintenanceDate);
                        } else {
                            checkboxVirus.setChecked(false);
                            checkboxUninstall.setChecked(false);
                            checkboxUpdate.setChecked(false);
                            lastMaintainedDisplay.setText("Not maintained yet");
                        }
                        updateMaintenanceStatus();
                    }
                });
    }

    private void updateUIBasedOnMode() {
        updateBtn.setText("Edit");
        setCheckboxesEnabled(false);
    }

    private void setCheckboxesEnabled(boolean enabled) {
        checkboxVirus.setEnabled(enabled);
        checkboxUninstall.setEnabled(enabled);
        checkboxUpdate.setEnabled(enabled);
    }

    private void setupDatePickers() {
        dateAcquiredInput.setOnClickListener(v -> {
            if (isEditMode) showDatePicker(dateAcquiredInput);
        });
    }

    private void showDatePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            editText.setText(dateFormat.format(selected.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupCheckboxListeners() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            if (isEditMode) {
                updateMaintenanceInfo();
                updateMaintenanceStatus();
            }
        };

        checkboxVirus.setOnCheckedChangeListener(listener);
        checkboxUninstall.setOnCheckedChangeListener(listener);
        checkboxUpdate.setOnCheckedChangeListener(listener);
    }

    private void updateTaskIcon(ImageView icon, boolean isCompleted) {
        if (isCompleted) {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            icon.setImageResource(R.drawable.ic_close_circle);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    private void updateMaintenanceInfo() {
        String currentDate = dateFormat.format(new Date());
        performedByText.setText("Performed by: " + currentUser);
        maintenanceDateText.setText("Date: " + currentDate);
        lastMaintainedDisplay.setText(currentDate);
        lastUpdatedText.setText("Last Updated: " + currentDate);
        maintainerText.setText("Last maintained by: " + currentUser);
    }

    private void updateMaintenanceStatus() {
        boolean complete = checkboxVirus.isChecked() && checkboxUninstall.isChecked() && checkboxUpdate.isChecked();

        if (complete) {
            statusText.setText("Status: Fully Maintained");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            diagnosticsText.setText("Diagnostics: All systems operational - All maintenance tasks completed");
        } else {
            statusText.setText("Status: Partial Maintenance");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            diagnosticsText.setText("Diagnostics: Some maintenance tasks pending");
        }

        updateTaskIcon(virusCheckIcon, checkboxVirus.isChecked());
        updateTaskIcon(uninstallIcon, checkboxUninstall.isChecked());
        updateTaskIcon(updateIcon, checkboxUpdate.isChecked());
    }

    private void setupButtons() {
        updateBtn.setText("Edit");
        isEditMode = false;
        setCheckboxesEnabled(false);

        updateBtn.setOnClickListener(v -> {
            if (!isEditMode) {
                isEditMode = true;
                updateBtn.setText("Save");
                setCheckboxesEnabled(true);
                Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
            } else {
                saveMaintenanceRecord();
                isEditMode = false;
                updateBtn.setText("Edit");
                setCheckboxesEnabled(false);
                Toast.makeText(this, "Maintenance updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMaintenanceRecord() {
        Map<String, Object> data = new HashMap<>();
        data.put("pc_id", pcId);
        data.put("virus_check", checkboxVirus.isChecked());
        data.put("uninstall_programs", checkboxUninstall.isChecked());
        data.put("update_software", checkboxUpdate.isChecked());
        data.put("last_maintainer", currentUser);
        data.put("last_maintenance_date", dateFormat.format(new Date()));
        data.put("updated_timestamp", System.currentTimeMillis());

        db.collection("maintenance").document(pcId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Maintenance record saved successfully", Toast.LENGTH_SHORT).show();
                    updateMaintenanceStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
