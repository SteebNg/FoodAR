package com.capstone.foodar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.Model.FoodOption;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityAdminAddFoodBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminAddFoodActivity extends AppCompatActivity {

    private ActivityAdminAddFoodBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private static final int REQUEST_STORAGE_PERMISSION = 10;
    private ActivityResultLauncher<Intent> foodImageLauncher;
    private ActivityResultLauncher<Intent> objFileLauncher;
    private ActivityResultLauncher<Intent> mtlFileLauncher;
    private ActivityResultLauncher<Intent> jpgFileLauncher;
    private Uri foodImageUri;
    private Uri objFileUri;
    private Uri mtlFileUri;
    private Uri jpgFileUri;
    private ArrayList<FoodOption> foodOptions;
    private ArrayList<SubOption> subOptions;
    private PopupMenu popupMenu;
    private PreferenceManager preferenceManager;
    private ArrayList<String> foodCategories;
    private String currentFoodCategory;
    private boolean wantModelUpload;
    String[] generatedFoodId;
    private boolean addCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminAddFoodBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        initActivityResultLaunchers();
        getAllFoodCategories();
        setListeners();
        checkForStoragePermission();
    }

    private void checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void setListeners() {
        binding.buttonAdminAddFoodUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
        binding.buttonAdminAddFoodUploadObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(objFileLauncher, "*/*");
            }
        });
        binding.buttonAdminAddFoodUploadMtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(mtlFileLauncher, "/");
            }
        });
        binding.buttonAdminAddFoodUploadJpg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(mtlFileLauncher, "image/*");
            }
        });
        binding.buttonAdminAddFoodAddOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewOption();
            }
        });
        binding.buttonAdminAddFoodSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitToDb();
            }
        });
        binding.buttonAdminAddFoodAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addCategory) {
                    binding.imageAdminAddFoodHideCategory.setVisibility(View.VISIBLE);
                    binding.imageAdminAddFoodAddCategory.setVisibility(View.GONE);
                    binding.layoutAdminAddFoodAddFoodCategory.setVisibility(View.VISIBLE);
                    addCategory = true;
                } else {
                    binding.imageAdminAddFoodHideCategory.setVisibility(View.GONE);
                    binding.imageAdminAddFoodAddCategory.setVisibility(View.VISIBLE);
                    binding.layoutAdminAddFoodAddFoodCategory.setVisibility(View.GONE);
                    addCategory = false;
                }
            }
        });
        binding.buttonAdminAddFoodAddCategoryConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String categoryToBeAdded = binding.etAdminAddFoodAddCategory.getText().toString().trim();

                if (categoryToBeAdded.isEmpty()) {
                    Toast.makeText(AdminAddFoodActivity.this, "Please enter a food category.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog alertDialog = getAlertDialog(categoryToBeAdded);
                alertDialog.show();
            }
        });
    }

    private AlertDialog getAlertDialog(String categoryToBeAdded) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AdminAddFoodActivity.this);
        builder.setMessage("You are about to add the category: " + categoryToBeAdded);
        builder.setTitle("Confirmation on adding food category");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            addCategoryToDb(categoryToBeAdded);
        });
        return builder.create();
    }

    private void addCategoryToDb(String category) {
        db.collection(Constants.KEY_LOCATIONS).document(preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .update(Constants.KEY_FOOD_CATEGORIES, FieldValue.arrayUnion(category))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        foodCategories.add(category);
                        popupMenu.getMenu().add(category);

                        binding.imageAdminAddFoodHideCategory.setVisibility(View.GONE);
                        binding.imageAdminAddFoodAddCategory.setVisibility(View.VISIBLE);
                        binding.layoutAdminAddFoodAddFoodCategory.setVisibility(View.GONE);
                        addCategory = false;
                    }
                });
    }

    private void getAllFoodCategories() {
        db.collection(Constants.KEY_LOCATIONS)
                .document(preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            return;
                        }

                        foodCategories = (ArrayList<String>) document.get(Constants.KEY_FOOD_CATEGORIES);
                        setFoodCategoryPopupMenu();
                    }
                });
    }

    private void setFoodCategoryPopupMenu() {
        popupMenu = new PopupMenu(AdminAddFoodActivity.this, binding.buttonAdminAddFoodFoodCategory);
        popupMenu.getMenuInflater().inflate(R.menu.menu_food_categories, popupMenu.getMenu());

        binding.textAdminAddFoodFoodCategory.setText(foodCategories.get(0));

        for (String categories : foodCategories) {
            popupMenu.getMenu().add(categories);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                currentFoodCategory = (String) item.getTitle();
                binding.textAdminAddFoodFoodCategory.setText(currentFoodCategory);
                return true;
            }
        });
    }

    private void submitToDb() {
        String foodName = binding.etAdminAddFoodFoodName.getText().toString().trim();
        String foodDesc = binding.etAdminAddFoodFoodDescription.getText().toString().trim();
        String foodPrice = binding.etAdminAddFoodFoodPrice.getText().toString().trim();

        if (foodName.isEmpty()) {
            binding.etAdminAddFoodFoodName.setError("Food name is required");
            binding.etAdminAddFoodFoodName.requestFocus();
            return;
        }

        if (foodDesc.isEmpty()) {
            binding.etAdminAddFoodFoodDescription.setError("Food description is required");
            binding.etAdminAddFoodFoodDescription.requestFocus();
            return;
        }

        if (foodPrice.isEmpty()) {
            binding.etAdminAddFoodFoodPrice.setError("Food price is required");
            binding.etAdminAddFoodFoodPrice.requestFocus();
            return;
        }

        if (foodImageUri == null) {
            Toast.makeText(this, "Please upload a food image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (objFileUri != null) {
            if (mtlFileUri == null || jpgFileUri == null) {
                Snackbar snackbar = Snackbar.make(binding.main
                        , "OBJ file requires MTL and JPG file to be uploaded too."
                        , Snackbar.LENGTH_LONG);
                snackbar.show();
                return;
            } else {
                wantModelUpload = true;
            }
        }

        boolean isSuccessful = collectOptionsData();

        if (isSuccessful) {
            Map<String, Object> food = new HashMap<>();
            food.put(Constants.KEY_FOOD_NAME, foodName);
            food.put(Constants.KEY_FOOD_DESC, foodDesc);
            food.put(Constants.KEY_FOOD_PRICE, Double.parseDouble(foodPrice));
            food.put(Constants.KEY_FOOD_CATEGORY, currentFoodCategory);
            food.put(Constants.KEY_FOODS_RATING, 0.0);

            db.collection(Constants.KEY_FOODS).add(food)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            documentReference.update(Constants.KEY_LOCATION_ID, FieldValue.arrayUnion(preferenceManager.getString(Constants.KEY_LOCATION_ID)));
                            generatedFoodId[0] = documentReference.getId();
                        }
                    });

            Map<String, Object> foodOptionsToBeSentToDb = new HashMap<>();
            for (FoodOption option : foodOptions) {
                foodOptionsToBeSentToDb.put(option.optionTitle, option.individualOptions);
            }

            db.collection(Constants.KEY_FOOD_OPTIONS).document(generatedFoodId[0]).set(foodOptionsToBeSentToDb)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.d("Upload Food Option", "Uploaded Successfully.");
                        }
                    });

            storageRef.child(Constants.KEY_FOODS
                    + "/"
                    + generatedFoodId[0]
                    + "/")
                    .putFile(foodImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d("Food Image Upload", "Successfully Uploaded");
                        }
                    });

            if (wantModelUpload) {
                uploadObjFileToDb();
            }
        }
    }

    private void uploadObjFileToDb() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + generatedFoodId[0]
                        + "/3DModel.obj")
                .putFile(objFileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload OBJ File", "Upload Successfully");
                        uploadMtlFileToDb();
                    }
                });
    }

    private void uploadMtlFileToDb() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + generatedFoodId[0]
                        + "/3DModel.mtl")
                .putFile(mtlFileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload MTL File", "Upload Successfully");
                        uploadJpgFileToDb();
                    }
                });
    }

    private void uploadJpgFileToDb() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + generatedFoodId[0]
                        + "/3DModel.jpg")
                .putFile(jpgFileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload JPG File", "Upload Successfully");

                        Intent intent = new Intent(AdminAddFoodActivity.this, AdminAddFoodSuccessActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private boolean collectOptionsData() {
        boolean isSuccessful = false;

        for (int i = 0; i < binding.layoutOptionsContainer.getChildCount(); i++) {
            View optionView = binding.layoutOptionsContainer.getChildAt(i);
            TextInputEditText etOptionName = optionView.findViewById(R.id.etLayoutAdminAddFoodOptionName);
            LinearLayout layoutSubOptionsContainer = optionView.findViewById(R.id.layoutAdminAddFoodSubOptionsContainer);
            Map<String, Double> foodSubOptions = new HashMap<>();

            FoodOption foodOption = foodOptions.get(i);
            foodOption.optionTitle = etOptionName.getText().toString().trim();

            for (int j = 0; j < layoutSubOptionsContainer.getChildCount(); j++) {
                View subOptionView = layoutSubOptionsContainer.getChildAt(j);
                TextInputEditText etSubOptionName = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionName);
                TextInputEditText etSubOptionPrice = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionPrice);

                SubOption subOption = subOptions.get(j);
                subOption.name = etSubOptionName.getText().toString().trim();

                String priceString = etSubOptionPrice.getText().toString().trim();
                if (!priceString.isEmpty()) {
                    try {
                        subOption.price = Double.parseDouble(priceString);
                        foodSubOptions.put(subOption.name, subOption.price);

                        isSuccessful = true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Price must be numeric", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            foodOption.individualOptions = foodSubOptions;
        }
        return isSuccessful;
    }

    private void addNewOption() {
        FoodOption foodOption = new FoodOption();
        foodOptions.add(foodOption);

        View optionView = LayoutInflater.from(this)
                .inflate(R.layout.layout_admin_add_food_item_food_option
                        , binding.layoutOptionsContainer
                        , false);

        TextInputEditText etOptionName = optionView.findViewById(R.id.etLayoutAdminAddFoodOptionName);
        ImageButton btnRemoveOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodRemoveOption);
        LinearLayout layoutSubOptionsContainer = optionView.findViewById(R.id.layoutAdminAddFoodSubOptionsContainer);
        Button btnAddSubOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodAddSubOption);

        btnRemoveOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSubOptionsContainer.removeView(optionView);
                foodOptions.remove(foodOption);
            }
        });

        btnAddSubOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewSubOption(layoutSubOptionsContainer, foodOption);
            }
        });

        layoutSubOptionsContainer.addView(optionView);
    }

    private void addNewSubOption(LinearLayout container, FoodOption foodOption) {
        SubOption subOption = new SubOption();
        subOptions.add(subOption);

        View subOptionView = LayoutInflater.from(this).inflate(R.layout.layout_admin_add_food_item_sub_option
                , container
                , false);

        TextInputEditText etSubOptionName = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionName);
        TextInputEditText etSubOptionPrice = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionPrice);
        ImageButton btnRemoveSubOption = subOptionView.findViewById(R.id.buttonLayoutAdminAddFoodRemoveSubOption);

        btnRemoveSubOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                container.removeView(subOptionView);
                subOptions.remove(subOption);
            }
        });

        container.addView(subOptionView);
    }

    private void openFilePicker(ActivityResultLauncher<Intent> launcher, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        foodImageLauncher.launch(intent);
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        foodOptions = new ArrayList<>();
        preferenceManager = new PreferenceManager(getApplicationContext());
        wantModelUpload = false;
        generatedFoodId = new String[1];
        addCategory = false;
    }

    private void initActivityResultLaunchers() {
        foodImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        foodImageUri = result.getData().getData();
                        loadImageIntoView(foodImageUri, binding.imageAdminAddFoodFoodThumbnail);
                    }
                });
        objFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        objFileUri = result.getData().getData();

                        String mimeType = getApplicationContext().getContentResolver().getType(objFileUri);

                        if (mimeType != null && (mimeType.equals("model/obj") || mimeType.equals("application/octet-stream") || isObjFile(objFileUri))) {
                            binding.textAdminAddFoodObjFileName.setText(getFileName(objFileUri));
                            binding.textAdminAddFoodObjFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminAddFoodObjFileName.setText("Invalid file type. Please select an OBJ file");
                            binding.textAdminAddFoodObjFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
                            objFileUri = null;
                        }
                    }
                });
        mtlFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        mtlFileUri = result.getData().getData();

                        if (isMtlFile(mtlFileUri)) {
                            binding.textAdminAddFoodMtlFileName.setText(getFileName(mtlFileUri));
                            binding.textAdminAddFoodMtlFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminAddFoodMtlFileName.setText("Invalid file type. Please select and MTL file");
                            binding.textAdminAddFoodMtlFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
                            mtlFileUri = null;
                        }
                    }
                });
        jpgFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        jpgFileUri = result.getData().getData();

                        String mimeType = getApplicationContext().getContentResolver().getType(jpgFileUri);

                        if (mimeType != null && mimeType.equals("image/jpeg")) {
                            binding.textAdminAddFoodJpgFileName.setText(getFileName(jpgFileUri));
                            binding.textAdminAddFoodJpgFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminAddFoodJpgFileName.setText("Invalid file type. Please select a JPEG image.");
                            binding.textAdminAddFoodJpgFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
                            jpgFileUri = null;
                        }
                    }
                });
    }

    private boolean isObjFile(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName != null) {
            return fileName.toLowerCase(Locale.ROOT).endsWith(".obj");
        }
        return false;
    }

    private boolean isMtlFile(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName != null) {
            return fileName.toLowerCase(Locale.ROOT).endsWith(".mtl");
        }
        return false;
    }

    private void loadImageIntoView(Uri imageUri, ImageView imageView) {
        Glide.with(AdminAddFoodActivity.this).load(imageUri).into(imageView);
    }

    private String getFileName(Uri uri) {
        String fileName = "Unnamed File";
        if (uri != null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf("/");
                if (cut != -1) {
                    fileName = path.substring(cut + 1);
                }
            }
        }
        return fileName;
    }

    private static class SubOption {
        String name;
        double price;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage Permission is required to upload files.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}