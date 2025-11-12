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
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class HomeAllMenuListAdapter extends RecyclerView.Adapter<HomeAllMenuListAdapter.ViewHolder>{
    private ArrayList<Food> foods;
    private Context context;
    private OnItemClickListener onItemClickListener;
    private static final DecimalFormat priceFormat = new DecimalFormat("0.00");

    public HomeAllMenuListAdapter(ArrayList<Food> foods, Context context) {
        Collections.sort(foods, new Comparator<Food>() {
            @Override
            public int compare(Food o1, Food o2) {
                return Double.compare(o2.foodRating, o1.foodRating);
            }
        });
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
        String foodPrice = "RM " + priceFormat.format(food.foodPrice);
        holder.foodPrice.setText(foodPrice);
        holder.foodRating.setRating((float) food.foodRating);
        Glide.with(context).load(food.foodImage).into(holder.foodImage);
        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(foods.get(position)));
        holder.foodCategory.setText(food.foodCategory);
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView foodImage;
        TextView foodName, foodPrice, foodCategory;
        RatingBar foodRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageAllMenuItem);
            foodName = itemView.findViewById(R.id.textAllMenuFoodName);
            foodPrice = itemView.findViewById(R.id.textAllMenuFoodPrice);
            foodRating = itemView.findViewById(R.id.ratingAllMenuFood);
            foodCategory = itemView.findViewById(R.id.textAllMenuFoodCategory);
        }
    }

    public void filterAllMenuList(ArrayList<Food> filteredFoodList) {
        foods = filteredFoodList;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(Food food);
    }
}
