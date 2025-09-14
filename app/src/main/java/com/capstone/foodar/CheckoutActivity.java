package com.capstone.foodar;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.FoodCartFoodListAdapter;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityCheckoutBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CheckoutActivity extends AppCompatActivity {

    ActivityCheckoutBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private String servingMode;
    private PreferenceManager preferenceManager;
    private ArrayList<FoodInCart> foodsInCart;
    private FoodCartFoodListAdapter foodCartListAdapter;
    private final int FOOD_DETAILS_RESULT = 2;
    private final int DESTINATION_RESULT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
        setListeners();
        getFoodList();
    }

    private void getFoodList() {
        db.collection(Constants.KEY_CARTS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            final int[] foodInCartProcessed = {0};
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                FoodInCart food = new FoodInCart();
                                food.foodId = document.getString(Constants.KEY_FOOD_ID);
                                food.foodOptions = (ArrayList<String>) document.get(Constants.KEY_FOOD_OPTIONS);
                                food.foodPrice = document.getDouble(Constants.KEY_ORDER_PRICE);
                                food.locationId = document.getString(Constants.KEY_LOCATION_ID);
                                food.remarks = document.getString(Constants.KEY_REMARKS);
                                food.foodQuantity = Math.toIntExact(document.getLong(Constants.KEY_FOOD_AMOUNT));
                                food.foodName = document.getString(Constants.KEY_FOOD_NAME);
                                food.cartId = document.getId();

                                getFoodImages(food, task.getResult().size(), foodInCartProcessed);
                            }
                        }
                    }
                });
    }

    private void getFoodImages(FoodInCart food, int numOfFoodCart, int[] foodInCartProcessed) {
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
                        foodsInCart.add(food);
                        foodInCartProcessed[0]++;

                        if (foodInCartProcessed[0] == numOfFoodCart) {
                            setFoodInCartRecycler();
                        }
                    }
                });
    }

    private void setFoodInCartRecycler() {
        foodCartListAdapter = getFoodCardListAdapter();
        binding.recyclerCheckoutFoodList.setAdapter(foodCartListAdapter);
        foodCartListAdapter.notifyDataSetChanged();
        calculateTotalPrice();
    }

    private void calculateTotalPrice() {
        double totalPrice = 0;
        if (foodsInCart != null && !foodsInCart.isEmpty()) {
            for (FoodInCart food : foodsInCart) {
                totalPrice += food.foodPrice;
            }
            String formattedTotalAmount = "RM " + String.format(Locale.getDefault(), "%.2f", totalPrice);
            binding.textCheckoutBottomTotalAmount.setText(formattedTotalAmount);
        }
    }

    private FoodCartFoodListAdapter getFoodCardListAdapter() {
        FoodCartFoodListAdapter adapter = new FoodCartFoodListAdapter(foodsInCart, getApplicationContext());

        adapter.SetOnRemoveOnClickListener(new FoodCartFoodListAdapter.OnRemoveOnClickListener() {
            @Override
            public void onClick(int pos, FoodInCart food) {
                Dialog dialog = new Dialog(CheckoutActivity.this);
                dialog.setContentView(R.layout.dialog_checkout_confirm_remove);
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setCancelable(true);
                dialog.show();

                Button button = dialog.findViewById(R.id.buttonDialogCheckoutRemoveConfirm);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        removeCart(pos, food, false);
                        calculateTotalPrice();
                    }
                });
            }
        });

        adapter.SetOnEditOnClickListener(new FoodCartFoodListAdapter.OnEditOnClickListener() {
            @Override
            public void onClick(int pos, FoodInCart food) {
                Intent intent = new Intent(CheckoutActivity.this, FoodDetailsActivity.class);
                intent.putExtra(Constants.KEY_FOOD_ID, food.foodId);
                intent.putExtra(Constants.KEY_CART_ID, food.cartId);
                intent.putExtra(Constants.KEY_POSITION, pos);
                startActivityForResult(intent, FOOD_DETAILS_RESULT);
            }
        });

        return adapter;
    }

    private void removeCart(int pos, FoodInCart food, boolean isEdit) {
        if (pos != -1) {
            db.collection(Constants.KEY_CARTS).document(food.cartId).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            foodCartListAdapter.removeCartId(food.cartId, pos);
                            foodsInCart.remove(food);

                            if (isEdit) {
                                getFoodList();
                            }
                        }
                    });
        }
    }

    private void setListeners() {
        binding.imageCheckoutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        binding.textCheckoutChangeToDelivery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.layoutCheckoutDeliveryMode.setVisibility(View.VISIBLE);
                binding.layoutCheckoutDineInMode.setVisibility(View.GONE);
                servingMode = Constants.KEY_DELIVERY_MODE;
            }
        });
        binding.textCheckoutChangeToDineIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.layoutCheckoutDineInMode.setVisibility(View.VISIBLE);
                binding.layoutCheckoutDeliveryMode.setVisibility(View.GONE);
                servingMode = Constants.KEY_DINE_IN_MODE;
            }
        });
        binding.buttonCheckoutPlaceOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (servingMode.equals(Constants.KEY_DINE_IN_MODE)) {
                    String tableNum = String.valueOf(binding.etCheckoutTableNum.getText());
                    if (tableNum.isEmpty()) {
                        Toast.makeText(CheckoutActivity.this, "Please enter the table number.", Toast.LENGTH_SHORT).show();
                    } else {
                        ArrayList<String> cartsId = new ArrayList<>();
                        for (FoodInCart food : foodsInCart) {
                            cartsId.add(food.cartId);
                        }

                        Map<String, Object> order = new HashMap<>();
                        order.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                        order.put(Constants.KEY_CARTS, cartsId);
                        order.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                        order.put(Constants.KEY_ORDER_PRICE, binding.textCheckoutBottomTotalAmount.getText().toString());
                        order.put(Constants.KEY_PAYMENT_METHOD, "Cash"); //TODO: Change the payment method??
                        order.put(Constants.KEY_SERVING_METHOD, servingMode);
                        order.put(Constants.KEY_TABLE_NUM, tableNum);
                        registerOrderToDb(order);
                    }
                } else if (servingMode.equals(Constants.KEY_DELIVERY_MODE)){
                    String destination = String.valueOf(binding.textCheckoutDeliveryDestinationName.getText());
                    if (!destination.isEmpty()) {
                        ArrayList<String> cartsId = new ArrayList<>();
                        for (FoodInCart food : foodsInCart) {
                            cartsId.add(food.cartId);
                        }

                        Map<String, Object> order = new HashMap<>();
                        order.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                        order.put(Constants.KEY_CARTS, cartsId);
                        order.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                        order.put(Constants.KEY_ORDER_PRICE, binding.textCheckoutBottomTotalAmount.getText().toString());
                        order.put(Constants.KEY_PAYMENT_METHOD, "Cash"); //TODO: Change the payment method??
                        order.put(Constants.KEY_SERVING_METHOD, servingMode);
                        order.put(Constants.KEY_DESTINATION, destination);
                        registerOrderToDb(order);
                    } else {
                        Intent intent = new Intent(CheckoutActivity.this, DestinationSelectActivity.class);
                        startActivityForResult(intent, DESTINATION_RESULT);
                    }
                }
            }
        });
        binding.imageCheckoutDestinationChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckoutActivity.this, DestinationSelectActivity.class);
                startActivityForResult(intent, DESTINATION_RESULT);
            }
        });
    }

    private void registerOrderToDb(Map<String, Object> order) {
        db.collection(Constants.KEY_CURRENT_ORDERS).add(order)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        removeCartFromDb();
                    }
                });
    }

    private void removeCartFromDb() {
        final int[] foodsInCartProcessed = {0};
        for (FoodInCart food : foodsInCart) {
            db.collection(Constants.KEY_CARTS).document(food.cartId).delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            foodsInCartProcessed[0]++;
                            if (foodsInCartProcessed[0] == foodsInCart.size()) {
//                                Intent intent = new Intent(CheckoutActivity.this, );
//                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        }
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        preferenceManager = new PreferenceManager(getApplicationContext());
        foodsInCart = new ArrayList<>();

        // TODO: Check the scanned QR data
        servingMode = Constants.KEY_DELIVERY_MODE;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FOOD_DETAILS_RESULT && resultCode == RESULT_OK && data != null) {
            if (data.getIntExtra(Constants.KEY_POSITION, -1) != -1) {
                String cartId = data.getStringExtra(Constants.KEY_CART_ID);
                int pos = data.getIntExtra(Constants.KEY_POSITION, -1);
                FoodInCart foodInCart = new FoodInCart();

                for (FoodInCart food : foodsInCart) {
                    if (food.cartId.equals(cartId)) {
                        foodInCart = food;
                        break;
                    }
                }

                removeCart(pos, foodInCart, true);
            }
        } else if (requestCode == DESTINATION_RESULT && resultCode == RESULT_OK && data != null) {
            if (!Objects.requireNonNull(data.getStringExtra(Constants.KEY_DESTINATION)).isEmpty()) {
                String destination = data.getStringExtra(Constants.KEY_DESTINATION);
                binding.textCheckoutDeliveryDestinationName.setText(destination);
            }
        }
    }
}