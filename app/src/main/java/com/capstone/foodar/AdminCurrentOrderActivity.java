package com.capstone.foodar;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.AdminCurrentOrderTableListAdapter;
import com.capstone.foodar.Adapter.AdminHomeTableCurrentOrderListAdapter;
import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityAdminCurrentOrderBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCurrentOrderActivity extends AppCompatActivity {

    //TODO make this so that it listens to live changes
    ActivityAdminCurrentOrderBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private PreferenceManager preferenceManager;
    private ArrayList<CurrentOrder> orders;
    private AdminCurrentOrderTableListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminCurrentOrderBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        getCurrentOrders();
    }

    private void getCurrentOrders() {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }
                        int[] orderProcessed = {0};
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CurrentOrder order = new CurrentOrder();
                            order.currentOrderId = document.getId();
                            order.destination = document.getString(Constants.KEY_DESTINATION);
                            order.tableNum = document.getString(Constants.KEY_TABLE_NUM);

                            List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);

                            if (cartItems != null) {
                                int [] foodsProcessed = {0};
                                for (Map<String, Object> cartItem : cartItems) {
                                    FoodInCart food = new FoodInCart();

                                    food.foodOptions = (ArrayList<String>) cartItem.get(Constants.KEY_FOOD_OPTIONS);
                                    food.foodId = cartItem.get(Constants.KEY_FOOD_ID).toString();
                                    food.foodPrice = (double) cartItem.get(Constants.KEY_FOOD_PRICE);
                                    food.foodQuantity = (int) cartItem.get(Constants.KEY_FOOD_AMOUNT);
                                    food.foodName = cartItem.get(Constants.KEY_FOOD_NAME).toString();
                                    food.remarks = cartItem.get(Constants.KEY_REMARKS).toString();
                                    getFoodImage(orderProcessed, foodsProcessed, task.getResult().size(), cartItems.size(), food, order);
                                }
                            }
                        }
                    }
                });
    }

    private void getFoodImage(int[] orderProcessed, int[] foodProcessed, int expectedOrderSize, int expectedFoodsSize, FoodInCart food, CurrentOrder order) {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + food.foodId
                        + "/"
                        + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        food.foodImage = uri;
                        order.foods.add(food);
                        foodProcessed[0]++;

                        if (foodProcessed[0] == expectedFoodsSize) {
                            orders.add(order);
                            orderProcessed[0]++;
                            if (orderProcessed[0] == expectedOrderSize) {
                                addOrderToRecycler();
                            }
                        }
                    }
                });
    }

    private void addOrderToRecycler() {
        adapter = getCurrentOrderRecycler();
        binding.recyclerAdminCurrentOrder.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private AdminCurrentOrderTableListAdapter getCurrentOrderRecycler() {
        return new AdminCurrentOrderTableListAdapter(orders, AdminCurrentOrderActivity.this);
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }
}