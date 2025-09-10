package com.capstone.foodar;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
    }

    private void setListeners() {
        binding.textLoginSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        binding.buttonLoginShowPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonLoginHidePassword.setVisibility(View.VISIBLE);
                binding.buttonLoginShowPassword.setVisibility(View.GONE);
                binding.etLoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
        });
        binding.buttonLoginHidePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.buttonLoginHidePassword.setVisibility(View.GONE);
                binding.buttonLoginShowPassword.setVisibility(View.VISIBLE);
                binding.etLoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });
        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginButtonLoading(true);
                binding.textLoginError.setVisibility(View.INVISIBLE);
                boolean credentialsValid = isCredentialsValid();

                if (credentialsValid) {
                    signInUser();
                } else {
                    isLoginButtonLoading(false);
                }
            }
        });
    }

    private void signInUser() {
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    currentUser = firebaseAuth.getCurrentUser();

//                    if (currentUser.isEmailVerified()) {
//                        preferenceManager.putString(Constants.KEY_EMAIL, email);
//                        preferenceManager.putString(Constants.KEY_USER_ID, currentUser.getUid());
//                        preferenceManager.putString(Constants.KEY_USERNAME, currentUser.getDisplayName());
//
//                        finish();
//                    } else {
//                        showError("Please verify your email first.");
//                    }

                    preferenceManager.putString(Constants.KEY_EMAIL, email);
                    preferenceManager.putString(Constants.KEY_USER_ID, currentUser.getUid());
                    preferenceManager.putString(Constants.KEY_USERNAME, currentUser.getDisplayName());

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    isLoginButtonLoading(false);
                }
            }
        });
    }

    private void showError(String msg) {
        binding.textLoginError.setVisibility(View.VISIBLE);
        binding.textLoginError.setText(msg);
    }

    private boolean isCredentialsValid() {
        String email = binding.etLoginEmail.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString();

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return false;
        } else if (!(Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            showError("Please enter a valid email.");
            return false;
        } else if (password.isEmpty()) {
            showError("Please enter your password");
            return false;
        } else if (password.length() < 6) {
            showError("The password is at least 6 characters long");
            return false;
        } else {
            return true;
        }
    }

    private void isLoginButtonLoading(boolean isLoading) {
        if (isLoading) {
            binding.buttonLogin.setEnabled(false);
            binding.buttonLogin.setText("");
            binding.progressBarLogin.setVisibility(View.VISIBLE);
        } else {
            binding.buttonLogin.setEnabled(true);
            binding.buttonLogin.setText("Login");
            binding.progressBarLogin.setVisibility(View.GONE);
        }
    }

    private void init() {
        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }

    private boolean isLoggedIn() {
        if (preferenceManager.contains(Constants.KEY_EMAIL)) {
            showToast("Already Logged In");
            Log.d("Logged In?", "Logged");
            return true;
        } else {
            return false;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void checkForInternet() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // TODO: Hide message
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // TODO: Show Message
            }
        };
    }
}