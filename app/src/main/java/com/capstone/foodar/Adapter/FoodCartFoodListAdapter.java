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
import java.util.Locale;

public class FoodCartFoodListAdapter extends RecyclerView.Adapter<FoodCartFoodListAdapter.ViewHolder>{

    private ArrayList<FoodInCart> foodsInCart;
    private Context context;
    private OnEditOnClickListener onEditOnClickListener;
    private OnRemoveOnClickListener onRemoveOnClickListener;

    public FoodCartFoodListAdapter(ArrayList<FoodInCart> foodsInCart, Context context) {
        this.foodsInCart = foodsInCart;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_checkout_food_item_list,
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

        String formattedTotalPrice = String.format(Locale.ROOT, "Total: RM %.2f", food.FoodPrice);
        holder.foodTotalPrice.setText(formattedTotalPrice);

        Glide.with(context).load(food.foodImage).into(holder.foodImage);

        holder.editFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditOnClickListener.onClick(holder.getAdapterPosition(), food);
            }
        });

        holder.removeFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRemoveOnClickListener.onClick(holder.getAdapterPosition(), food);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodsInCart.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView foodImage;
        TextView foodName, foodOptions, remarks, foodTotalPrice, foodAmount, removeFood, editFood;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            foodImage = itemView.findViewById(R.id.imageLayoutCheckoutItemFoodImage);
            foodName = itemView.findViewById(R.id.textLayoutCheckoutItemFoodName);
            foodOptions = itemView.findViewById(R.id.textLayoutCheckoutItemFoodOption);
            remarks = itemView.findViewById(R.id.textLayoutCheckoutItemFoodRemarks);
            foodTotalPrice = itemView.findViewById(R.id.textLayoutCheckoutItemFoodPriceTotal);
            foodAmount = itemView.findViewById(R.id.textLayoutCheckoutItemFoodAmount);
            removeFood = itemView.findViewById(R.id.textLayoutCheckoutItemFoodRemove);
            editFood = itemView.findViewById(R.id.textLayoutCheckoutItemFoodEdit);
        }
    }

    public void removeCartId(String cartId, int pos) {
        for (FoodInCart food : foodsInCart) {
            if (food.CartId.equals(cartId)) {
                foodsInCart.remove(food);
                notifyItemRemoved(pos);
                break;
            }
        }
    }

    public void SetOnEditOnClickListener(OnEditOnClickListener onEditOnClickListener) {
        this.onEditOnClickListener = onEditOnClickListener;
    }

    public void SetOnRemoveOnClickListener(OnRemoveOnClickListener onRemoveOnClickListener) {
        this.onRemoveOnClickListener = onRemoveOnClickListener;
    }

    public interface OnEditOnClickListener {
        void onClick(int pos, FoodInCart food);
    }

    public interface OnRemoveOnClickListener {
        void onClick(int pos, FoodInCart food);
    }
}
