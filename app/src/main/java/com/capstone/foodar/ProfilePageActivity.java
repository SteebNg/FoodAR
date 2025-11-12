package com.capstone.foodar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityProfilePageBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class ProfilePageActivity extends AppCompatActivity {

    ActivityProfilePageBinding binding;
    private String userId;
    private StorageReference storageRef;
    private FirebaseAuth auth;
    private PreferenceManager preferenceManager;
    private ActivityResultLauncher<Intent> profilePicLauncher;
    private Uri profilePicUri;
    private int REQUEST_STORAGE_PERMISSION = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfilePageBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setProfile();
        setListeners();
        setLaunchers();
        checkForStoragePermission();
    }

    private void setLaunchers() {
        profilePicLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        profilePicUri = result.getData().getData();
                        storageRef.child(Constants.KEY_USERS_LIST
                                + "/"
                                + preferenceManager.getString(Constants.KEY_USER_ID)
                                + "/"
                                + Constants.KEY_PROFILE_IMAGE)
                                .putFile(profilePicUri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        init();
                                        setProfile();
                                    }
                                });
                    }
                });
    }

    private void setListeners() {
        binding.buttonProfileLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preferenceManager.clearString(Constants.KEY_USER_ID);
                preferenceManager.clearString(Constants.KEY_EMAIL);
                preferenceManager.clearString(Constants.KEY_USERNAME);
                auth.signOut();
                setResult(RESULT_OK);
                finish();
            }
        });
        binding.imageProfileBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.buttonProfileHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfilePageActivity.this, OrderHistoryActivity.class);
                startActivity(intent);
            }
        });
        binding.buttonProfileChangeProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        profilePicLauncher.launch(intent);
    }

    private void checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void setProfile() {
        storageRef.child(Constants.KEY_USERS_LIST
                + "/"
                + userId
                + "/"
                + Constants.KEY_PROFILE_IMAGE)
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getApplicationContext()).load(uri).into(binding.imageProfileUserPicture);
                    }
                });

        binding.textProfileUserName.setText(auth.getCurrentUser().getDisplayName());
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        storageRef = FirebaseStorage.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!(requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, "Storage permission is required to upload images and files", Toast.LENGTH_SHORT).show();
        }
    }
}