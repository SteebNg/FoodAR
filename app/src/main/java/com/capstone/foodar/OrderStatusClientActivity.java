package com.capstone.foodar;

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
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityOrderStatusClientBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class OrderStatusClientActivity extends AppCompatActivity {

    ActivityOrderStatusClientBinding binding;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private int ORDER_PENDING = 25, PREPARING = 50, DELIVERING = 75, SERVING = 75, Completed = 100;
    private String currentOrderId;

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
        getOrderStatus();
        setListeners();
    }

    private void setListeners() {
        binding.buttonClientStatusConfirmReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.collection(Constants.KEY_CURRENT_ORDERS)
                        .document(currentOrderId)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(OrderStatusClientActivity.this, "Have a nice day.", Toast.LENGTH_SHORT).show();
                                finish();
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

    private void getOrderStatus() {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                currentOrderId = document.getId();
                                String orderStatusTitle = document.getString(Constants.KEY_ORDER_STATUS);
                                binding.textClientStatusStatusTitle.setText(orderStatusTitle + "...");
                                switch (orderStatusTitle) {
                                    case Constants.KEY_ORDER_PENDING:
                                        binding.textClientStatusStatusBody.setText("Waiting for order to be accepted...");
                                        Glide.with(OrderStatusClientActivity.this).load(R.drawable.receipt_verification).into(binding.imageClientStatusBack);
                                        binding.progressClientStatus.setProgress(ORDER_PENDING);
                                        break;
                                    case Constants.KEY_PREPARING:
                                        binding.textClientStatusStatusBody.setText("In the kitchen...");
                                        Glide.with(OrderStatusClientActivity.this).load(R.drawable.frying).into(binding.imageClientStatusBack);
                                        binding.progressClientStatus.setProgress(PREPARING);
                                        break;
                                    case Constants.KEY_DELIVERING:
                                        binding.textClientStatusStatusBody.setText("To your location...");
                                        Glide.with(OrderStatusClientActivity.this).load(R.drawable.delivery_scooter).into(binding.imageClientStatusBack);
                                        binding.progressClientStatus.setProgress(DELIVERING);
                                        break;
                                    case Constants.KEY_SERVING:
                                        binding.textClientStatusStatusBody.setText("Waiting for waiter to serve...");
                                        Glide.with(OrderStatusClientActivity.this).load(R.drawable.waiter).into(binding.imageClientStatusBack);
                                        binding.progressClientStatus.setProgress(SERVING);
                                        break;
                                    case Constants.KEY_COMPLETED:
                                        binding.textClientStatusStatusBody.setText("Your order as arrived. Click the below button to confirm.");
                                        Glide.with(OrderStatusClientActivity.this).load(R.drawable.verified).into(binding.imageClientStatusBack);
                                        binding.progressClientStatus.setProgress(Completed);
                                        binding.buttonClientStatusConfirmReceive.setVisibility(View.VISIBLE);
                                        binding.buttonClientStatusConfirmReceive.setEnabled(true);
                                        break;
                                }
                            }
                        }
                    }
                });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());
    }
}