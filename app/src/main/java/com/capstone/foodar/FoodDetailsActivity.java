package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Adapter.FoodDetailsReviewListAdapter;
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.Model.Review;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.databinding.ActivityFoodDetailsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class FoodDetailsActivity extends AppCompatActivity {

    private ActivityFoodDetailsBinding binding;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private String foodId;
    private Food food;
    private ArrayList<Review> reviews;
    private FoodDetailsReviewListAdapter reviewListAdapter;

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
        setFoodDetails();
        setListeners();
        setReviews();
    }

    private void setReviews() {
        db.collection(Constants.KEY_REVIEWS)
                .whereEqualTo(Constants.KEY_FOOD_ID, foodId)
                .limit(2)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot document = task.getResult();
                            if (!document.isEmpty()) {
                                putReviewsInRecycler(document);
                            } else {
                                // TODO
                            }
                        }
                    }
                });
    }

    private void putReviewsInRecycler(QuerySnapshot documentSnapshots) {
        for (QueryDocumentSnapshot document : documentSnapshots) {
            Review review = new Review();
            review.comment = document.getString(Constants.KEY_COMMENT);
            review.rating = Integer.parseInt(document.getString(Constants.KEY_FOODS_RATING));
            review.timestamp = document.getTimestamp(Constants.KEY_TIMESTAMP);

            final int[] reviewWentThrough = {0};
            storageRef.child(Constants.KEY_USERS_LIST
                    + "/"
                    + document.getString(Constants.KEY_USER_ID)
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
                Intent intent = new Intent(FoodDetailsActivity.this, );
                intent.putExtra(Constants.KEY_FOOD_ID, foodId);
                startActivity(intent);
            }
        });
    }

    private void setQuantity(int changes) {
        String sCurrentQuantity = binding.textFoodDetailsQuantity.getText().toString();
        int currentQuantity = Integer.parseInt(sCurrentQuantity);
        currentQuantity += changes;

        if (changes < 1) {
            Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
        } else {
            binding.textFoodDetailsQuantity.setText(String.valueOf(currentQuantity));
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
                                binding.textFoodDetailsPriceAmount.setText(String.valueOf(food.foodPrice));

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
                + Constants.KEY_FOOD_IMAGE)
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
        reviews = new ArrayList<>();
    }
}