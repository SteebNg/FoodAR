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

import java.sql.Timestamp;
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
        View view = LayoutInflater.from(context).inflate(R.layout.layout_order_history_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderHistoryFoodParent foodsOnThatDate = orderHistoryFoodParents.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yy - HH:mm", Locale.getDefault());
        holder.date.setText(sdf.format(firebaseToJavaUtilTimestamp(foodsOnThatDate.timestamp)));
        holder.location.setText(foodsOnThatDate.location);

        OrderHistoryChildRecyclerListAdapter adapter = new OrderHistoryChildRecyclerListAdapter(orderHistoryFoodParents.get(position).foodsInCart, context);
        holder.recyclerIndiFoodsHistory.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return orderHistoryFoodParents.size();
    }

    private Timestamp firebaseToJavaUtilTimestamp(com.google.firebase.Timestamp firebaseTimestamp) {
        long seconds = firebaseTimestamp.getSeconds();
        int nanos = firebaseTimestamp.getNanoseconds();

        long milliseconds = seconds * 1000;

        Timestamp javaTimestamp = new Timestamp(milliseconds);

        javaTimestamp.setNanos(nanos);

        return javaTimestamp;
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView date, location;
        RecyclerView recyclerIndiFoodsHistory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            location = itemView.findViewById(R.id.textLayoutHistoryLocation);
            date = itemView.findViewById(R.id.textLayoutHistoryDate);
            recyclerIndiFoodsHistory = itemView.findViewById(R.id.recyclerLayoutHistoryFoods);
        }
    }
}
