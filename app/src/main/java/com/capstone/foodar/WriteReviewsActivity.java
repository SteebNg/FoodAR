package com.capstone.foodar;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityWriteReviewsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class WriteReviewsActivity extends AppCompatActivity {

    ActivityWriteReviewsBinding binding;
    private StorageReference storageRef;
    private String foodId;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWriteReviewsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        getFoodImage();
        setListeners();
    }

    private void setListeners() {
        binding.imageWriteReviewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.buttonWriteReviewSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> foodRating = new HashMap<>();
                foodRating.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                foodRating.put(Constants.KEY_FOODS_RATING, binding.ratingWriteReview.getRating());
                foodRating.put(Constants.KEY_COMMENT, binding.etWriteReviewComment.getText().toString());
                foodRating.put(Constants.KEY_FOOD_ID, foodId);
                foodRating.put(Constants.KEY_TIMESTAMP, Timestamp.now());

                db.collection(Constants.KEY_REVIEWS).add(foodRating)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                calculateAverageRatingOnDb();
                            }
                        });
            }
        });
        binding.ratingWriteReview.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (rating < 1.0f) {
                    ratingBar.setRating(1.0f);
                }
            }
        });
    }

    private void calculateAverageRatingOnDb() {
        db.collection(Constants.KEY_REVIEWS)
                .whereEqualTo(Constants.KEY_FOOD_ID, foodId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            double totalRating = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                totalRating += document.getDouble(Constants.KEY_FOODS_RATING);
                            }
                            double averageRating = totalRating / task.getResult().size();
                            updateAverageRatingToDb(averageRating);
                        }
                    }
                });
    }

    private void updateAverageRatingToDb(double averageRating) {
        db.collection(Constants.KEY_FOODS)
                .document(foodId)
                .update(Constants.KEY_FOODS_RATING, averageRating)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(WriteReviewsActivity.this, "Review Added", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void getFoodImage() {
        storageRef.child(Constants.KEY_FOODS
                + "/"
                + foodId
                + "/"
                + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(WriteReviewsActivity.this).load(uri).into(binding.imageWriteReviewFoodImage);
                    }
                });
    }

    private void init() {
        storageRef = FirebaseStorage.getInstance().getReference();
        foodId = getIntent().getStringExtra(Constants.KEY_FOOD_ID);
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }
}