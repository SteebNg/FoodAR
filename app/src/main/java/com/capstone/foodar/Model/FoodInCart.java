package com.capstone.foodar.Model;

import android.net.Uri;

import java.util.ArrayList;

public class FoodInCart {
    public String foodId, locationId, remarks, foodName, cartId;
    public double foodPrice;
    public ArrayList<String> foodOptions;
    public int foodQuantity;
    public Uri foodImage;
}
