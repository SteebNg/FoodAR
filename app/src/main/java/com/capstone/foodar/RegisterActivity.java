package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        if (!isLoggedIn()) {
            setListeners();
        } else {
            showToast("It is detected that you are already signed in to your account.");
            Log.d("isLoggedIn", "User logged in detected.");
        }
    }

    private void setListeners() {
        binding.textRegisterLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //just destroy this activity cause LoginActivity is still not destroyed yet
            }
        });
        binding.buttonRegisterHidePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonRegisterShowPassword.setVisibility(View.VISIBLE);
                binding.buttonRegisterHidePassword.setVisibility(View.GONE);
                binding.etRegisterPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        });
        binding.buttonRegisterShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonRegisterShowPassword.setVisibility(View.GONE);
                binding.buttonRegisterHidePassword.setVisibility(View.VISIBLE);
                binding.etRegisterPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });
        binding.buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRegisterButtonLoading(true);
                binding.textRegisterError.setVisibility(View.INVISIBLE);
                boolean isCredentialsValid = credentialsValid();

                if (isCredentialsValid) {
                    firebaseAuth.createUserWithEmailAndPassword(
                            binding.etRegisterEmail.getText().toString().trim(),
                            binding.etRegisterPassword.getText().toString().trim()
                    ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                currentUser = firebaseAuth.getCurrentUser();

                                // setDisplayName
                                UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(binding.etRegisterUsername.getText().toString())
                                        .build();
                                currentUser.updateProfile(profileUpdate)
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d("Setting Profile Username", "Failed");
                                            }
                                        });

                                sendVerificationEmail();
                                firebaseAuth.signOut();
                            }
                        }
                    });
                } else {
                    isRegisterButtonLoading(false);
                }
            }
        });
    }

    private void setDefaultProfilePic() {
        storageReference.child(Constants.KEY_USERS_LIST + "/" + currentUser.getUid() + "/" + Constants.KEY_PROFILE_IMAGE)
                .putFile(getRandomProfileImage())
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Intent intent = new Intent(RegisterActivity.this, VerificationActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private Uri getRandomProfileImage() {
        Random random = new Random();
        int num = random.nextInt(6);
        int imageChoosenId;

        switch (num) {
            case 0: {
                imageChoosenId = R.drawable.capybara;
                break;
            }
            case 1: {
                imageChoosenId = R.drawable.ketchup;
                break;
            }
            case 2: {
                imageChoosenId = R.drawable.pizza;
                break;
            }
            case 3: {
                imageChoosenId = R.drawable.santa_hat;
                break;
            }
            case 4: {
                imageChoosenId = R.drawable.teddybear;
                break;
            }
            case 5: {
                imageChoosenId = R.drawable.watermelon;
                break;
            }
            default: {
                imageChoosenId = R.drawable.watermelon;
                break;
            }
        }
        String packageName = getPackageName();

        return Uri.parse("android.resource://" + packageName + "/" + imageChoosenId);
    }

    private void sendVerificationEmail() {
        currentUser.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                registerUserToDb();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                currentUser.delete();
                showToast("There is a problem with the internet connection. Please try again.");
                Log.d("Send Verification Email", "Message: " + e);
            }
        });
    }

    private void registerUserToDb() {
        Map<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_EMAIL, binding.etRegisterEmail.getText().toString().trim());
        user.put(Constants.KEY_USERNAME, binding.etRegisterUsername.getText().toString().trim());

        db.collection(Constants.KEY_USERS_LIST).document(currentUser.getUid())
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        setDefaultProfilePic();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showErrorMessage("Firestore Error");
                    }
                });
    }

    private boolean credentialsValid() {
        String username = binding.etRegisterUsername.getText().toString().trim();
        String email = binding.etRegisterEmail.getText().toString().trim();
        String password = binding.etRegisterPassword.getText().toString();

        if (username.isEmpty()) {
            showErrorMessage("Please enter your username.");
            return false;
        } else if (email.isEmpty()) {
            showErrorMessage("Please enter your email.");
            return false;
        } else if (!(Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            showErrorMessage("Please enter a valid email.");
            return false;
        } else if (password.isEmpty()) {
            showErrorMessage("Please enter your password with at least 6 characters.");
            return false;
        } else if (password.length() < 6) {
            showErrorMessage("Your password must contain at least 6 characters.");
            return false;
        } else if (!password.equals(binding.etRegisterConfirmPassword.getText().toString())) {
            showErrorMessage("The password and confirmed password is not the same");
            return false;
        } else {
            return true;
        }
    }

    private void showErrorMessage(String msg) {
        binding.textRegisterError.setVisibility(View.VISIBLE);
        binding.textRegisterError.setText(msg);
    }

    private void isRegisterButtonLoading(boolean isLoading) {
        if (isLoading) {
            binding.buttonRegister.setText("");
            binding.buttonRegister.setEnabled(false);
            binding.progressBarRegister.setVisibility(View.VISIBLE);
        } else {
            binding.buttonRegister.setText("Register");
            binding.buttonRegister.setEnabled(true);
            binding.progressBarRegister.setVisibility(View.GONE);
        }
    }

    private void init() {
        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        db = FirebaseFirestore.getInstance();
    }

    private boolean isLoggedIn() {
        // DO NOT USE FIREBASE WAY TO CHECK, even if it is listed in their documentation
        return preferenceManager.contains(Constants.KEY_EMAIL);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}