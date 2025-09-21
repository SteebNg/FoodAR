package com.capstone.foodar.Model;

import java.util.ArrayList;

public class CurrentOrder {
    public String locationId, userId, paymentMethod, servingMethod, tableNum, destination, currentOrderId, status;
    public ArrayList<FoodInCart> foods;
    public double orderTotalPrice;
}
