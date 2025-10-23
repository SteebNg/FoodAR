package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.R;

import java.util.ArrayList;
import java.util.Locale;

public class AdminCurrentOrderTableListAdapter extends RecyclerView.Adapter<AdminCurrentOrderTableListAdapter.ViewHolder>{

    private ArrayList<CurrentOrder> orders;
    private Context context;
    private OnButtonClickListener onButtonClickListener;
    private int ORDER_PENDING = 25, PREPARING = 50, DELIVERING = 75, SERVING = 75, Completed = 100;

    public AdminCurrentOrderTableListAdapter(ArrayList<CurrentOrder> orders, Context context) {
        this.orders = orders;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_admin_current_order_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CurrentOrder order = orders.get(position);
        holder.paymentMethod.setText(order.paymentMethod);
        holder.orderTotalPrice.setText(String.format(Locale.ROOT, "RM %.2f", order.orderTotalPrice));
        holder.orderStatus.setText(order.status);
        setProgress(order.status, holder.orderProgress, holder.orderStatus, holder.buttonStatusProgressing);

        if (order.tableNum != null) {
            holder.tableNum.setText("Table: " + order.tableNum);
        } else {
            holder.tableNum.setText("Destination: " + order.destination);
        }

        holder.buttonStatusProgressing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClickListener.onClick(order, holder.buttonStatusProgressing);
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

    private void setProgress(String status, ProgressBar orderProgress, TextView orderStatus, Button button) {
        String header = "Current Status: ";
        switch (status) {
            case Constants.KEY_ORDER_PENDING:
                orderProgress.setProgress(ORDER_PENDING);
                orderStatus.setText(header + "Pending to be accepted");
                button.setText("Accept Order");
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
                break;
            case Constants.KEY_PREPARING:
                orderProgress.setProgress(PREPARING);
                orderStatus.setText(header + "Cooking");
                button.setText("Done Cooking");
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
                break;
            case Constants.KEY_DELIVERING:
                orderProgress.setProgress(DELIVERING);
                orderStatus.setText(header + "Delivering");
                button.setVisibility(View.GONE);
                break;
            case Constants.KEY_SERVING:
                orderProgress.setProgress(SERVING);
                orderStatus.setText(header + "Serving");
                button.setVisibility(View.GONE);
                break;
            case Constants.KEY_COMPLETED:
                orderProgress.setProgress(Completed);
                orderStatus.setText(header + "Waiting for customer's confirmation");
                button.setVisibility(View.GONE);
                break;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tableNum, paymentMethod, orderTotalPrice, orderStatus;
        RecyclerView orders;
        ProgressBar orderProgress;
        Button buttonStatusProgressing;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tableNum = itemView.findViewById(R.id.textLayoutAdminCurrentOrderTableItemTableNum);
            orders = itemView.findViewById(R.id.recyclerLayoutAdminCurrentOrderCurrentOrder);
            paymentMethod = itemView.findViewById(R.id.textLayoutAdminCurrentOrderTableItemPaymentMethod);
            orderTotalPrice = itemView.findViewById(R.id.textLayoutAdminCurrentOrderTableTotalOrderPrice);
            orderStatus = itemView.findViewById(R.id.textLayoutAdminCurrentOrderTableCurrentStatusHeading);
            orderProgress = itemView.findViewById(R.id.progressBarLayoutAdminCurrentOrderTable);
            buttonStatusProgressing = itemView.findViewById(R.id.buttonLayoutAdminCurrentOrderTableStatusProgress);
        }
    }

    public void setOnButtonClickListener(OnButtonClickListener onButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener;
    }

    public interface OnButtonClickListener {
        void onClick(CurrentOrder order, Button button);
    }
}
