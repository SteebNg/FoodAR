package com.capstone.foodar;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.print.PrintHelper;

import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.FragmentAdminGenerateTableQrBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class AdminGenerateTableQrFragment extends Fragment {

    private FragmentAdminGenerateTableQrBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminGenerateTableQrBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();
        setListeners();

        return view;
    }

    private void setListeners() {
        binding.qrGenerateQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateQrCode();
            }
        });
        binding.qrDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToStorage();
            }
        });
        binding.qrPrintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printQrCode();
            }
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(requireContext());

        // Initially show empty state and hide QR display
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
        binding.qrDisplayCard.setVisibility(View.GONE);
        binding.qrDownloadButton.setVisibility(View.GONE);
        binding.qrPrintButton.setVisibility(View.GONE);
    }

    private void generateQrCode() {
        String tableNum = binding.qrTableNumEditText.getText().toString().trim();
        if (tableNum.isEmpty()) {
            binding.qrTableNumEditText.setError("Enter a table num");
            return;
        }

        String locationId = preferenceManager.getString(Constants.KEY_LOCATION_ID);
        if (locationId == null || locationId.isEmpty()) {
            Toast.makeText(getContext(), "Location ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrCodeData = tableNum + ", " + locationId;

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 600, 600);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            binding.qrImageView.setImageBitmap(bitmap);

            binding.qrTableNameText.setText(tableNum);

            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.qrDisplayCard.setVisibility(View.VISIBLE);

            Toast.makeText(getContext(), "QR Code generated successfully!", Toast.LENGTH_SHORT).show();

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void printQrCode() {
        if (PrintHelper.systemSupportsPrint()) {
            BitmapDrawable drawable = (BitmapDrawable) binding.qrImageView.getDrawable();
            if (drawable == null) {
                Toast.makeText(getContext(), "QR code image not found.", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = drawable.getBitmap();
            PrintHelper printHelper = new PrintHelper(requireActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("Table " + binding.qrTableNumEditText.getText().toString() + " QR Code", bitmap);

            binding.qrPrintButton.setEnabled(true);
            binding.qrDownloadButton.setEnabled(true);
            binding.qrPrintButton.setVisibility(View.VISIBLE);
            binding.qrDownloadButton.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getContext(), "Printing is not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToStorage() {
        BitmapDrawable drawable = (BitmapDrawable) binding.qrImageView.getDrawable();
        if (drawable == null) {
            Toast.makeText(getContext(), "QR code image not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bitmap = drawable.getBitmap();

        OutputStream fos;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "table_qr_" + binding.qrTableNumEditText.getText().toString() + ".jpg");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = requireContext().getContentResolver().openOutputStream(Objects.requireNonNull(uri));
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                File image = new File(imagesDir, "table_qr_" + binding.qrTableNumEditText.getText().toString() + ".jpg");
                fos = new FileOutputStream(image);
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Objects.requireNonNull(fos).close();
            Toast.makeText(getContext(), "QR Code saved to Pictures folder", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving QR code", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
