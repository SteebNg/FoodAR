package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Adapter.FoodDetailsOptionListAdapter;
import com.capstone.foodar.Adapter.FoodDetailsReviewListAdapter;
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.Model.FoodOption;
import com.capstone.foodar.Model.Review;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityFoodDetailsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.rejowan.cutetoast.CuteToast;
import com.unity3d.player.UnityPlayerGameActivity;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoodDetailsActivity extends AppCompatActivity {

    private ActivityFoodDetailsBinding binding;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private String foodId;
    private Food food;
    private ArrayList<Review> reviews;
    private ArrayList<FoodOption> foodOptions;
    private FoodDetailsReviewListAdapter reviewListAdapter;
    private FoodDetailsOptionListAdapter optionListAdapter;
    private PreferenceManager preferenceManager;
    private static DecimalFormat priceFormat;

    // for checkout activity
    private String cartId;
    private int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFoodDetailsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
        setFoodDetails();
        checkIf3DModelExists();
        setReviews();
        setOptions();
    }

//    private void checkIf3DModelExists() {
//        storageRef.child(Constants.KEY_FOODS
//                + "/"
//                + foodId
//                + "/"
//                + "3DModel.obj")
//                .getDownloadUrl()
//                .addOnSuccessListener(new OnSuccessListener<Uri>() {
//                    @Override
//                    public void onSuccess(Uri uri) {
//                        int arAvailableDrawable = R.drawable.video_camera_back_24;
//
//                        binding.buttonFoodDetailsAR.setEnabled(true);
//                        Glide.with(FoodDetailsActivity.this).load(arAvailableDrawable).into(binding.imageFoodDetailsAr);
//                        binding.bgFoodDetailsAr.setBackgroundTintList(
//                                ContextCompat.getColorStateList(FoodDetailsActivity.this, R.color.lightGreen)
//                        );
//                    }
//                });
//    }

    private void checkIf3DModelExists() {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + foodId
                        + "/")
                .listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        boolean objExists = false;
                        boolean mtlExists = false;
                        boolean jpgExists = false;

                        for (StorageReference file : listResult.getItems()) {
                            String fileName = file.getName();

                            switch (fileName) {
                                case "3DModel.obj":
                                    objExists = true;
                                    break;
                                case "3DModel.mtl":
                                    mtlExists = true;
                                    break;
                                case "3DModel.jpg":
                                    jpgExists = true;
                                    break;
                            }

                            if (objExists && mtlExists && jpgExists) {
                                int arAvailableDrawable = R.drawable.video_camera_back_24;

                                binding.buttonFoodDetailsAR.setEnabled(true);
                                Glide.with(FoodDetailsActivity.this).load(arAvailableDrawable).into(binding.imageFoodDetailsAr);
                                binding.bgFoodDetailsAr.setBackgroundTintList(
                                        ContextCompat.getColorStateList(FoodDetailsActivity.this, R.color.lightGreen));

                                break;
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Check 3D Model Exists in DB", "Failed: " + e);
                    }
                });
    }

    private void setOptions() {
        db.collection(Constants.KEY_FOOD_OPTIONS)
                .document(foodId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                putOptionsInRecycler(document);
                            } else {
                                binding.recyclerFoodDetailFoodOption.setVisibility(View.INVISIBLE);
                                binding.dividerFoodDetails2.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                });
    }

    private void putOptionsInRecycler(DocumentSnapshot document) {
        Map<String, Object> fields = document.getData();

        if (!fields.isEmpty()) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                FoodOption foodOption = new FoodOption();
                Map<String, Object> option = (Map<String, Object>) entry.getValue();

                foodOption.optionTitle = entry.getKey();

                Object individualOptionsObject = option.get(Constants.KEY_FOOD_INDIVIDUAL_OPTIONS);
                if (individualOptionsObject instanceof Map) {
                    foodOption.individualOptions = getDoubleMap((Map<?, ?>) individualOptionsObject);
                }

                foodOptions.add(foodOption);
            }
            setFoodOptionRecycler();
        }
    }

    private static @NonNull Map<String, Double> getDoubleMap(Map<?, ?> individualOptionsObject) {
        Map<?, ?> rawMap = individualOptionsObject;
        Map<String, Double> convertedMap = new HashMap<>();

        for (Map.Entry<?, ?> mapEntry : rawMap.entrySet()) {
            if (mapEntry.getKey() instanceof String) {
                String key = (String) mapEntry.getKey();
                Object value = mapEntry.getValue();

                if (value instanceof Long) {
                    convertedMap.put(key, ((Long) value).doubleValue());
                } else if (value instanceof Double) {
                    convertedMap.put(key, (Double) value);
                }
            }
        }
        return convertedMap;
    }

    private void setFoodOptionRecycler() {
        optionListAdapter = getOptionListAdapter();
        binding.recyclerFoodDetailFoodOption.setAdapter(optionListAdapter);
        optionListAdapter.notifyDataSetChanged();
        optionListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                calculateTotalPrice();
                optionListAdapter.unregisterAdapterDataObserver(this);
            }
        });
    }

    private FoodDetailsOptionListAdapter getOptionListAdapter() {
        FoodDetailsOptionListAdapter adapter = new FoodDetailsOptionListAdapter(foodOptions, getApplicationContext());

        adapter.setOnSelectedChangeListener(new FoodDetailsOptionListAdapter.OnSelectedChangeListener() {
            @Override
            public void onChange(String optionName) {
                calculateTotalPrice();
            }
        });

        return adapter;
    }

    private void setReviews() {
        db.collection(Constants.KEY_REVIEWS)
                .whereEqualTo(Constants.KEY_FOOD_ID, foodId)
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Firebase Get Reviews", "Failed: " + task.getException());
                            return;
                        }
                        QuerySnapshot document = task.getResult();
                        if (!document.isEmpty()) {
                            putReviewsInRecycler(document);
                        } else {
                            binding.recyclerFoodDetailsReviews.setVisibility(View.INVISIBLE);
                            binding.textFoodDetailsNoReviews.setVisibility(View.VISIBLE);
                            binding.textFoodDetailsReviewMore.setVisibility(View.INVISIBLE);
                            binding.textFoodDetailsReviewMore.setEnabled(false);
                        }
                    }
                });
    }

    private void putReviewsInRecycler(QuerySnapshot documentSnapshots) {
        final int[] reviewWentThrough = {0};
        for (QueryDocumentSnapshot document : documentSnapshots) {
            Review review = new Review();
            review.comment = document.getString(Constants.KEY_COMMENT);
            review.rating = document.getDouble(Constants.KEY_FOODS_RATING);
            review.timestamp = convertFirebaseTimestamp(Objects.requireNonNull(document.getTimestamp(Constants.KEY_TIMESTAMP)));
            review.userId = document.getString(Constants.KEY_USER_ID);
            review.userName = document.getString(Constants.KEY_USERNAME);

            storageRef.child(Constants.KEY_USERS_LIST
                    + "/"
                    + review.userId
                    + "/"
                    + Constants.KEY_PROFILE_IMAGE)
                    .getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            review.profileImage = uri;
                            reviews.add(review);
                            reviewWentThrough[0]++;

                            if (reviewWentThrough[0] == documentSnapshots.size()) {
                                setReviewRecycler();
                            }
                        }
                    });
        }
    }

    private Timestamp convertFirebaseTimestamp(com.google.firebase.Timestamp firebaseTime) {
        long seconds = firebaseTime.getSeconds();
        long nanoSeconds = firebaseTime.getNanoseconds();

        long milliseconds = seconds * 1000;

        long totalMilliseconds = milliseconds + (nanoSeconds / 1000000);

        return new Timestamp(totalMilliseconds);
    }

    private void setReviewRecycler() {
        reviewListAdapter = getReviewAdapter();
        binding.recyclerFoodDetailsReviews.setAdapter(reviewListAdapter);
        reviewListAdapter.notifyDataSetChanged();
    }

    private FoodDetailsReviewListAdapter getReviewAdapter() {
        // any internal edits to the adapter just do it here
        return new FoodDetailsReviewListAdapter(reviews, getApplicationContext());
    }

    private void setListeners() {
        binding.buttonFoodDetailMinusQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setQuantity(-1);
            }
        });
        binding.buttonFoodDetailPlusQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setQuantity(1);
            }
        });
        binding.textFoodDetailsReviewMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FoodDetailsActivity.this, ReviewsActivity.class);
                intent.putExtra(Constants.KEY_FOOD_ID, foodId);
                startActivity(intent);
            }
        });
        binding.buttonFoodDetailAddToCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> individualFoodOrder = new HashMap<>();
                individualFoodOrder.put(Constants.KEY_FOOD_ID, foodId);
                individualFoodOrder.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                individualFoodOrder.put(Constants.KEY_REMARKS, binding.etFoodDetailsRemark.getText().toString().trim());
                individualFoodOrder.put(Constants.KEY_FOOD_OPTIONS, getFoodOptions());
                individualFoodOrder.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                individualFoodOrder.put(Constants.KEY_FOOD_AMOUNT, Integer.parseInt(binding.textFoodDetailsQuantity.getText().toString()));
                individualFoodOrder.put(Constants.KEY_FOOD_NAME, food.foodName);

                String sTotalPrice = binding.textFoodDetailsPriceAmount.getText().toString();
                String sExtractedPrice = sTotalPrice.replaceAll("[^\\d.-]", "");

                individualFoodOrder.put(Constants.KEY_ORDER_PRICE, Double.parseDouble(sExtractedPrice));

                db.collection(Constants.KEY_CARTS)
                        .add(individualFoodOrder)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
//                                Toast.makeText(FoodDetailsActivity.this, "Order Added", Toast.LENGTH_SHORT).show();
                                CuteToast.ct(FoodDetailsActivity.this, "Order Added to Cart", CuteToast.LENGTH_LONG,
                                        CuteToast.SUCCESS, true).show();
                                if (cartId != null) {
                                    Intent intent = new Intent();
                                    intent.putExtra(Constants.KEY_CART_ID, cartId);
                                    intent.putExtra(Constants.KEY_POSITION, pos);
                                    setResult(RESULT_OK, intent);
                                }
                                finish();
                            }
                        });
            }
        });
        binding.buttonFoodDetailsAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FoodDetailsActivity.this, UnityPlayerGameActivity.class);
                intent.putExtra("result", foodId);
                startActivity(intent);
            }
        });
    }

    private ArrayList<String> getFoodOptions() {
        ArrayList<String> options = new ArrayList<>();
        if (foodOptions != null && !foodOptions.isEmpty()) {
            RecyclerView recyclerView = binding.recyclerFoodDetailFoodOption;

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View view = recyclerView.getChildAt(i);
                RadioGroup radioGroup = view.findViewById(R.id.radioGroupFoodOptionItem);

                int checkedId = radioGroup.getCheckedRadioButtonId();

                if (checkedId != -1) {
                    RadioButton selectedRadioButton = view.findViewById(checkedId);
                    String radioButtonText = extractOptionName(selectedRadioButton.getText().toString());
                    options.add(radioButtonText);
                }
            }
        }
        return options;
    }

    private String extractOptionName(String optionName) {
        String[] parts = optionName.split("\\s*[+-]\\s*RM");
        return parts[0].trim();
    }

    private void setQuantity(int changes) {
        String sCurrentQuantity = binding.textFoodDetailsQuantity.getText().toString();
        int currentQuantity = Integer.parseInt(sCurrentQuantity);
        currentQuantity += changes;

        if (currentQuantity < 1) {
            Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
        } else {
            binding.textFoodDetailsQuantity.setText(String.valueOf(currentQuantity));
        }

        calculateTotalPrice();
    }

    private void calculateTotalPrice() {
        double totalPrice = food.foodPrice;

        if (foodOptions != null && !foodOptions.isEmpty()) {
            ArrayList<Double> prices = new ArrayList<>();
            RecyclerView recyclerView = binding.recyclerFoodDetailFoodOption;

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View view = recyclerView.getChildAt(i);
                RadioGroup radioGroup = view.findViewById(R.id.radioGroupFoodOptionItem);

                int checkedId = radioGroup.getCheckedRadioButtonId();

                if (checkedId != -1) {
                    RadioButton selectedRadioButton = view.findViewById(checkedId);
                    double radioButtonPrice = extractCost(selectedRadioButton.getText().toString());
                    prices.add(radioButtonPrice);
                }
            }

            for (double price : prices) {
                totalPrice += price;
            }
        }

        // calculate quantity
        totalPrice *= Integer.parseInt(binding.textFoodDetailsQuantity.getText().toString());
        String formattedPrice = priceFormat.format(totalPrice);

        binding.textFoodDetailsPriceAmount.setText("RM " + formattedPrice);
    }

    private double extractCost(String optionName) {
        Pattern pattern = Pattern.compile("[+-]\\s*RM\\s*(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(optionName);

        if (matcher.find()) {
            String numberAsString = matcher.group(1);
            double cost = Double.parseDouble(numberAsString);
            if (optionName.contains("-RM")) {
                return -cost;
            } else {
                return cost;
            }
        } else {
            return 0;
        }
    }

    private void setFoodDetails() {
        db.collection(Constants.KEY_FOODS)
                .document(foodId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                food.foodCategory = document.getString(Constants.KEY_FOOD_CATEGORY);
                                food.foodName = document.getString(Constants.KEY_FOOD_NAME);
                                food.foodDesc = document.getString(Constants.KEY_FOOD_DESC);
                                food.foodPrice = document.getDouble(Constants.KEY_FOOD_PRICE);

                                binding.textFoodDetailsName.setText(food.foodName);
                                binding.textFoodDetailsDesc.setText(food.foodDesc);
                                binding.textFoodDetailsPriceAmount.setText("RM " + priceFormat.format(food.foodPrice));

                                setFoodImage();
                            }
                        }
                    }
                });
    }

    private void setFoodImage() {
        storageRef.child(Constants.KEY_FOODS
                + "/"
                + foodId
                + "/"
                + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getApplicationContext()).load(uri).into(binding.imageFoodDetails);
                    }
                });
    }

    private void init() {
        storageRef = FirebaseStorage.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        foodId = getIntent().getStringExtra(Constants.KEY_FOOD_ID);
        cartId = getIntent().getStringExtra(Constants.KEY_CART_ID);
        pos = getIntent().getIntExtra(Constants.KEY_POSITION, -1);
        reviews = new ArrayList<>();
        food = new Food();
        foodOptions = new ArrayList<>();
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding.buttonFoodDetailsAR.setEnabled(false);
        priceFormat = new DecimalFormat("0.00");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ResumingFoodDetails", "Resumed");
    }
}