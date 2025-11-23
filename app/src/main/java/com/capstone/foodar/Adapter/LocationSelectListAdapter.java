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
import com.capstone.foodar.Model.Location;
import com.capstone.foodar.R;

import java.util.ArrayList;

public class LocationSelectListAdapter extends RecyclerView.Adapter<LocationSelectListAdapter.ViewHolder>{

    private ArrayList<Location> locations;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public LocationSelectListAdapter(ArrayList<Location> locations, Context context) {
        this.context = context;
        this.locations = locations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_location_select_item_list,
                parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Location location = locations.get(position);

        holder.locationName.setText(location.locationName);
        holder.locationAddress.setText(location.locationAddress);

        if (location.logo != null) {
            Glide.with(context).load(location.logo).into(holder.logo);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationName, locationAddress;
        ImageView logo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            locationAddress = itemView.findViewById(R.id.textLayoutLocationItemAddress);
            locationName = itemView.findViewById(R.id.textLayoutLocationItemName);
            logo = itemView.findViewById(R.id.imageLayoutLocationItem);
        }
    }

    public void setFilteredLocationList(ArrayList<Location> filteredLocations) {
        locations = filteredLocations;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(Location location);
    }
}
