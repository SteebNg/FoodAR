package com.capstone.foodar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.DecodeCallback;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityQrCodeScanBinding;
import com.google.zxing.Result;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class QrCodeScanActivity extends AppCompatActivity {

    // https://stackoverflow.com/questions/4322182/base64-encoder-and-decoder
    // for the encoding side

    ActivityQrCodeScanBinding binding;
    private PreferenceManager preferenceManager;
    private CodeScanner codeScanner;
    private boolean cameraPermission = false;
    private final int PERMISSION_CODE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrCodeScanBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        checkForPermissions();
        if (cameraPermission) {
            setListeners();
        }
    }

    private void checkForPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, PERMISSION_CODE);
        } else {
            cameraPermission = true;
        }
    }

    private void setListeners() {
        codeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String[] partsDecryptedString = result.getText().split(",");
                        preferenceManager.putString(Constants.KEY_LOCATION_ID, partsDecryptedString[0].trim());
                        preferenceManager.putString(Constants.KEY_TABLE_NUM, partsDecryptedString[1].trim());

                        Toast.makeText(QrCodeScanActivity.this, "QR Scanned. Have a great meal.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
        });
        binding.codeScannerQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                codeScanner.startPreview();
            }
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        codeScanner = new CodeScanner(this, binding.codeScannerQr);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraPermission = true;
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                cameraPermission = false;
                Toast.makeText(this, "QR Code Scanner will not work without permission", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }
}