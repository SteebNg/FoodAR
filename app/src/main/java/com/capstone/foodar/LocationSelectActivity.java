package com.capstone.foodar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.LocationSelectListAdapter;
import com.capstone.foodar.Model.Location;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityFoodDetailsBinding;
import com.capstone.foodar.databinding.ActivityLocationSelectBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class LocationSelectActivity extends AppCompatActivity {

    public static final String LOCATION_ID_FOR_RESULT = "LOCATION_ID_FOR_RESULT";
    ActivityLocationSelectBinding binding;
    private FirebaseFirestore db;
    private ArrayList<Location> locations;
    private LocationSelectListAdapter locationSelectListAdapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationSelectBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        getLocations();
        setListeners();
    }

    private void setListeners() {
        binding.searchBarLocation.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterLocation(newText);
                return true;
            }
        });
        binding.buttonLocationBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void filterLocation(String query) {
        ArrayList<Location> filteredLocations = new ArrayList<>();

        for (Location location : locations) {
            if (location.locationName.toLowerCase().contains(query.toLowerCase())
                    || location.locationAddress.toLowerCase().contains(query.toLowerCase())) {
                filteredLocations.add(location);
            }
        }

        locationSelectListAdapter.setFilteredLocationList(filteredLocations);
    }

    private void getLocations() {
        db.collection(Constants.KEY_LOCATIONS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            int locationsProcessed = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Location location = new Location();
                                location.locationName = document.getString(Constants.KEY_LOCATION_NAME);
                                location.locationAddress = document.getString(Constants.KEY_LOCATION_ADDRESS);
                                location.locationId = document.getId();
                                locations.add(location);
                                locationsProcessed++;

                                if (locationsProcessed == task.getResult().size()) {
                                    addLocationToRecycler();
                                }
                            }
                        }
                    }
                });
    }

    private void addLocationToRecycler() {
        locationSelectListAdapter = getLocationSelectAdapter();
        binding.recyclerLocationList.setAdapter(locationSelectListAdapter);
        locationSelectListAdapter.notifyDataSetChanged();
    }

    private LocationSelectListAdapter getLocationSelectAdapter() {
        LocationSelectListAdapter adapter = new LocationSelectListAdapter(locations, getApplicationContext());

        adapter.setOnItemClickListener(new LocationSelectListAdapter.OnItemClickListener() {
            @Override
            public void onClick(Location location) {
                preferenceManager.putString(Constants.KEY_LOCATION_ID, location.locationId);
                preferenceManager.putString(Constants.KEY_LOCATION_NAME, location.locationName);

                Intent intent = new Intent();
                intent.putExtra(LOCATION_ID_FOR_RESULT, location.locationName);
                setResult(RESULT_OK);
                finish();
            }
        });

        return adapter;
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        locations = new ArrayList<>();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }
}