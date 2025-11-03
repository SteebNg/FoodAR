package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Model.Review;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class FoodDetailsReviewListAdapter extends RecyclerView.Adapter<FoodDetailsReviewListAdapter.ViewHolder>{

    private ArrayList<Review> reviews;
    private Context context;

    public FoodDetailsReviewListAdapter(ArrayList<Review> reviews, Context context) {
        this.reviews = reviews;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_food_details_review_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.comment.setText(review.comment);
        holder.username.setText(review.userName);
        Glide.with(context).load(review.profileImage).into(holder.profileImage);
        holder.ratingBar.setRating((float) review.rating);
    }

    @Override
    public int getItemCount() {
        return reviews.size(); // Max reviews
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage;
        RatingBar ratingBar;
        TextView comment, username;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.layoutFoodDetailsItemProfilePic);
            ratingBar = itemView.findViewById(R.id.ratingReviewFoodDetailsItem);
            comment = itemView.findViewById(R.id.textFoodDetailsItemComment);
            username = itemView.findViewById(R.id.textFoodDetailsItemUsername);
        }
    }
}
