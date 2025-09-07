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

import com.capstone.foodar.Model.Food;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class HomeAllMenuListAdapter extends RecyclerView.Adapter<HomeAllMenuListAdapter.ViewHolder>{
    private ArrayList<Food> foods;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public HomeAllMenuListAdapter(ArrayList<Food> foods, Context context) {
        this.foods = foods;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_all_menu_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foods.get(position);

        holder.foodName.setText(food.foodName);
        holder.foodPrice.setText(String.valueOf(food.foodPrice));
        holder.foodRating.setRating((float) food.foodRating);
        holder.foodImage.setImageURI(food.foodImage);
        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(foods.get(position)));
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImage;
        TextView foodName, foodPrice;
        RatingBar foodRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageAllMenuItem);
            foodName = itemView.findViewById(R.id.textAllMenuFoodName);
            foodPrice = itemView.findViewById(R.id.textAllMenuFoodPrice);
            foodRating = itemView.findViewById(R.id.ratingAllMenuFood);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(Food food);
    }
}
