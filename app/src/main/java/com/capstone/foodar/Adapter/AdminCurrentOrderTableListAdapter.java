package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class AdminCurrentOrderTableListAdapter extends RecyclerView.Adapter<AdminCurrentOrderTableListAdapter.ViewHolder>{

    private ArrayList<CurrentOrder> orders;
    private Context context;
    private AdminHomeTableCurrentOrderListAdapter.OnItemClickListener onItemClickListener;

    public AdminCurrentOrderTableListAdapter(ArrayList<CurrentOrder> orders, Context context) {
        this.orders = orders;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_admin_home_table_current_order_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.more.setVisibility(View.GONE);
        CurrentOrder order = orders.get(position);

        if (!order.tableNum.isEmpty()) {
            holder.tableNum.setText(order.tableNum);
        } else {
            holder.tableNum.setText(order.destination);
        }
        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onClick(order);
            }
        });

        AdminHomeIndiCurrentOrderListAdapter adapter = new AdminHomeIndiCurrentOrderListAdapter(context, order.foods);
        holder.orders.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tableNum, more;
        RecyclerView orders;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tableNum = itemView.findViewById(R.id.textLayoutOrderHistoryTableItemTableNum);
            orders = itemView.findViewById(R.id.recyclerLayoutAdminHomeTableCurrentOrder);
            more = itemView.findViewById(R.id.textLayoutOrderHistoryTableItemMore);
        }
    }
}
