package com.capstone.foodar.Model;

import android.net.Uri;

import java.util.ArrayList;

public class FoodInCart {
    public String FoodId, LocationId, Remarks, FoodName, CartId;
    public double FoodPrice;
    public ArrayList<String> FoodOptions;
    public int FoodAmount;
    public Uri foodImage;
}
