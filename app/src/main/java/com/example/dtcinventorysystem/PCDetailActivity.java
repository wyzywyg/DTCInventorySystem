package com.example.dtcinventorysystem;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PCDetailActivity extends AppCompatActivity {

    // UI Components
    private TextView pcIdText;
    private EditText pcNameInput, pcSpecsInput, dateAcquiredInput;
    private TextView lastMaintainedDisplay;
    private CheckBox checkboxVirus, checkboxUninstall, checkboxUpdate;
    private ImageView virusCheckIcon, uninstallIcon, updateIcon;
    private TextView performedByText, maintenanceDateText;
    private TextView statusText, lastUpdatedText, maintainerText, diagnosticsText;
    private Button updateBtn, addNewItemBtn;

    // User data
    private String currentUser = ""; // Will be set based on login
    private String userRole = ""; // "admin" or "maintainer"
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pcdetail);

        initializeViews();
        setupUserData();
        loadSavedData();
        setupDatePickers();
        setupCheckboxListeners();
        setupButtons();
        updateMaintenanceStatus();
    }

    private void initializeViews() {
        pcIdText = findViewById(R.id.pc_id_text);
        pcNameInput = findViewById(R.id.pc_name_input);
        pcSpecsInput = findViewById(R.id.pc_specs_input);
        dateAcquiredInput = findViewById(R.id.date_acquired_input);
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
        addNewItemBtn = findViewById(R.id.add_new_item_btn);

        preferences = getSharedPreferences("PCMaintenance", MODE_PRIVATE);
    }

    private void setupUserData() {
        // In a real app, this would come from login/authentication
        // For demo purposes, you can set these values or get from Intent
        currentUser = getIntent().getStringExtra("username");
        userRole = getIntent().getStringExtra("user_role");

        // Default values for testing
        if (currentUser == null) currentUser = "Maintainer245";
        if (userRole == null) userRole = "maintainer";
    }

    private void loadSavedData() {
        // Load saved PC data
        pcNameInput.setText(preferences.getString("pc_name", "COM77-17"));
        pcSpecsInput.setText(preferences.getString("pc_specs", "PC-110 Intel Core i7-6700 Processor 3.4 GHz"));
        dateAcquiredInput.setText(preferences.getString("date_acquired", "February 24, 2017"));

        // Load maintenance status
        checkboxVirus.setChecked(preferences.getBoolean("virus_check", true));
        checkboxUninstall.setChecked(preferences.getBoolean("uninstall_programs", false));
        checkboxUpdate.setChecked(preferences.getBoolean("update_software", false));

        // Load maintenance info
        String lastMaintainer = preferences.getString("last_maintainer", currentUser);
        String lastMaintenanceDate = preferences.getString("last_maintenance_date", dateFormat.format(new Date()));

        lastMaintainedDisplay.setText(lastMaintenanceDate);
        maintainerText.setText("Last maintained by: " + lastMaintainer);
        lastUpdatedText.setText("Last Updated: " + lastMaintenanceDate);
    }

    private void setupDatePickers() {
        dateAcquiredInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(dateAcquiredInput);
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
                if (isChecked) {
                    updateMaintenanceInfo("virus_check");
                }
            }
        });

        checkboxUninstall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTaskIcon(uninstallIcon, isChecked);
                if (isChecked) {
                    updateMaintenanceInfo("uninstall_programs");
                }
            }
        });

        checkboxUpdate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateTaskIcon(updateIcon, isChecked);
                if (isChecked) {
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

        // Update the maintenance info display
        performedByText.setText("Performed by: " + currentUser);
        maintenanceDateText.setText("Date: " + currentDate);
        lastMaintainedDisplay.setText(currentDate);
        lastUpdatedText.setText("Last Updated: " + currentDate);
        maintainerText.setText("Last maintained by: " + currentUser);

        // Update overall status
        updateMaintenanceStatus();

        // Show notification
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

        // Update icons based on current status
        updateTaskIcon(virusCheckIcon, checkboxVirus.isChecked());
        updateTaskIcon(uninstallIcon, checkboxUninstall.isChecked());
        updateTaskIcon(updateIcon, checkboxUpdate.isChecked());
    }

    private void setupButtons() {
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllData();
                showUpdateConfirmation();
            }
        });

        addNewItemBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewItemDialog();
            }
        });
    }

    private void saveAllData() {
        SharedPreferences.Editor editor = preferences.edit();

        // Save PC information
        editor.putString("pc_name", pcNameInput.getText().toString());
        editor.putString("pc_specs", pcSpecsInput.getText().toString());
        editor.putString("date_acquired", dateAcquiredInput.getText().toString());

        // Save maintenance status
        editor.putBoolean("virus_check", checkboxVirus.isChecked());
        editor.putBoolean("uninstall_programs", checkboxUninstall.isChecked());
        editor.putBoolean("update_software", checkboxUpdate.isChecked());

        // Save maintenance info
        editor.putString("last_maintainer", currentUser);
        editor.putString("last_maintenance_date", dateFormat.format(new Date()));

        editor.apply();
    }

    private void showUpdateConfirmation() {
        String message = "PC maintenance record updated successfully!\n\n" +
                "Updated by: " + currentUser + " (" + userRole + ")\n" +
                "Date: " + dateFormat.format(new Date());

        new AlertDialog.Builder(this)
                .setTitle("Update Successful")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAddNewItemDialog() {
        // Check user permissions
        if (!userRole.equals("admin")) {
            Toast.makeText(this, "Only administrators can add new items", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Add New PC")
                .setMessage("Do you want to add a new PC to the maintenance system?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // In a real app, this would navigate to a new PC entry form
                    Toast.makeText(this, "Navigate to Add New PC form", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveAllData(); // Auto-save when leaving the activity
    }
}