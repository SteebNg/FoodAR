package com.capstone.foodar.Adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.Food;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class HomeOrderAgainListAdapter extends RecyclerView.Adapter<HomeOrderAgainListAdapter.ViewHolder> {

    private ArrayList<Food> foods;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public HomeOrderAgainListAdapter(ArrayList<Food> foods, Context context) {
        this.foods = foods;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_order_again_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foods.get(position);

        holder.foodName.setText(food.foodName);
        holder.foodPrice.setText(String.valueOf(food.foodPrice));
        holder.foodImage.setImageURI(food.foodImage);
        holder.ratingBar.setRating((float) food.foodRating);
        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(food));
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImage;
        TextView foodName, foodPrice;
        RatingBar ratingBar;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageOrderAgainFood);
            foodName = itemView.findViewById(R.id.textOrderAgainFoodName);
            foodPrice = itemView.findViewById(R.id.textOrderAgainFoodPrice);
            ratingBar = itemView.findViewById(R.id.ratingOrderAgain);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(Food food);
    }
}
