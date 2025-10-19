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

public class OrderHistoryChildRecyclerListAdapter extends RecyclerView.Adapter<OrderHistoryChildRecyclerListAdapter.ViewHolder>{

    private ArrayList<FoodInCart> foodsInCart;
    private Context context;

    public OrderHistoryChildRecyclerListAdapter(ArrayList<FoodInCart> foodsInCart, Context context) {
        this.foodsInCart = foodsInCart;
        this.context = context;
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
        FoodInCart food = foodsInCart.get(position);

        holder.foodName.setText(food.FoodName);
        holder.foodAmount.setText(food.FoodAmount + "x");

        StringBuilder foodOptionsCombined = new StringBuilder();
        for (String foodOption : food.FoodOptions) {
            foodOptionsCombined.append(foodOption).append("\n");
        }
        holder.foodOptions.setText(foodOptionsCombined);

        String remarks = food.Remarks;
        if (remarks.isEmpty()) {
            holder.remarks.setText("Remarks: No remarks");
        } else {
            holder.remarks.setText("Remarks: " + remarks);
        }

        String formattedTotalPrice = "Total: RM " + food.FoodPrice;
        holder.foodTotalPrice.setText(formattedTotalPrice);

        Glide.with(context).load(food.foodImage).into(holder.foodImage);
    }

    @Override
    public int getItemCount() {
        return foodsInCart.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView foodImage;
        TextView foodName, foodOptions, remarks, foodTotalPrice, foodAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageLayoutOrderHistoryItemFoodImage);
            foodName = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodName);
            foodOptions = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodOption);
            remarks = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodRemarks);
            foodTotalPrice = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodPriceTotal);
            foodAmount = itemView.findViewById(R.id.textLayoutOrderHistoryItemFoodAmount);
        }
    }
}
