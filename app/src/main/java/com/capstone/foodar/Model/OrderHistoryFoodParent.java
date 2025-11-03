package com.capstone.foodar.Model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class OrderHistoryFoodParent {
    public ArrayList<FoodInCart> foodsInCart;
    public String locationId, servingMode, destination, locationName;
    public Timestamp timestamp;
    public double orderPrice;
}
