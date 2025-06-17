package com.example.dtcinventorysystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    Button scanBtn, dashboardBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanBtn = findViewById(R.id.scanBtn);
        dashboardBtn = findViewById(R.id.dashboardBtn);

        scanBtn.setOnClickListener(v -> startActivity(new Intent(this, QRScannerActivity.class)));

        dashboardBtn.setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
    }
}
