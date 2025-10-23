package com.capstone.foodar.Model;

import android.net.Uri;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class OrderHistoryFoodParent {
    public ArrayList<FoodInCart> foodsInCart;
    public String location;
    public Timestamp timestamp;
}
