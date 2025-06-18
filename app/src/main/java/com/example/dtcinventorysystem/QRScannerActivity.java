package com.example.dtcinventorysystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView scanningStatus;
    private TextView scannedDataDisplay; // New TextView to show scanned data
    private ExecutorService cameraExecutor;
    private FirebaseAuth mAuth;
    private boolean isScanning = true;
    private String lastScannedData = ""; // Store the last scanned data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        scanningStatus = findViewById(R.id.scanningStatus);
        scannedDataDisplay = findViewById(R.id.scannedDataDisplay); // Initialize new TextView

        mAuth = FirebaseAuth.getInstance();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        } else {
            startCamera();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::scanBarcode);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                runOnUiThread(() -> scanningStatus.setText("Point camera at PC QR code to scan"));

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
                    scanningStatus.setText("Camera initialization failed");
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void scanBarcode(ImageProxy imageProxy) {
        if (!isScanning || imageProxy == null || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );
        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && isScanning) {
                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();

                        if (rawValue != null && !rawValue.trim().isEmpty() && !rawValue.equals(lastScannedData)) {
                            lastScannedData = rawValue;

                            runOnUiThread(() -> {
                                // Display the actual scanned data
                                scanningStatus.setText("✓ QR Code Scanned Successfully!");
                                scannedDataDisplay.setText("Scanned Data: " + rawValue);
                                scannedDataDisplay.setVisibility(TextView.VISIBLE);

                                Toast.makeText(this, "Scanned: " + rawValue, Toast.LENGTH_LONG).show();
                            });

                            // Wait 2 seconds to let user see the scanned data, then proceed
                            new android.os.Handler().postDelayed(() -> {
                                if (isScanning) {
                                    isScanning = false; // Prevent multiple scans

                                    runOnUiThread(() -> {
                                        // Determine user role and username
                                        FirebaseUser currentUser = mAuth.getCurrentUser();
                                        String userRole = "maintainer"; // Default role
                                        String username = "Unknown User";

                                        if (currentUser != null) {
                                            username = currentUser.getDisplayName();
                                            if (username == null || username.isEmpty()) {
                                                username = currentUser.getEmail();
                                            }

                                            // Determine role based on email or other logic
                                            if (currentUser.getEmail() != null && currentUser.getEmail().contains("admin")) {
                                                userRole = "admin";
                                            }
                                        }

                                        // Create intent to return to calling activity or go to PC detail
                                        Intent resultIntent = new Intent();
                                        resultIntent.putExtra("pc_id", rawValue.trim());
                                        resultIntent.putExtra("username", username);
                                        resultIntent.putExtra("role", userRole);
                                        resultIntent.putExtra("scan_timestamp", System.currentTimeMillis());

                                        // If this was called for result, return the data
                                        if (getIntent().hasExtra("return_result") && getIntent().getBooleanExtra("return_result", false)) {
                                            setResult(RESULT_OK, resultIntent);
                                            finish();
                                        } else {
                                            // Otherwise, launch PC detail activity
                                            resultIntent.setClass(this, PCDetailActivity.class);
                                            startActivity(resultIntent);
                                            finish();
                                        }
                                    });
                                }
                            }, 2000); // 2 second delay
                        }
                    }
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        scanningStatus.setText("Scanning failed, try again");
                    });
                    imageProxy.close();
                })
                .addOnCompleteListener(task -> {
                    scanner.close();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isScanning = false;
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isScanning = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanning = true;
        lastScannedData = ""; // Reset to allow re-scanning same code
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                scanningStatus.setText("Camera permission denied");
                finish();
            }
        }
    }
}