package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.capstone.foodar.Adapter.AdminHomeTableCurrentOrderListAdapter;
import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityAdminHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminHomeActivity extends AppCompatActivity {

    ActivityAdminHomeBinding binding;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private ArrayList<CurrentOrder> orders;
    private StorageReference storageRef;
    private AdminHomeTableCurrentOrderListAdapter adapter;
    private final int CURRENT_ORDER_ACTIVITY_RESULT = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
        setLocation();
        getCurrentOrders();
        //getRevenueInfo(); TODO
    }

    private void getCurrentOrders() {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int[] orderProcessed = {0};
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                CurrentOrder order = new CurrentOrder();
                                order.currentOrderId = document.getId();
                                order.destination = document.getString(Constants.KEY_DESTINATION);
                                order.tableNum = document.getString(Constants.KEY_TABLE_NUM);

                                List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);

                                if (cartItems != null) {
                                    int [] foodsProcessed = {0};
                                    for (int i = 0; i < 2; i++) {
                                        Map<String, Object> cartItem = cartItems.get(i);
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
        adapter = getAdminHomeTableAdapter();
        binding.recyclerAdminHomeCurrentOrder.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private AdminHomeTableCurrentOrderListAdapter getAdminHomeTableAdapter() {
        AdminHomeTableCurrentOrderListAdapter tableAdapter = new AdminHomeTableCurrentOrderListAdapter(orders, AdminHomeActivity.this);

        tableAdapter.setOnItemClickListener(new AdminHomeTableCurrentOrderListAdapter.OnItemClickListener() {
            @Override
            public void onClick(CurrentOrder order) {
                Intent intent = new Intent(AdminHomeActivity.this, AdminCurrentOrderActivity.class);
                intent.putExtra(Constants.KEY_ORDER_ID, order.currentOrderId);
                startActivityForResult(intent, CURRENT_ORDER_ACTIVITY_RESULT);
            }
        });

        return tableAdapter;
    }

    private void setLocation() {
        db.collection(Constants.KEY_LOCATIONS).document(preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String locationName = document.getString(Constants.KEY_LOCATION_NAME);
                                binding.textAdminHomeLocation.setText(locationName);
                            }
                        }
                    }
                });
    }

    private void setListeners() {
        binding.textAdminHomeManageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        });
        binding.main.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                init();
                setListeners();
                setLocation();
                getCurrentOrders();
                //TODO getRevenueInfo();
                binding.main.setRefreshing(false);
            }
        });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        orders = new ArrayList<>();
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CURRENT_ORDER_ACTIVITY_RESULT && resultCode == RESULT_OK) {
            getCurrentOrders();
        }
    }
}