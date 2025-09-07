package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.FoodOption;
import com.capstone.foodar.Model.Review;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class FoodDetailsOptionListAdapter extends RecyclerView.Adapter<FoodDetailsOptionListAdapter.ViewHolder>{

    private ArrayList<FoodOption> foodOptions;
    private Context context;

    public FoodDetailsOptionListAdapter(ArrayList<FoodOption> foodOptions, Context context) {
        this.foodOptions = foodOptions;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_food_details_options_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodOption foodOption = foodOptions.get(position);

        holder.optionTitle.setText(foodOption.optionTitle);

        if (foodOption.isCompulsory) {
            holder.optionMustChoose.setText("*Choose One");
        } else {
            holder.optionMustChoose.setText("Optional");
        }



    }

    @Override
    public int getItemCount() {
        return foodOptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView optionTitle, optionMustChoose;
        RadioGroup radioGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            optionTitle = itemView.findViewById(R.id.textFoodOptionsItemTitle);
            optionMustChoose = itemView.findViewById(R.id.textFoodOptionsItemMustChoose);
            radioGroup = itemView.findViewById(R.id.radioGroupFoodOptionItem);
        }
    }
}
