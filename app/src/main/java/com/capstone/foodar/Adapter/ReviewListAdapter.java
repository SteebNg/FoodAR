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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ReviewListAdapter extends RecyclerView.Adapter<ReviewListAdapter.ViewHolder>{

    private ArrayList<Review> reviews;
    private Context context;

    public ReviewListAdapter(ArrayList<Review> reviews, Context context) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_review_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.ratingBar.setRating((float) review.rating);
        holder.username.setText(review.userName);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        String timestamp = dateFormat.format(review.timestamp);
        holder.timestamp.setText(timestamp);

        holder.comment.setText(review.comment);
        Glide.with(context).load(review.profileImage).into(holder.userImage);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView userImage;
        TextView comment, username, timestamp;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.imageLayoutReviewUserImage);
            comment = itemView.findViewById(R.id.textLayoutReviewComment);
            username = itemView.findViewById(R.id.textLayoutReviewUsername);
            timestamp = itemView.findViewById(R.id.textLayoutReviewReviewTimestamp);
            ratingBar = itemView.findViewById(R.id.ratingLayoutReviewUserRating);
        }
    }
}
