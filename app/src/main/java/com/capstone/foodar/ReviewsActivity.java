package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.ReviewListAdapter;
import com.capstone.foodar.Model.Review;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityReviewsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Locale;

public class ReviewsActivity extends AppCompatActivity {

    // TODO: Make sure this activity can be assessed from user's history too

    ActivityReviewsBinding binding;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private String foodId;
    private PreferenceManager preferenceManager;
    private ArrayList<Review> reviews;
    private ReviewListAdapter reviewListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReviewsBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        checkUserOrdered();
        setListeners();
        getReviewsFromDb();
        calculateRating();
    }

    private void calculateRating() {
        if (reviews.isEmpty()) {
            return;
        }

        int[] ratings = {0, 0, 0, 0, 0};
        double totalRating = 0;

        for (Review review : reviews) {
            double reviewRating = review.rating;
            totalRating += reviewRating;

            int roundedRating = (int) Math.round(reviewRating);

            switch (roundedRating) {
                case 1:
                    ratings[0]++;
                    break;
                case 2:
                    ratings[1]++;
                    break;
                case 3:
                    ratings[2]++;
                    break;
                case 4:
                    ratings[3]++;
                    break;
                case 5:
                    ratings[4]++;
                    break;
            }
        }

        double averageRating = totalRating / reviews.size();
        setCalculatedRating(averageRating, ratings);
    }

    private void setCalculatedRating(double averageRating, int[] ratings) {
        String formattedAverageRating = String.format(Locale.getDefault(), ".1f", averageRating);
        binding.textReviewFoodOverallRating.setText(formattedAverageRating);
        binding.textReviewFoodOverallNumberOfReviews.setText(String.valueOf(reviews.size()));

        binding.progressBarReview1Star.setProgress((ratings[0] / reviews.size()) * 100);
        binding.progressBarReview2Star.setProgress((ratings[1] / reviews.size()) * 100);
        binding.progressBarReview3Star.setProgress((ratings[2] / reviews.size()) * 100);
        binding.progressBarReview4Star.setProgress((ratings[3] / reviews.size()) * 100);
        binding.progressBarReview5Star.setProgress((ratings[4] / reviews.size()) * 100);
    }

    private void getReviewsFromDb() {
        db.collection(Constants.KEY_REVIEWS)
                .whereEqualTo(Constants.KEY_FOOD_ID, foodId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Review review = new Review();
                                review.rating = Math.toIntExact(document.getLong(Constants.KEY_FOODS_RATING));
                                review.comment = document.getString(Constants.KEY_COMMENT);
                                review.userId = document.getString(Constants.KEY_USER_ID);
                                review.timestamp = document.getTimestamp(Constants.KEY_TIMESTAMP);
                                review.userName = document.getString(Constants.KEY_USERNAME);

                                getFoodImage(review);
                            }
                        }
                    }
                });
    }

    private void getFoodImage(Review review) {
        storageRef.child(Constants.KEY_FOODS
                + "/"
                + foodId
                + "/"
                + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        review.profileImage = uri;
                        reviews.add(review);
                        setReviewRecycler();
                    }
                });
    }

    private void setReviewRecycler() {
        reviewListAdapter = getReviewListAdapter();
        binding.recyclerReview.setAdapter(reviewListAdapter);
        reviewListAdapter.notifyDataSetChanged();
    }

    private ReviewListAdapter getReviewListAdapter() {
        return new ReviewListAdapter(reviews, ReviewsActivity.this); // Might need to change to application context
    }


    private void setListeners() {
        binding.buttonReviewWriteReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ReviewsActivity.this, WriteReviewsActivity.class);
                intent.putExtra(Constants.KEY_FOOD_ID, foodId);
                startActivity(intent);
            }
        });
    }

    private void checkUserOrdered() {
        db.collection(Constants.KEY_ORDER_HISTORY)
                .whereEqualTo(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_FOOD_ID, foodId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult().isEmpty()) {
                            binding.layoutReviewButtonWriteReview.setVisibility(View.INVISIBLE);
                            binding.buttonReviewWriteReview.setEnabled(false);
                        } else {
                            binding.layoutReviewButtonWriteReview.setVisibility(View.VISIBLE);
                            binding.buttonReviewWriteReview.setEnabled(true);
                        }
                    }
                });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        foodId = getIntent().getStringExtra(Constants.KEY_FOOD_ID);
        preferenceManager = new PreferenceManager(getApplicationContext());
        reviews = new ArrayList<>();
    }
}