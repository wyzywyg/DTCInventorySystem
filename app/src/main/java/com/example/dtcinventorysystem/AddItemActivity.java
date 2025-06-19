package com.example.dtcinventorysystem;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddItemActivity extends AppCompatActivity {

    // UI Components
    private TextView pcIdText;
    private EditText pcNameInput, pcSpecsInput, dateAcquiredInput, endUserInput, amountInput;
    private TextView lastMaintainedDisplay;
    private CheckBox checkboxVirus, checkboxUninstall, checkboxUpdate;
    private ImageView virusCheckIcon, uninstallIcon, updateIcon;
    private TextView performedByText, maintenanceDateText;
    private TextView statusText, lastUpdatedText, maintainerText, diagnosticsText;
    private Button updateBtn, addNewItemBtn, enableEditBtn;

    // Data variables
    private String currentUser = "";
    private String userRole = "";
    private String pcId = "";
    private String mode = ""; // "view_existing" or "add_new"
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private FirebaseFirestore db;
    private boolean isEditMode = false;

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
        lastMaintainedDisplay = findViewById(R.id.last_maintained_display);

        // Add new input fields
        endUserInput = findViewById(R.id.end_user_input);
        amountInput = findViewById(R.id.amount_input);

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
        addNewItemBtn = findViewById(R.id.add_new_item_btn);
        enableEditBtn = findViewById(R.id.save_btn);
    }

    private void setupUserData() {
        currentUser = getIntent().getStringExtra("username");
        userRole = getIntent().getStringExtra("user_role");
        pcId = getIntent().getStringExtra("pc_id");
        mode = getIntent().getStringExtra("mode");

        // Default values
        if (currentUser == null) currentUser = "Unknown User";
        if (userRole == null) userRole = "maintainer";
        if (pcId == null) pcId = "UNKNOWN-PC";
        if (mode == null) mode = "view_existing";
    }

    private void loadDataFromIntent() {
        // Set PC ID
        pcIdText.setText(pcId);

        if ("add_new".equals(mode)) {
            // Clear all fields for new item
            pcNameInput.setText("");
            pcSpecsInput.setText("");
            dateAcquiredInput.setText("");
            endUserInput.setText("");
            amountInput.setText("");
            lastMaintainedDisplay.setText("Not maintained yet");

            // Reset checkboxes
            checkboxVirus.setChecked(false);
            checkboxUninstall.setChecked(false);
            checkboxUpdate.setChecked(false);

            isEditMode = true; // Enable editing for new items
        } else {
            // Load data from intent (from Firebase)
            String article = getIntent().getStringExtra("article");
            String specs = getIntent().getStringExtra("specifications");
            String dateAcquired = getIntent().getStringExtra("date_acquired");
            String endUser = getIntent().getStringExtra("end_user");
            String amount = getIntent().getStringExtra("amount");
            String propertyNumber = getIntent().getStringExtra("property_number");

            // Populate fields
            pcNameInput.setText(propertyNumber != null ? propertyNumber : "");
            pcSpecsInput.setText(specs != null ? specs : "");
            dateAcquiredInput.setText(dateAcquired != null ? dateAcquired : "");
            endUserInput.setText(endUser != null ? endUser : "");
            amountInput.setText(amount != null ? amount : "");

            // Load maintenance status from Firestore or set defaults
            loadMaintenanceStatus();

            isEditMode = false; // Disable editing initially
        }
    }

    private void loadMaintenanceStatus() {
        // Check if maintenance record exists in Firestore
        db.collection("maintenance").document(pcId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Load existing maintenance data
                            checkboxVirus.setChecked(document.getBoolean("virus_check") != null ?
                                    document.getBoolean("virus_check") : false);
                            checkboxUninstall.setChecked(document.getBoolean("uninstall_programs") != null ?
                                    document.getBoolean("uninstall_programs") : false);
                            checkboxUpdate.setChecked(document.getBoolean("update_software") != null ?
                                    document.getBoolean("update_software") : false);

                            String lastMaintainer = document.getString("last_maintainer");
                            String lastMaintenanceDate = document.getString("last_maintenance_date");

                            if (lastMaintenanceDate != null) {
                                lastMaintainedDisplay.setText(lastMaintenanceDate);
                                lastUpdatedText.setText("Last Updated: " + lastMaintenanceDate);
                            }
                            if (lastMaintainer != null) {
                                maintainerText.setText("Last maintained by: " + lastMaintainer);
                            }
                        } else {
                            // No maintenance record exists
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
        if ("add_new".equals(mode)) {
            // Add new item mode
            updateBtn.setText("Save Item");
            addNewItemBtn.setVisibility(View.GONE);
            enableEditBtn.setVisibility(View.GONE);

            // Enable all fields
            setFieldsEnabled(true);
            setCheckboxesEnabled(true);

        } else {
            // View existing mode
            updateBtn.setText("Update Maintenance");
            addNewItemBtn.setVisibility(userRole.equals("admin") ? View.VISIBLE : View.GONE);
            enableEditBtn.setVisibility(View.VISIBLE);

            // Initially disable editing
            setFieldsEnabled(false);
            setCheckboxesEnabled(false);
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        pcNameInput.setEnabled(enabled);
        pcSpecsInput.setEnabled(enabled);
        dateAcquiredInput.setEnabled(enabled);
        endUserInput.setEnabled(enabled);
        amountInput.setEnabled(enabled);
    }

    private void setCheckboxesEnabled(boolean enabled) {
        checkboxVirus.setEnabled(enabled);
        checkboxUninstall.setEnabled(enabled);
        checkboxUpdate.setEnabled(enabled);
    }

    private void setupDatePickers() {
        dateAcquiredInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode) {
                    showDatePicker(dateAcquiredInput);
                }
            }
        });
    }

    private void showDatePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        editText.setText(dateFormat.format(selectedDate.getTime()));
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void setupCheckboxListeners() {
        checkboxVirus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTaskIcon(virusCheckIcon, isChecked);
                if (isChecked && isEditMode) {
                    updateMaintenanceInfo("virus_check");
                }
            }
        });

        checkboxUninstall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTaskIcon(uninstallIcon, isChecked);
                if (isChecked && isEditMode) {
                    updateMaintenanceInfo("uninstall_programs");
                }
            }
        });

        checkboxUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTaskIcon(updateIcon, isChecked);
                if (isChecked && isEditMode) {
                    updateMaintenanceInfo("update_software");
                }
            }
        });
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

    private void updateMaintenanceInfo(String taskType) {
        String currentDate = dateFormat.format(new Date());

        performedByText.setText("Performed by: " + currentUser);
        maintenanceDateText.setText("Date: " + currentDate);
        lastMaintainedDisplay.setText(currentDate);
        lastUpdatedText.setText("Last Updated: " + currentDate);
        maintainerText.setText("Last maintained by: " + currentUser);

        updateMaintenanceStatus();
        Toast.makeText(this, "Task updated by " + currentUser, Toast.LENGTH_SHORT).show();
    }

    private void updateMaintenanceStatus() {
        boolean allTasksComplete = checkboxVirus.isChecked() &&
                checkboxUninstall.isChecked() &&
                checkboxUpdate.isChecked();

        if (allTasksComplete) {
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
        enableEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isEditMode = !isEditMode;
                if (isEditMode) {
                    enableEditBtn.setText("Cancel Edit");
                    setCheckboxesEnabled(true);
                    Toast.makeText(AddItemActivity.this, "Maintenance editing enabled", Toast.LENGTH_SHORT).show();
                } else {
                    enableEditBtn.setText("Enable Edit");
                    setCheckboxesEnabled(false);
                    Toast.makeText(AddItemActivity.this, "Maintenance editing disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("add_new".equals(mode)) {
                    saveNewItem();
                } else {
                    if (isEditMode) {
                        updateMaintenanceRecord();
                    } else {
                        Toast.makeText(AddItemActivity.this, "Please enable edit mode first", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        addNewItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewItemDialog();
            }
        });
    }

    private void saveNewItem() {
        // Validate required fields
        if (pcNameInput.getText().toString().trim().isEmpty() ||
                pcSpecsInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create PC document
        Map<String, Object> pcData = new HashMap<>();
        pcData.put("Article", "Desktop Computer"); // Default value
        pcData.put("Amount", amountInput.getText().toString().trim());
        pcData.put("Date Acquired", dateAcquiredInput.getText().toString().trim());
        pcData.put("End User", endUserInput.getText().toString().trim());
        pcData.put("Property Number", pcNameInput.getText().toString().trim());
        pcData.put("Specifications", pcSpecsInput.getText().toString().trim());

        // Save to Firestore
        db.collection("articles").document(pcId)
                .set(pcData)
                .addOnSuccessListener(aVoid -> {
                    // Also create maintenance record
                    saveMaintenanceRecord();
                    showSuccessDialog("New PC item added successfully!");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save item: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateMaintenanceRecord() {
        if (!isEditMode) {
            Toast.makeText(this, "Please enable edit mode first", Toast.LENGTH_SHORT).show();
            return;
        }

        saveMaintenanceRecord();
        showSuccessDialog("Maintenance record updated successfully!");
    }

    private void saveMaintenanceRecord() {
        Map<String, Object> maintenanceData = new HashMap<>();
        maintenanceData.put("pc_id", pcId);
        maintenanceData.put("virus_check", checkboxVirus.isChecked());
        maintenanceData.put("uninstall_programs", checkboxUninstall.isChecked());
        maintenanceData.put("update_software", checkboxUpdate.isChecked());
        maintenanceData.put("last_maintainer", currentUser);
        maintenanceData.put("last_maintenance_date", dateFormat.format(new Date()));
        maintenanceData.put("updated_timestamp", System.currentTimeMillis());

        // Save to Firestore
        db.collection("maintenance").document(pcId)
                .set(maintenanceData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Maintenance record saved successfully", Toast.LENGTH_SHORT).show();
                    updateMaintenanceStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save maintenance record: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if ("add_new".equals(mode)) {
                        // Return to main activity after adding new item
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("item_added", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        // Stay on current screen
                        dialog.dismiss();
                        isEditMode = false;
                        enableEditBtn.setText("Enable Edit");
                        setCheckboxesEnabled(false);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showAddNewItemDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add New Item")
                .setMessage("Would you like to add a new PC to the inventory?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Generate new PC ID
                    String newPcId = "PC-" + System.currentTimeMillis();
                    Intent intent = new Intent(AddItemActivity.this, PCDetailActivity.class);
                    intent.putExtra("pc_id", newPcId);
                    intent.putExtra("mode", "add_new");
                    intent.putExtra("username", currentUser);
                    intent.putExtra("user_role", userRole);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (isEditMode && !"add_new".equals(mode)) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        updateMaintenanceRecord();
                        super.onBackPressed();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}