package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityOrderStatusClientBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderStatusClientActivity extends AppCompatActivity {

    ActivityOrderStatusClientBinding binding;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private int ORDER_PENDING = 25, PREPARING = 50, DELIVERING = 75, SERVING = 75, Completed = 100;
    private CurrentOrder currentOrder;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderStatusClientBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        getCurrentOrder();
        setListeners();
    }

    private void getCurrentOrder() {
        binding.buttonClientStatusConfirmReceive.setEnabled(false);
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                currentOrder.currentOrderId = document.getId();
                                currentOrder.status = document.getString(Constants.KEY_ORDER_STATUS);
                                currentOrder.orderTotalPrice = document.getDouble(Constants.KEY_ORDER_PRICE);
                                currentOrder.locationId = document.getString(Constants.KEY_LOCATION_ID);
                                currentOrder.paymentMethod = document.getString(Constants.KEY_PAYMENT_METHOD);
                                currentOrder.servingMethod = document.getString(Constants.KEY_SERVING_METHOD);
                                currentOrder.tableNum = document.getString(Constants.KEY_TABLE_NUM);
                                currentOrder.destination = document.getString(Constants.KEY_DESTINATION);
                                currentOrder.timestamp = document.getTimestamp(Constants.KEY_TIMESTAMP);

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
                                        getFoodImage(foodsProcessed, cartItems.size(), food);
                                    }
                                }
                            }
                            getOrderStatus();
                        }
                    }
                });
    }

    private void getFoodImage(int[] foodProcessed, int expectedFoodsSize, FoodInCart food) {
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
                        currentOrder.foods.add(food);
                        foodProcessed[0]++;

                        if (foodProcessed[0] == expectedFoodsSize) {
                            binding.buttonClientStatusConfirmReceive.setEnabled(true);
                        }
                    }
                });
    }

    private void setListeners() {
        binding.buttonClientStatusConfirmReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> order = new HashMap<>();
                order.put(Constants.KEY_USER_ID, currentOrder.userId);
                order.put(Constants.KEY_LOCATION_ID, currentOrder.locationId);
                order.put(Constants.KEY_ORDER_PRICE, currentOrder.orderTotalPrice);
                order.put(Constants.KEY_CARTS, currentOrder.foods);
                order.put(Constants.KEY_TIMESTAMP, currentOrder.timestamp);
                order.put(Constants.KEY_PAYMENT_METHOD, currentOrder.paymentMethod);
                order.put(Constants.KEY_SERVING_METHOD, currentOrder.servingMethod);
                order.put(Constants.KEY_TABLE_NUM, currentOrder.tableNum);
                order.put(Constants.KEY_DESTINATION, currentOrder.destination);

                db.collection(Constants.KEY_ORDER_HISTORY)
                        .add(order)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                removeCurrentOrder();
                            }
                        });
            }
        });
        binding.imageClientStatusBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void removeCurrentOrder() {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .document(currentOrder.currentOrderId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Intent intent = new Intent(OrderStatusClientActivity.this, FinishOrderClientActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void getOrderStatus() {
        String orderStatusTitle = currentOrder.status;
        binding.textClientStatusStatusTitle.setText(orderStatusTitle + "...");
        switch (orderStatusTitle) {
            case Constants.KEY_ORDER_PENDING:
                binding.textClientStatusStatusBody.setText("Waiting for order to be accepted...");
                Glide.with(OrderStatusClientActivity.this).asGif().load(R.drawable.receipt_verification).into(binding.imageClientStatusMain);
                binding.progressClientStatus.setProgress(ORDER_PENDING);
                break;
            case Constants.KEY_PREPARING:
                binding.textClientStatusStatusBody.setText("In the kitchen...");
                Glide.with(OrderStatusClientActivity.this).asGif().load(R.drawable.frying).into(binding.imageClientStatusMain);
                binding.progressClientStatus.setProgress(PREPARING);
                break;
            case Constants.KEY_DELIVERING:
                binding.textClientStatusStatusBody.setText("To your location...");
                Glide.with(OrderStatusClientActivity.this).asGif().load(R.drawable.delivery_scooter).into(binding.imageClientStatusMain);
                binding.progressClientStatus.setProgress(DELIVERING);
                break;
            case Constants.KEY_SERVING:
                binding.textClientStatusStatusBody.setText("Waiting for waiter to serve...");
                Glide.with(OrderStatusClientActivity.this).asGif().load(R.drawable.waiter).into(binding.imageClientStatusMain);
                binding.progressClientStatus.setProgress(SERVING);
                break;
            case Constants.KEY_COMPLETED:
                binding.textClientStatusStatusBody.setText("Your order as arrived. Click the below button to confirm.");
                Glide.with(OrderStatusClientActivity.this).asGif().load(R.drawable.verified).into(binding.imageClientStatusMain);
                binding.progressClientStatus.setProgress(Completed);
                binding.buttonClientStatusConfirmReceive.setVisibility(View.VISIBLE);
                binding.buttonClientStatusConfirmReceive.setEnabled(true);
                break;
        }
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
        storageRef = FirebaseStorage.getInstance().getReference();
        currentOrder = new CurrentOrder();
    }
}