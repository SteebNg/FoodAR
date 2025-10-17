package com.capstone.foodar;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.OrderHistoryParentRecyclerListAdapter;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.Model.OrderHistoryFoodParent;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityOrderHistoryBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderHistoryActivity extends AppCompatActivity {

    ActivityOrderHistoryBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private PreferenceManager preferenceManager;
    private ArrayList<OrderHistoryFoodParent> orderHistoryFoodParents;
    private OrderHistoryParentRecyclerListAdapter parentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
        getFoodHistories();
    }

    private void getFoodHistories() {
        db.collection(Constants.KEY_ORDER_HISTORY)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                binding.layoutOrderHistoryEmpty.setVisibility(View.GONE);
                                binding.recyclerOrderHistory.setVisibility(View.VISIBLE);
                                int[] orderProcessed = {0};
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    OrderHistoryFoodParent order = new OrderHistoryFoodParent();
                                    order.timestamp = document.getTimestamp(Constants.KEY_TIMESTAMP);

                                    List<Map<String, Object>> foodsInCartData = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);
                                    if (foodsInCartData != null) {
                                        int[] foodProcessed = {0};
                                        ArrayList<FoodInCart> foodsInCart = new ArrayList<>();
                                        for (Map<String, Object> foodMap : foodsInCartData) {
                                            FoodInCart foodInCart = new FoodInCart();
                                            foodInCart.CartId = foodMap.get(Constants.KEY_CART_ID).toString();
                                            foodInCart.FoodId = (String) foodMap.get(Constants.KEY_FOOD_ID);
                                            foodInCart.FoodOptions = (ArrayList<String>) foodMap.get(Constants.KEY_FOOD_OPTIONS);
                                            foodInCart.FoodPrice = (double) foodMap.get(Constants.KEY_FOOD_PRICE);
                                            foodInCart.LocationId = (String) foodMap.get(Constants.KEY_LOCATION_ID);
                                            foodInCart.Remarks = (String) foodMap.get(Constants.KEY_REMARKS);
                                            foodInCart.FoodAmount = Math.toIntExact((long) foodMap.get(Constants.KEY_FOOD_AMOUNT));
                                            foodInCart.FoodName = (String) foodMap.get(Constants.KEY_FOOD_NAME);
                                            getFoodsImage(foodsInCart, foodInCart, foodProcessed, order, orderProcessed, task.getResult().size());
                                        }
                                    }
                                }
                            } else {
                                binding.layoutOrderHistoryEmpty.setVisibility(View.VISIBLE);
                                binding.recyclerOrderHistory.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    private void getFoodsImage(ArrayList<FoodInCart> foodsInCart, FoodInCart foodInCart, int[] foodProcessed, OrderHistoryFoodParent order, int[] orderProcessed, int totalOrders) {
        storageRef.child(Constants.KEY_FOODS
                + "/"
                + foodInCart.FoodId
                + "/"
                + Constants.KEY_FOOD_IMAGE + ".jpeg")
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        foodInCart.foodImage = uri;
                        foodsInCart.add(foodInCart);
                        foodProcessed[0]++;
                        if (foodProcessed[0] == foodsInCart.size()) {
                            order.foodsInCart = foodsInCart;
                            orderProcessed[0]++;
                        }
                        if (orderProcessed[0] == totalOrders) {
                            orderHistoryFoodParents.add(order);
                            setOrderHistoryRecycler();
                        }
                    }
                });
    }

    private void setOrderHistoryRecycler() {
        parentAdapter = getParentAdapter();
        binding.recyclerOrderHistory.setAdapter(parentAdapter);
        parentAdapter.notifyDataSetChanged();
    }

    private OrderHistoryParentRecyclerListAdapter getParentAdapter() {
        return new OrderHistoryParentRecyclerListAdapter(orderHistoryFoodParents, OrderHistoryActivity.this);
    }

    private void setListeners() {
        binding.imageOrderHistoryBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        preferenceManager = new PreferenceManager(getApplicationContext());
        orderHistoryFoodParents = new ArrayList<>();
    }
}