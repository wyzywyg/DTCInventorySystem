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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
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
    private TextView scannedDataDisplay;
    private ExecutorService cameraExecutor;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isScanning = true;
    private String lastScannedData = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        previewView = findViewById(R.id.previewView);
        scanningStatus = findViewById(R.id.scanningStatus);
        scannedDataDisplay = findViewById(R.id.scannedDataDisplay);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

                // Optimized settings for faster scanning
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
                            isScanning = false; // Stop further scanning

                            runOnUiThread(() -> {
                                scanningStatus.setText("✓ QR Code Detected - Checking Database...");
                                scannedDataDisplay.setText("Scanned: " + rawValue);
                                scannedDataDisplay.setVisibility(TextView.VISIBLE);
                            });

                            // Check database for the scanned PC ID
                            checkPCInDatabase(rawValue.trim());
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

    private void checkPCInDatabase(String pcId) {
        runOnUiThread(() -> scanningStatus.setText("🔍 Searching database..."));

        db.collection("articles").document(pcId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // PC found in database
                            runOnUiThread(() -> {
                                scanningStatus.setText("✅ PC Record Found!");
                                Toast.makeText(this, "PC found: " + pcId, Toast.LENGTH_SHORT).show();
                            });

                            // Navigate to PC detail with data
                            navigateToPCDetail(pcId, document);
                        } else {
                            // PC not found - show "No Record" popup
                            runOnUiThread(() -> {
                                scanningStatus.setText("❌ No Record Found");
                                showNoRecordDialog(pcId);
                            });
                        }
                    } else {
                        // Database error
                        runOnUiThread(() -> {
                            scanningStatus.setText("❌ Database Error");
                            Toast.makeText(this, "Error checking database: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            // Reset scanning after error
                            new android.os.Handler().postDelayed(() -> {
                                isScanning = true;
                                lastScannedData = "";
                                scanningStatus.setText("Point camera at PC QR code to scan");
                                scannedDataDisplay.setVisibility(TextView.GONE);
                            }, 2000);
                        });
                    }
                });
    }

    private void showNoRecordDialog(String pcId) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("No Record Found")
                .setMessage("The scanned QR code '" + pcId + "' is not found in the database.\n\nWould you like to add this item?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Add Item", (dialog, which) -> {
                    // Navigate to AddItemActivity and pass pcId + mode + user info
                    Intent intent = new Intent(this, AddItemActivity.class);
                    intent.putExtra("pc_id", pcId);
                    intent.putExtra("mode", "add_new");
                    intent.putExtra("username", getCurrentUsername());
                    intent.putExtra("user_role", getCurrentUserRole());
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Scan Again", (dialog, which) -> {
                    // Reset scanning
                    isScanning = true;
                    lastScannedData = "";
                    runOnUiThread(() -> {
                        scanningStatus.setText("Point camera at PC QR code to scan");
                        scannedDataDisplay.setVisibility(TextView.GONE);
                    });
                })
                .setCancelable(false)
                .show();
    }


    private void navigateToPCDetail(String pcId, DocumentSnapshot document) {
        // Small delay to show success message
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(this, PCDetailActivity.class);

            // Pass PC ID and mode
            intent.putExtra("pc_id", pcId);
            intent.putExtra("mode", "view_existing");

            // Pass user info
            intent.putExtra("username", getCurrentUsername());
            intent.putExtra("user_role", getCurrentUserRole());

            // Pass PC data from Firestore
            if (document.contains("Amount")) {
                intent.putExtra("amount", document.getString("Amount"));
            }
            if (document.contains("Article")) {
                intent.putExtra("article", document.getString("Article"));
            }
            if (document.contains("Date Acquired")) {
                intent.putExtra("date_acquired", document.getString("Date Acquired"));
            }
            if (document.contains("End User")) {
                intent.putExtra("end_user", document.getString("End User"));
            }
            if (document.contains("Property Number")) {
                intent.putExtra("property_number", document.getString("Property Number"));
            }
            if (document.contains("Specifications")) {
                intent.putExtra("specifications", document.getString("Specifications"));
            }

            intent.putExtra("scan_timestamp", System.currentTimeMillis());

            startActivity(intent);
            finish();
        }, 1000);
    }

    private String getCurrentUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String username = currentUser.getDisplayName();
            if (username == null || username.isEmpty()) {
                username = currentUser.getEmail();
                if (username != null && username.contains("@")) {
                    username = username.substring(0, username.indexOf("@"));
                }
            }
            return username != null ? username : "Unknown User";
        }
        return "Unknown User";
    }

    private String getCurrentUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            // Simple role determination - you can modify this logic
            if (currentUser.getEmail().contains("admin")) {
                return "admin";
            }
        }
        return "maintainer";
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
        if (!isScanning) {
            isScanning = true;
            lastScannedData = "";
            runOnUiThread(() -> {
                scanningStatus.setText("Point camera at PC QR code to scan");
                scannedDataDisplay.setVisibility(TextView.GONE);
            });
        }
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