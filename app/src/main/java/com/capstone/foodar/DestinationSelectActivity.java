package com.capstone.foodar;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.databinding.ActivityDestinationSelectBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DestinationSelectActivity extends AppCompatActivity {

    ActivityDestinationSelectBinding binding;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int ASK_LOCATION_PERMISSION = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDestinationSelectBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
    }

    private boolean checkIfGpsIsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void init() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setListeners() {
        binding.imageDestinationBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        binding.buttonDestinationAutoDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLocation();
                Toast.makeText(DestinationSelectActivity.this, "It might take some time to detect your location.", Toast.LENGTH_SHORT).show();
            }
        });
        binding.buttonDestinationConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra(Constants.KEY_DESTINATION, binding.etDestinationAddress.getText().toString()
                        + binding.etDestinationRemarks.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (checkIfGpsIsEnabled()) {
                LocationRequest locationRequest = new LocationRequest.Builder(5000)
                        .setMinUpdateIntervalMillis(2000)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build();

                LocationCallback locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                Geocoder geocoder = new Geocoder(DestinationSelectActivity.this, Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        setLocationInfo(addresses);
                                        fusedLocationProviderClient.removeLocationUpdates(this);
                                        return;
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                };

                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } else {
                Snackbar snackbar = Snackbar
                        .make(binding.main, "Location is not turned on.", Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        } else {
            askPermissionFromUser();
        }
    }

    private void askPermissionFromUser() {
        Dialog dialog = new Dialog(DestinationSelectActivity.this);
        dialog.setContentView(R.layout.dialog_location_permission);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.show();

        Button buttonCancel = dialog.findViewById(R.id.buttonDialogLocationCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button buttonConfirm = dialog.findViewById(R.id.buttonDialogLocationConfirm);
        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                ActivityCompat.requestPermissions(DestinationSelectActivity.this, new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION}, ASK_LOCATION_PERMISSION);
            }
        });
    }

    private void setLocationInfo(List<Address> addresses) {
        binding.etDestinationAddressCity.setText(addresses.get(0).getLocality());
        binding.etDestinationAddress.setText(addresses.get(0).getAddressLine(0));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ASK_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                Toast.makeText(this, "It might take some time to detect your location.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Auto-detect is disabled due to location permission not permitted.", Toast.LENGTH_SHORT).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}