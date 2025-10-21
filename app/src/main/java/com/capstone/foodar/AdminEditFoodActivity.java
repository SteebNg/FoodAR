package com.capstone.foodar;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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
import com.capstone.foodar.Model.FoodOption;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.Utility.DecimalDigitsInputFilter;
import com.capstone.foodar.databinding.ActivityAdminEditFoodBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminEditFoodActivity extends AppCompatActivity {
    
    ActivityAdminEditFoodBinding binding;
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
    private ArrayList<AdminEditFoodActivity.SubOption> subOptions;
    private PopupMenu popupMenu;
    private PreferenceManager preferenceManager;
    private ArrayList<String> foodCategories;
    private String currentFoodCategory;
    private boolean wantModelUpload;
    private boolean addCategory;
    private String foodIdFromPreviousActivity;
    private ArrayList<String> locations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminEditFoodBinding.inflate(getLayoutInflater());
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
        getFoodDetails();
        setListeners();
        checkForStoragePermission();
    }

    private void getFoodDetails() {
        foodIdFromPreviousActivity = getIntent().getStringExtra(Constants.KEY_FOOD_ID);

        db.collection(Constants.KEY_FOODS).document(foodIdFromPreviousActivity)
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
                        binding.etAdminEditFoodFoodName.setText(document.getString(Constants.KEY_FOOD_NAME));
                        binding.etAdminEditFoodFoodDescription.setText(document.getString(Constants.KEY_FOOD_DESC));
                        binding.etAdminEditFoodFoodPrice.setText(
                                String.format(Locale.ROOT, "%.2f", document.getDouble(Constants.KEY_FOOD_PRICE)));
                        binding.textAdminEditFoodFoodCategory.setText(document.getString(Constants.KEY_FOOD_CATEGORY));

                        List<Object> rawLocations = (List<Object>) document.get(Constants.KEY_LOCATION_ID);

                        if (rawLocations != null) {
                            for (Object location : rawLocations) {
                                if (location instanceof String) {
                                    locations.add((String) location);
                                }
                            }
                        }

                        getFoodOptions();
                        getFoodImage();
                        getObj();
                    }
                });
    }

    private void getFoodImage() {
        storageRef.child(Constants.KEY_FOODS
                + "/"
                + foodIdFromPreviousActivity
                + "/"
                + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(AdminEditFoodActivity.this)
                                .load(uri).into(binding.imageAdminEditFoodFoodThumbnail);
                        foodImageUri = uri;
                    }
                });
    }

    private void getObj() {
        storageRef.child(Constants.KEY_FOODS + "/" + foodIdFromPreviousActivity + "/3DModel.obj")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        binding.textAdminEditFoodObjFileName.setText("(Exists)");
                        objFileUri = uri;
                        getMtl();
                    }
                });
    }

    private void getMtl() {
        storageRef.child(Constants.KEY_FOODS + "/" + foodIdFromPreviousActivity + "/3DModel.mtl")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        binding.textAdminEditFoodMtlFileName.setText("(Exists)");
                        mtlFileUri = uri;
                        getJpg();
                    }
                });
    }

    private void getJpg() {
        storageRef.child(Constants.KEY_FOODS + "/" + foodIdFromPreviousActivity + "/3DModel.jpg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        binding.textAdminEditFoodJpgFileName.setText("(Exists)");
                        jpgFileUri = uri;
                    }
                });
    }

    private void getFoodOptions() {
        db.collection(Constants.KEY_FOOD_OPTIONS).document(foodIdFromPreviousActivity)
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

                        Map<String, Object> fields = document.getData();

                        if (fields != null && !fields.isEmpty()) {
                            for (Map.Entry<String, Object> option : fields.entrySet()) {
                                FoodOption foodOption = new FoodOption();
                                foodOption.individualOptions = new HashMap<>();

                                foodOption.optionTitle = option.getKey();
                                Map<String, Object> indiOptions = (Map<String, Object>) option.getValue();

                                Object foodOptionsFromDb = indiOptions.get(Constants.KEY_FOOD_INDIVIDUAL_OPTIONS);
                                if (!(foodOptionsFromDb instanceof Map)) {
                                    return;
                                }

                                Map<String, Object> foodOptionsMap = (Map<String, Object>) foodOptionsFromDb;

                                for (Map.Entry<String, Object> entry : foodOptionsMap.entrySet()) {
                                    String indiOptionName = entry.getKey();

                                    double indiOptionPrice;
                                    Object price = entry.getValue();
                                    if (price instanceof Long) {
                                        long primitiveLongValue = (long) price;
                                        indiOptionPrice = (double) primitiveLongValue;
                                    } else {
                                        indiOptionPrice = (double) price;
                                    }

                                    foodOption.individualOptions.put(indiOptionName, indiOptionPrice);
                                }
                                foodOptions.add(foodOption);
                            }
                            setFoodOption();
                        }
                    }
                });
    }

    private void setFoodOption() {
        for (FoodOption option : foodOptions) {
            View optionView = LayoutInflater.from(this)
                    .inflate(R.layout.layout_admin_add_food_item_food_option
                            , binding.layoutAdminEditFoodOptionsContainer
                            , false);

            TextInputEditText etOptionName = optionView.findViewById(R.id.etLayoutAdminAddFoodOptionName);
            ImageButton btnRemoveOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodRemoveOption);
            LinearLayout layoutSubOptionsContainer = optionView.findViewById(R.id.layoutAdminAddFoodSubOptionsContainer);
            Button btnAddSubOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodAddSubOption);

            etOptionName.setText(option.optionTitle);

            Map<String, Double> indiOptions = option.individualOptions;
            for (Map.Entry<String, Double> entry : indiOptions.entrySet()) {
                SubOption subOption = new SubOption();
                subOption.name = entry.getKey();
                subOption.price = entry.getValue();

                subOptions.add(subOption);
            }

            btnRemoveOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    binding.layoutAdminEditFoodOptionsContainer.removeView(optionView);
                    foodOptions.remove(option);
                }
            });

            btnAddSubOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addNewSubOption(layoutSubOptionsContainer, option);
                }
            });

            setSubOptions(layoutSubOptionsContainer, option);
            binding.layoutAdminEditFoodOptionsContainer.addView(optionView);
        }
    }

    private void setSubOptions(LinearLayout container, FoodOption foodOption) {
        Map<String, Double> indiOptions = foodOption.individualOptions;
        ArrayList<SubOption> tempSubOptions = new ArrayList<>();
        for (Map.Entry<String, Double> indiOption : indiOptions.entrySet()) {
            SubOption subOption = new SubOption();
            subOption.name = indiOption.getKey();
            subOption.price = indiOption.getValue();
            subOptions.add(subOption);
            tempSubOptions.add(subOption);
        }

        for (SubOption option : tempSubOptions) {
            View subOptionView = LayoutInflater.from(this).inflate(R.layout.layout_admin_add_food_item_sub_option
                    , container
                    , false);

            TextInputEditText etSubOptionName = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionName);
            TextInputEditText etSubOptionPrice = subOptionView.findViewById(R.id.etLayoutAdminAddFoodSubOptionPrice);
            ImageButton btnRemoveSubOption = subOptionView.findViewById(R.id.buttonLayoutAdminAddFoodRemoveSubOption);

            etSubOptionName.setText(option.name);
            etSubOptionPrice.setText(String.format(Locale.ROOT, "%.2f", option.price));

            btnRemoveSubOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    container.removeView(subOptionView);
                    subOptions.remove(option);
                }
            });

            container.addView(subOptionView);
        }
    }

    private void checkForStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void setListeners() {
        binding.buttonAdminEditFoodUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImagePicker();
            }
        });
        binding.buttonAdminEditFoodUploadObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(objFileLauncher, "*/*");
            }
        });
        binding.buttonAdminEditFoodUploadMtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(mtlFileLauncher, "*/*");
            }
        });
        binding.buttonAdminEditFoodUploadJpg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker(jpgFileLauncher, "image/*");
            }
        });
        binding.buttonAdminEditFoodAddOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewOption();
            }
        });
        binding.buttonAdminEditFoodSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitToDb();
                isLoading(true);
            }
        });
        binding.buttonAdminEditFoodAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!addCategory) {
                    binding.imageAdminEditFoodHideCategory.setVisibility(View.VISIBLE);
                    binding.imageAdminEditFoodAddCategory.setVisibility(View.GONE);
                    binding.layoutAdminEditFoodAddFoodCategory.setVisibility(View.VISIBLE);
                    addCategory = true;
                } else {
                    binding.imageAdminEditFoodHideCategory.setVisibility(View.GONE);
                    binding.imageAdminEditFoodAddCategory.setVisibility(View.VISIBLE);
                    binding.layoutAdminEditFoodAddFoodCategory.setVisibility(View.GONE);
                    addCategory = false;
                }
            }
        });
        binding.buttonAdminEditFoodAddCategoryConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String categoryToBeAdded = binding.etAdminEditFoodAddCategory.getText().toString().trim();

                if (categoryToBeAdded.isEmpty()) {
                    Toast.makeText(AdminEditFoodActivity.this, "Please enter a food category.", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog alertDialog = getAlertDialog(categoryToBeAdded);
                alertDialog.show();
            }
        });
        binding.buttonAdminEditFoodFoodCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
        binding.buttonAdminEditFoodBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.etAdminEditFoodFoodPrice.addTextChangedListener(
                new DecimalDigitsInputFilter(2, binding.etAdminEditFoodFoodPrice)
        );
    }

    private void isLoading(boolean loading) {
        if (loading) {
            binding.buttonAdminEditFoodSubmit.setVisibility(View.GONE);
            binding.buttonAdminEditFoodSubmit.setEnabled(false);
            binding.progressAdminEditFood.setVisibility(View.VISIBLE);
        } else {
            binding.buttonAdminEditFoodSubmit.setVisibility(View.VISIBLE);
            binding.buttonAdminEditFoodSubmit.setEnabled(true);
            binding.progressAdminEditFood.setVisibility(View.GONE);
        }
    }

    private AlertDialog getAlertDialog(String categoryToBeAdded) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AdminEditFoodActivity.this);
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

                        binding.imageAdminEditFoodHideCategory.setVisibility(View.GONE);
                        binding.imageAdminEditFoodAddCategory.setVisibility(View.VISIBLE);
                        binding.layoutAdminEditFoodAddFoodCategory.setVisibility(View.GONE);
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
        popupMenu = new PopupMenu(AdminEditFoodActivity.this, binding.buttonAdminEditFoodFoodCategory);
        popupMenu.getMenuInflater().inflate(R.menu.menu_food_categories, popupMenu.getMenu());

        binding.textAdminEditFoodFoodCategory.setText(foodCategories.get(0));
        currentFoodCategory = foodCategories.get(0);

        for (String categories : foodCategories) {
            popupMenu.getMenu().add(categories);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                currentFoodCategory = (String) item.getTitle();
                binding.textAdminEditFoodFoodCategory.setText(currentFoodCategory);
                return true;
            }
        });
    }

    private void submitToDb() {
        String foodName = binding.etAdminEditFoodFoodName.getText().toString().trim();
        String foodDesc = binding.etAdminEditFoodFoodDescription.getText().toString().trim();
        String foodPrice = binding.etAdminEditFoodFoodPrice.getText().toString().trim();

        if (foodName.isEmpty()) {
            binding.etAdminEditFoodFoodName.setError("Food name is required");
            binding.etAdminEditFoodFoodName.requestFocus();
            isLoading(false);
            return;
        }

        if (foodDesc.isEmpty()) {
            binding.etAdminEditFoodFoodDescription.setError("Food description is required");
            binding.etAdminEditFoodFoodDescription.requestFocus();
            isLoading(false);
            return;
        }

        if (foodPrice.isEmpty()) {
            binding.etAdminEditFoodFoodPrice.setError("Food price is required");
            binding.etAdminEditFoodFoodPrice.requestFocus();
            isLoading(false);
            return;
        }

        if (foodImageUri == null) {
            Toast.makeText(this, "Please upload a food image", Toast.LENGTH_SHORT).show();
            isLoading(false);
            return;
        }

        if (objFileUri != null) {
            if (mtlFileUri == null || jpgFileUri == null) {
                Snackbar snackbar = Snackbar.make(binding.main
                        , "OBJ file requires MTL and JPG file to be uploaded too."
                        , Snackbar.LENGTH_LONG);
                snackbar.show();
                isLoading(false);
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
            food.put(Constants.KEY_LOCATION_ID, locations);

            updateFood(food);
        }
    }

    private void updateFood(Map<String, Object> food) {
        db.collection(Constants.KEY_FOODS).document(foodIdFromPreviousActivity).set(food)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Overwrite on Firestore", "Successful");
                        updateFoodOptions(food);
                    }
                });
    }

    private void updateFoodOptions(Map<String, Object> food) {
        Map<String, Object> foodOptionsToBeSentToDb = new HashMap<>();
        for (FoodOption option : foodOptions) {
            foodOptionsToBeSentToDb.put(option.optionTitle, option.individualOptions);
        }

        db.collection(Constants.KEY_FOOD_OPTIONS).document(foodIdFromPreviousActivity).set(foodOptionsToBeSentToDb)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Upload Food Option", "Uploaded Successfully.");
                        updateFoodImage();
                    }
                });
    }

    private void updateFoodImage() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + foodIdFromPreviousActivity
                        + "/"
                        + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .putFile(foodImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Food Image Upload", "Successfully Uploaded");
                    }
                });

        if (wantModelUpload) {
            uploadObjFileToDb();
        } else {
            Toast.makeText(this, "Item Updated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void uploadObjFileToDb() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + foodIdFromPreviousActivity
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
                        + foodIdFromPreviousActivity
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
                        + foodIdFromPreviousActivity
                        + "/3DModel.jpg")
                .putFile(jpgFileUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d("Upload JPG File", "Upload Successfully");
                        Toast.makeText(AdminEditFoodActivity.this, "Item Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private boolean collectOptionsData() {
        boolean isSuccessful = binding.layoutAdminEditFoodOptionsContainer.getChildCount() < 1;

        for (int i = 0; i < binding.layoutAdminEditFoodOptionsContainer.getChildCount(); i++) {
            View optionView = binding.layoutAdminEditFoodOptionsContainer.getChildAt(i);
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

            for (Map.Entry<String, Double> entry : foodOption.individualOptions.entrySet()) {
                SubOption subOptionToRemove = new SubOption();
                subOptionToRemove.name = entry.getKey();
                subOptionToRemove.price = entry.getValue();

                subOptions.remove(subOptionToRemove);
            }
        }
        return isSuccessful;
    }

    private void addNewOption() {
        FoodOption foodOption = new FoodOption();
        foodOptions.add(foodOption);

        View optionView = LayoutInflater.from(this)
                .inflate(R.layout.layout_admin_add_food_item_food_option
                        , binding.layoutAdminEditFoodOptionsContainer
                        , false);

        TextInputEditText etOptionName = optionView.findViewById(R.id.etLayoutAdminAddFoodOptionName);
        ImageButton btnRemoveOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodRemoveOption);
        LinearLayout layoutSubOptionsContainer = optionView.findViewById(R.id.layoutAdminAddFoodSubOptionsContainer);
        Button btnAddSubOption = optionView.findViewById(R.id.buttonLayoutAdminAddFoodAddSubOption);

        btnRemoveOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.layoutAdminEditFoodOptionsContainer.removeView(optionView);
                foodOptions.remove(foodOption);
            }
        });

        btnAddSubOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewSubOption(layoutSubOptionsContainer, foodOption);
            }
        });

        binding.layoutAdminEditFoodOptionsContainer.addView(optionView);
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

        etSubOptionPrice.addTextChangedListener(new DecimalDigitsInputFilter(2, etSubOptionPrice));

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
        addCategory = false;
        locations = new ArrayList<>();
        subOptions = new ArrayList<>();
    }

    private void initActivityResultLaunchers() {
        foodImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        foodImageUri = result.getData().getData();
                        loadImageIntoView(foodImageUri, binding.imageAdminEditFoodFoodThumbnail);
                    }
                });
        objFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        objFileUri = result.getData().getData();

                        String mimeType = getApplicationContext().getContentResolver().getType(objFileUri);

                        if (mimeType != null && isObjFile(objFileUri)) {
                            binding.textAdminEditFoodObjFileName.setText(getFileName(objFileUri));
                            binding.textAdminEditFoodObjFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminEditFoodObjFileName.setText("Invalid file type. Please select an OBJ file");
                            binding.textAdminEditFoodObjFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
                            objFileUri = null;
                        }
                    }
                });
        mtlFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        mtlFileUri = result.getData().getData();

                        if (isMtlFile(mtlFileUri)) {
                            binding.textAdminEditFoodMtlFileName.setText(getFileName(mtlFileUri));
                            binding.textAdminEditFoodMtlFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminEditFoodMtlFileName.setText("Invalid file type. Please select and MTL file");
                            binding.textAdminEditFoodMtlFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
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
                            binding.textAdminEditFoodJpgFileName.setText(getFileName(jpgFileUri));
                            binding.textAdminEditFoodJpgFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.primary_text));
                        } else {
                            binding.textAdminEditFoodJpgFileName.setText("Invalid file type. Please select a JPEG image.");
                            binding.textAdminEditFoodJpgFileName.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.hintColor));
                            jpgFileUri = null;
                        }
                    }
                });
    }

    private boolean isObjFile(Uri uri) {
        String fileName = getFileName(uri);
        return fileName.toLowerCase(Locale.ROOT).endsWith(".obj");
    }

    private boolean isMtlFile(Uri uri) {
        String fileName = getFileName(uri);
        return fileName.toLowerCase(Locale.ROOT).endsWith(".mtl");
    }

    private void loadImageIntoView(Uri imageUri, ImageView imageView) {
        Glide.with(AdminEditFoodActivity.this).load(imageUri).into(imageView);
    }

//    private String getFileName(Uri uri) {
//        String fileName = "Unnamed File";
//        if (uri != null) {
//            String path = uri.getPath();
//            if (path != null) {
//                int cut = path.lastIndexOf("/");
//                if (cut != -1) {
//                    fileName = path.substring(cut + 1);
//                }
//            }
//        }
//        return fileName;
//    }

    private String getFileName(Uri uri) {
        if (uri == null) {
            return "Unnamed File";
        }

        String result = null;

        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = getContentResolver();

            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("ContentResolver", "Failed");
            }
        }

        if (result == null && uri.getScheme().equals("file")) {
            result = uri.getLastPathSegment();
        }

        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf("/");
                if (cut != -1) {
                    result = path.substring(cut + 1);
                }
            }
        }

        return (result != null) ? result : "Unnamed File";
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