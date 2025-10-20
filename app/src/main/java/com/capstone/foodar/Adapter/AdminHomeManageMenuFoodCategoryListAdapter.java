package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.R;

import java.util.ArrayList;

public class AdminHomeManageMenuFoodCategoryListAdapter extends RecyclerView.Adapter<AdminHomeManageMenuFoodCategoryListAdapter.ViewHolder>{

    private ArrayList<String> foodCategories;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public AdminHomeManageMenuFoodCategoryListAdapter(ArrayList<String> foodCategories, Context context) {
        this.foodCategories = foodCategories;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_admin_home_manage_menu_food_category,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String foodCategory = foodCategories.get(position);
        holder.foodCategory.setText(foodCategory);

        if (position == 0) {
            holder.bg.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.bg_darker)
            );
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.bg.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.bg_darker)
                );
                onItemClickListener.onClick(foodCategory);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodCategories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView foodCategory;
        ConstraintLayout bg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            foodCategory = itemView.findViewById(R.id.textLayoutAdminHomeManageMenuFoodCategory);
            bg = itemView.findViewById(R.id.bgLayoutAdminHomeManageMenuFoodCategory);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(String foodCategory);
    }
}
