package com.capstone.foodar;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.AdminCurrentOrderTableListAdapter;
import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityAdminCurrentOrderBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCurrentOrderActivity extends AppCompatActivity {

    ActivityAdminCurrentOrderBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private PreferenceManager preferenceManager;
    private ArrayList<CurrentOrder> orders;
    private AdminCurrentOrderTableListAdapter adapter;
    private Query currentOrdersQuery;
    private ListenerRegistration currentOrdersListener;

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
        setCurrentOrdersListener();
    }

    private void setCurrentOrdersListener() {
        currentOrdersQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w("Snapshot Listener", error);
                            return;
                        }

                        if (value != null) {
                            int[] orderProcessed = {0};
                            for (QueryDocumentSnapshot document : value) {
                                CurrentOrder order = new CurrentOrder();
                                order.currentOrderId = document.getId();
                                order.destination = document.getString(Constants.KEY_DESTINATION);
                                order.tableNum = document.getString(Constants.KEY_TABLE_NUM);
                                order.orderTotalPrice = document.getDouble(Constants.KEY_ORDER_PRICE);
                                order.status = document.getString(Constants.KEY_ORDER_STATUS);

                                List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);

                                if (cartItems != null) {
                                    int [] foodsProcessed = {0};
                                    for (Map<String, Object> cartItem : cartItems) {
                                        FoodInCart food = new FoodInCart();

                                        food.FoodOptions = (ArrayList<String>) cartItem.get(Constants.KEY_FOOD_OPTIONS);
                                        food.FoodId = cartItem.get(Constants.KEY_FOOD_ID).toString();
                                        food.FoodPrice = (double) cartItem.get(Constants.KEY_FOOD_PRICE);
                                        food.FoodAmount = (int) cartItem.get(Constants.KEY_FOOD_AMOUNT);
                                        food.FoodName = cartItem.get(Constants.KEY_FOOD_NAME).toString();
                                        food.Remarks = cartItem.get(Constants.KEY_REMARKS).toString();

                                        getFoodImage(orderProcessed, foodsProcessed, value.size(), cartItems.size(), food, order);
                                    }
                                }
                            }
                        }
                    }
                });
    }

//    private void getCurrentOrders() {
//        db.collection(Constants.KEY_CURRENT_ORDERS)
//                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
//                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (!task.isSuccessful()) {
//                            return;
//                        }
//                        int[] orderProcessed = {0};
//                        for (QueryDocumentSnapshot document : task.getResult()) {
//                            CurrentOrder order = new CurrentOrder();
//                            order.currentOrderId = document.getId();
//                            order.destination = document.getString(Constants.KEY_DESTINATION);
//                            order.tableNum = document.getString(Constants.KEY_TABLE_NUM);
//                            order.orderTotalPrice = document.getDouble(Constants.KEY_ORDER_PRICE);
//                            order.status = document.getString(Constants.KEY_ORDER_STATUS);
//
//                            List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);
//
//                            if (cartItems != null) {
//                                int [] foodsProcessed = {0};
//                                for (Map<String, Object> cartItem : cartItems) {
//                                    FoodInCart food = new FoodInCart();
//
//                                    food.FoodOptions = (ArrayList<String>) cartItem.get(Constants.KEY_FOOD_OPTIONS);
//                                    food.FoodId = cartItem.get(Constants.KEY_FOOD_ID).toString();
//                                    food.FoodPrice = (double) cartItem.get(Constants.KEY_FOOD_PRICE);
//                                    food.FoodAmount = (int) cartItem.get(Constants.KEY_FOOD_AMOUNT);
//                                    food.FoodName = cartItem.get(Constants.KEY_FOOD_NAME).toString();
//                                    food.Remarks = cartItem.get(Constants.KEY_REMARKS).toString();
//                                    getFoodImage(orderProcessed, foodsProcessed, task.getResult().size(), cartItems.size(), food, order);
//                                }
//                            }
//                        }
//                    }
//                });
//    }

    private void getFoodImage(int[] orderProcessed, int[] foodProcessed, int expectedOrderSize, int expectedFoodsSize, FoodInCart food, CurrentOrder order) {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + food.FoodId
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
        AdminCurrentOrderTableListAdapter currentOrderAdapter = new AdminCurrentOrderTableListAdapter(orders, AdminCurrentOrderActivity.this);

        currentOrderAdapter.setOnUpdateButtonClickListener(new AdminCurrentOrderTableListAdapter.OnButtonClickListener() {
            @Override
            public void onClick(CurrentOrder order, Button confirmButton, Button cancelButton) {
                String orderStatus = order.status;
                confirmButton.setEnabled(false);
                if (orderStatus.equals(Constants.KEY_ORDER_PENDING)) {
                    updateCurrentOrderStatus(Constants.KEY_PREPARING, order.currentOrderId, confirmButton);
                } else if (orderStatus.equals(Constants.KEY_PREPARING)) {
                    if (order.servingMethod.equals(Constants.KEY_DELIVERY_MODE)) {
                        updateCurrentOrderStatus(Constants.KEY_DELIVERING, order.currentOrderId, confirmButton);
                    } else {
                        updateCurrentOrderStatus(Constants.KEY_SERVING, order.currentOrderId, confirmButton);
                    }
                }
            }
        });

        return currentOrderAdapter;
    }

    private void updateCurrentOrderStatus(String status, String currentOrderId, Button button) {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .document(currentOrderId)
                .update(Constants.KEY_ORDER_STATUS, status)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        button.setEnabled(true);
                    }
                });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        preferenceManager = new PreferenceManager(getApplicationContext());
        orders = new ArrayList<>();

        currentOrdersQuery = db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING);
    }

    private void stopListeningForCurrentOrders() {
        if (currentOrdersListener != null) {
            currentOrdersListener.remove();
            currentOrdersListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningForCurrentOrders();
    }
}