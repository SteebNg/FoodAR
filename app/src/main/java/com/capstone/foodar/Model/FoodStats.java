package com.capstone.foodar.Model;

public class FoodStats {

    public String foodId;
    public String foodName;
    public int quantity;
    public double totalSales;

    public FoodStats(String foodId, String foodName) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.quantity = 0;
        this.totalSales = 0;
    }
}
