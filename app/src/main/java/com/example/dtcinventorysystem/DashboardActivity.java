package com.example.dtcinventorysystem;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity {

    private TextView greetingText, itemCountText, lastUpdatedText;
    private TextView recentlyAddedText, categoryText, lastScanText, locationText;
    private TextView recentlyAddedUpdate, categoryUpdate, lastScanUpdate, locationUpdate;

    private String currentUser = "Maintainer245";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        greetingText = findViewById(R.id.greetingText);
        itemCountText = findViewById(R.id.itemCountText);
        lastUpdatedText = findViewById(R.id.lastUpdatedText);

        // Recently Added Card
        View recentlyAddedCard = findViewById(R.id.recentlyAddedCard);
        recentlyAddedText = recentlyAddedCard.findViewById(R.id.statValue);
        TextView recentlyAddedTitle = recentlyAddedCard.findViewById(R.id.statTitle);
        recentlyAddedUpdate = recentlyAddedCard.findViewById(R.id.statUpdate);
        recentlyAddedTitle.setText("Recently Added");

        // Categories Card
        View categoryCard = findViewById(R.id.categoryCard);
        categoryText = categoryCard.findViewById(R.id.statValue);
        TextView categoryTitle = categoryCard.findViewById(R.id.statTitle);
        categoryUpdate = categoryCard.findViewById(R.id.statUpdate);
        categoryTitle.setText("Categories");

        // Last Scan Card
        View lastScanCard = findViewById(R.id.lastScanCard);
        lastScanText = lastScanCard.findViewById(R.id.statValue);
        TextView lastScanTitle = lastScanCard.findViewById(R.id.statTitle);
        lastScanUpdate = lastScanCard.findViewById(R.id.statUpdate);
        lastScanTitle.setText("Scanned Today");

        // Location Card
        View locationCard = findViewById(R.id.locationCard);
        locationText = locationCard.findViewById(R.id.statValue);
        TextView locationTitle = locationCard.findViewById(R.id.statTitle);
        locationUpdate = locationCard.findViewById(R.id.statUpdate);
        locationTitle.setText("Locations");

        greetingText.setText("Hello! " + currentUser + " 👋");

        fetchDashboardData();
    }

    private void fetchDashboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference articlesRef = db.collection("articles");
        CollectionReference maintenanceRef = db.collection("maintenance");

        String today = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
        String updatedText = "Updated: " + new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());

        // Fetch articles
        articlesRef.get().addOnSuccessListener(articleSnapshots -> {
            recentlyAddedUpdate.setText(updatedText);
            categoryUpdate.setText(updatedText);
            locationUpdate.setText(updatedText);

            int totalItems = articleSnapshots.size();
            int recentlyAdded = 0;
            Set<String> categories = new HashSet<>();

            for (QueryDocumentSnapshot doc : articleSnapshots) {
                String dateAcquired = doc.getString("Date Acquired");
                if (today.equals(dateAcquired)) {
                    recentlyAdded++;
                }

                String category = doc.getString("Article");
                if (category != null) {
                    categories.add(category);
                }
            }

            itemCountText.setText(String.valueOf(totalItems));
            recentlyAddedText.setText(String.valueOf(recentlyAdded));
            categoryText.setText("7");
            locationText.setText("5"); // Static for now

            lastUpdatedText.setText(updatedText);
        });

        // Fetch maintenance data
        maintenanceRef.get().addOnSuccessListener(maintenanceSnapshots -> {
            lastScanUpdate.setText(updatedText);

            int scanToday = 0;
            for (QueryDocumentSnapshot doc : maintenanceSnapshots) {
                String lastScan = doc.getString("last_maintenance_date");
                if (today.equals(lastScan)) {
                    scanToday++;
                }
            }

            lastScanText.setText(String.valueOf(scanToday));
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading maintenance data", Toast.LENGTH_SHORT).show();
        });
    }
}
