package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.OrderHistoryFoodParent;
import com.capstone.foodar.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class OrderHistoryParentRecyclerListAdapter extends RecyclerView.Adapter<OrderHistoryParentRecyclerListAdapter.ViewHolder>{

    private ArrayList<OrderHistoryFoodParent> orderHistoryFoodParents;
    private Context context;

    public OrderHistoryParentRecyclerListAdapter(ArrayList<OrderHistoryFoodParent> orderHistoryFoodParents, Context context) {
        this.orderHistoryFoodParents = orderHistoryFoodParents;
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
        OrderHistoryFoodParent foodsOnThatDate = orderHistoryFoodParents.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM", Locale.getDefault());
        holder.date.setText(sdf.format(foodsOnThatDate.timestamp));

        OrderHistoryChildRecyclerListAdapter adapter = new OrderHistoryChildRecyclerListAdapter(orderHistoryFoodParents.get(position).foodsInCart, context);
        holder.recyclerIndiFoodsHistory.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return orderHistoryFoodParents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date;
        RecyclerView recyclerIndiFoodsHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.textLayoutHistoryDate);
            recyclerIndiFoodsHistory = itemView.findViewById(R.id.recyclerLayoutHistoryFoods);
        }
    }
}
