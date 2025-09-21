package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class AdminHomeIndiCurrentOrderListAdapter extends RecyclerView.Adapter<AdminHomeIndiCurrentOrderListAdapter.ViewHolder>{

    private ArrayList<FoodInCart> foods;
    private Context context;

    public AdminHomeIndiCurrentOrderListAdapter(Context context, ArrayList<FoodInCart> foods) {
        this.context = context;
        this.foods = foods;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_admin_home_indi_current_order_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodInCart food = foods.get(position);

        Glide.with(context).load(food.foodImage).into(holder.foodImage);
        holder.foodName.setText(food.foodName);

        StringBuilder foodOptionsCombined = new StringBuilder();
        for (String option : food.foodOptions) {
            foodOptionsCombined.append(option).append("\n");
        }
        holder.foodOptions.setText(foodOptionsCombined);

        holder.foodRemark.setText(food.remarks);
        holder.foodPrice.setText(String.valueOf(food.foodPrice));
        holder.foodAmount.setText(String.valueOf(food.foodQuantity));
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView foodImage;
        TextView foodName, foodOptions, foodRemark, foodPrice, foodAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageLayoutOrderHistoryItemFoodImage);
            foodName = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodName);
            foodOptions = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodOption);
            foodRemark = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodRemarks);
            foodPrice = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodPriceTotal);
            foodAmount = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodAmount);
        }
    }
}
