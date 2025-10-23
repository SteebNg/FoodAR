package com.capstone.foodar.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.FoodStats;
import com.capstone.foodar.R;

import java.text.NumberFormat;
import java.util.List;

public class TopFoodsAdapter extends RecyclerView.Adapter<TopFoodsAdapter.ViewHolder> {

    private List<FoodStats> foodStatsList;
    private NumberFormat currencyFormat;

    public TopFoodsAdapter(List<FoodStats> foodStatsList,
                           NumberFormat currencyFormat) {
        this.foodStatsList = foodStatsList;
        this.currencyFormat = currencyFormat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodStats stats = foodStatsList.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvFoodName.setText(stats.foodName);
        holder.tvQuantity.setText("Qty: " + stats.quantity);
        holder.tvTotalSales.setText(currencyFormat.format(stats.totalSales));
    }

    @Override
    public int getItemCount() {
        return foodStatsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvFoodName, tvQuantity, tvTotalSales;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTotalSales = itemView.findViewById(R.id.tvTotalSales);
        }
    }
}
