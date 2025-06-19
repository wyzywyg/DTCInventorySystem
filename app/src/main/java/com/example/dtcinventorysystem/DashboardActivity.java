package com.example.dtcinventorysystem;


import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;


public class DashboardActivity extends AppCompatActivity {
    ListView pcListView;
    DatabaseReference pcRef;
    ArrayAdapter<String> adapter;
    ArrayList<String> pcSummaries = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        pcListView = findViewById(R.id.pc_list);
        pcRef = FirebaseDatabase.getInstance().getReference("pcs");


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pcSummaries);
        pcListView.setAdapter(adapter);


        pcRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                pcSummaries.clear();
                for (DataSnapshot pcSnap : snapshot.getChildren()) {
                    String pcId = pcSnap.getKey();
                    Boolean virus = pcSnap.child("virusCheck").getValue(Boolean.class);
                    Boolean update = pcSnap.child("updateSoftware").getValue(Boolean.class);
                    pcSummaries.add(pcId + ": Virus Check - " + virus + ", Updated - " + update);
                }
                adapter.notifyDataSetChanged();
            }


            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}

