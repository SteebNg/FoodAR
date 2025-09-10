package com.capstone.foodar.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.foodar.Model.FoodOption;
import com.capstone.foodar.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class FoodDetailsOptionListAdapter extends RecyclerView.Adapter<FoodDetailsOptionListAdapter.ViewHolder>{

    private ArrayList<FoodOption> foodOptions;
    private Context context;
    private OnSelectedChangeListener onSelectedChangeListener;

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
        boolean isFirstButton = true;

        holder.radioGroup.removeAllViews();

        holder.optionTitle.setText(foodOption.optionTitle);

        for (Map.Entry<String, Double> individualOption : foodOption.individualOptions.entrySet()) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setId(View.generateViewId());

            String radioButtonName = individualOption.getKey();
            double optionPrice = individualOption.getValue();
            String formattedPrice = String.format(Locale.getDefault(),"%.2f", Math.abs(optionPrice));
            if (optionPrice < 0.0) {
                radioButtonName = radioButtonName + " -RM " + formattedPrice;
            } else if (optionPrice > 0.0) {
                radioButtonName = radioButtonName + " +RM " + formattedPrice;
            }
            radioButton.setText(radioButtonName);

            if (isFirstButton) {
                radioButton.setChecked(true);
                isFirstButton = false;
            }

            holder.radioGroup.addView(radioButton);
        }

        holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                RadioButton radioButton = group.findViewById(checkedId);
                onSelectedChangeListener.onChange(radioButton.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodOptions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{

        TextView optionTitle;
        RadioGroup radioGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            optionTitle = itemView.findViewById(R.id.textFoodOptionsItemTitle);
            radioGroup = itemView.findViewById(R.id.radioGroupFoodOptionItem);
        }
    }

    public void setOnSelectedChangeListener(OnSelectedChangeListener onSelectedChangeListener) {
        this.onSelectedChangeListener = onSelectedChangeListener;
    }

    public interface OnSelectedChangeListener {
        void onChange(String optionName);
    }
}
