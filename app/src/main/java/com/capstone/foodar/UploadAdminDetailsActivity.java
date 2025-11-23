package com.capstone.foodar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityUploadAdminDetailsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.HashMap;
import java.util.Map;

public class UploadAdminDetailsActivity extends AppCompatActivity {

    ActivityUploadAdminDetailsBinding binding;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private Uri logoUri;
    private PhoneNumberUtil phoneUtil;
    private ActivityResultLauncher<String> selectImageLauncher;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadAdminDetailsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setLaunchers();
        setListeners();
    }

    private void isLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressAdminUpload.setVisibility(View.VISIBLE);
            binding.btnSubmit.setVisibility(View.INVISIBLE);
            binding.btnSubmit.setEnabled(false);
        } else {
            binding.progressAdminUpload.setVisibility(View.GONE);
            binding.btnSubmit.setVisibility(View.VISIBLE);
            binding.btnSubmit.setEnabled(true);
        }
    }

    private void setLaunchers() {
        selectImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        logoUri = uri;
                        binding.ivStoreLogo.setImageURI(uri);
                        binding.ivStoreLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        binding.ivStoreLogo.setBackground(null);
                    }
                });
    }

    private void setListeners() {
        binding.buttonUploadAdminBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCredentialsValid()) {
                    uploadLocationDetailsToDb();
                    isLoading(false);
                }
            }
        });
        binding.ivStoreLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImageLauncher.launch("image/*");
            }
        });
    }

    private void uploadLocationDetailsToDb() {
        if (!preferenceManager.contains(Constants.KEY_EMAIL)) {
            Log.e("PreferenceManager", "No Email");
            return;
        }

        Map<String, Object> locationInfo = new HashMap<>();
        locationInfo.put(Constants.KEY_LOCATION_ADDRESS, binding.etAddress.getText().toString().trim());
        locationInfo.put(Constants.KEY_LOCATION_NAME, binding.etStoreName.getText().toString().trim());
        db.collection(Constants.KEY_LOCATIONS).add(locationInfo)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        uploadUserDetailsToDb(documentReference.getId());
                    }
                });
    }

    private void uploadUserDetailsToDb(String docId) {
        Map<String, Object> merchantInfo = new HashMap<>();
        merchantInfo.put(Constants.KEY_STORE_NAME, binding.etStoreName.getText().toString().trim());
        merchantInfo.put(Constants.KEY_USERNAME, binding.etOwnerName.getText().toString().trim());
        merchantInfo.put(Constants.KEY_PHONE_NUM, binding.etPhone.getText().toString().trim());
        merchantInfo.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
        merchantInfo.put(Constants.KEY_STREET_NAME, binding.etBuilding.getText().toString().trim());
        merchantInfo.put(Constants.KEY_LOCATION_ID, docId);
        merchantInfo.put(Constants.KEY_ADMIN, true);

        Task<DocumentReference> firestoreTask = db.collection(Constants.KEY_USERS_LIST).add(merchantInfo)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UploadAdminDetailsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        isLoading(false);
                        Log.e("Firestore", "Failed Upload: " + e);
                    }
                });

        Task<UploadTask.TaskSnapshot> storageTask = storageRef.child(Constants.KEY_LOCATIONS
                + "/"
                + docId
                + "/"
                + Constants.KEY_LOGO)
                .putFile(logoUri)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UploadAdminDetailsActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        isLoading(false);
                        Log.e("Storage", "Failed Upload: " + e);
                    }
                });

        Tasks.whenAll(firestoreTask, storageTask).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(UploadAdminDetailsActivity.this, "Upload Successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private boolean checkCredentialsValid() {
        String phoneNum = binding.etPhone.getText().toString().trim();

        if (logoUri == null) {
            Toast.makeText(this, "Please upload the logo image", Toast.LENGTH_SHORT).show();
            isLoading(false);
            return false;
        } else if (binding.etStoreName.getText().toString().trim().isEmpty()) {
            binding.etStoreName.setError("Please enter your store name");
            isLoading(false);
            return false;
        } else if (binding.etOwnerName.getText().toString().trim().isEmpty()) {
            binding.etOwnerName.setError("Please enter your real name");
            isLoading(false);
            return false;
        } else if (phoneNum.isEmpty() || !checkPhoneValid(phoneNum)) {
            isLoading(false);
            return false;
        } else if (binding.etBuilding.getText().toString().trim().isEmpty()) {
            binding.etBuilding.setError("Please enter the street name");
            isLoading(false);
            return false;
        } else if (binding.etAddress.getText().toString().trim().isEmpty()) {
            binding.etAddress.setError("Please enter the address of the store");
            isLoading(false);
            return false;
        } else {
            return true;
        }
    }

    private boolean checkPhoneValid(String phoneNum) {
        if (phoneNum.isEmpty()) {
            binding.etPhone.setError("Please enter your phone num");
            return false;
        }

        try {
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNum, "MY");

            if (phoneUtil.isValidNumber(numberProto)) {
                binding.etPhone.setError(null);
                return true;
            } else {
                binding.etPhone.setError("Invalid phone number");
                return false;
            }
        } catch (NumberParseException e) {
            binding.etPhone.setError("Invalid format");
            return false;
        }
    }

    private void init() {
        storageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        logoUri = null;
        phoneUtil = PhoneNumberUtil.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }
}