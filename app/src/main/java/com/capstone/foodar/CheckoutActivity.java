package com.capstone.foodar;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.capstone.foodar.Adapter.FoodCartFoodListAdapter;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityCheckoutBinding;
import com.capstone.foodar.databinding.DialogPromptBankOtpBinding;
import com.capstone.foodar.databinding.DialogPromptBankingInfoBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private String paymentMethod = Constants.KEY_CASH;
    private final String channelId = "foodar.otp";
    private final String description = "Notification For OTP";
    private final int notificationId = 100;
    private int otpNum = 0;

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
        checkForDeepLinkData();
        setListeners();
        getFoodList();
    }

    private void checkForDeepLinkData() {
        if (preferenceManager.contains(Constants.KEY_TABLE_NUM)) {
            binding.etCheckoutTableNum.setText(preferenceManager.getString(Constants.KEY_TABLE_NUM));
        }
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
                if (foodsInCart.isEmpty()) {
                    Snackbar snackbar = Snackbar.make(binding.main, "Empty cart. Place some order and checkout here.", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    return;
                }

                if (!paymentMethod.equals(Constants.KEY_BANKING)) {
                    handleOrderSubmission();
                    return;
                }

                promptBankingDetails(new BankingInfoCallback() {
                    @Override
                    public void onInfoValidated(boolean isValid) {
                        if (isValid) {
                            promptOtp(new OtpCallback() {
                                @Override
                                public void onOtpValidated(boolean isValid) {
                                    if (isValid) {
                                        handleOrderSubmission();
                                    }
                                }
                            });
                        }
                      
                        Map<String, Object> order = new HashMap<>();
                        order.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                        order.put(Constants.KEY_CARTS, cartsId);
                        order.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                        order.put(Constants.KEY_ORDER_PRICE, binding.textCheckoutBottomTotalAmount.getText().toString());
                        order.put(Constants.KEY_PAYMENT_METHOD, "Cash"); //TODO: Change the payment method??
                        order.put(Constants.KEY_SERVING_METHOD, servingMode);
                        order.put(Constants.KEY_TABLE_NUM, tableNum);
                        order.put(Constants.KEY_TIMESTAMP, Timestamp.now());
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
                        order.put(Constants.KEY_TIMESTAMP, Timestamp.now());
                        registerOrderToDb(order);
                    } else {
                        Intent intent = new Intent(CheckoutActivity.this, DestinationSelectActivity.class);
                        startActivityForResult(intent, DESTINATION_RESULT);
                    }
                });
            }
        });
        binding.imageCheckoutDestinationChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CheckoutActivity.this, DestinationSelectActivity.class);
                startActivityForResult(intent, DESTINATION_RESULT);
            }
        });
        binding.layoutCheckoutMoney.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentMethod = Constants.KEY_CASH;
                binding.layoutCheckoutMoney.setBackground(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.bg_rectangle_outlined_rounded_corner, null));
                binding.layoutCheckoutBanking.setBackground(null);
            }
        });
        binding.layoutCheckoutBanking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentMethod = Constants.KEY_BANKING;
                binding.layoutCheckoutBanking.setBackground(
                        ResourcesCompat.getDrawable(getResources(), R.drawable.bg_rectangle_outlined_rounded_corner, null));
                binding.layoutCheckoutMoney.setBackground(null);
            }
        });
    }

    private void handleOrderSubmission() {
        if (servingMode.equals(Constants.KEY_DINE_IN_MODE)) {
            String tableNum = String.valueOf(binding.etCheckoutTableNum.getText());
            if (tableNum.isEmpty()) {
                Toast.makeText(CheckoutActivity.this, "Please enter the table number.", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, Object> order = new HashMap<>();
                order.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                order.put(Constants.KEY_CARTS, foodsInCart);
                order.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                order.put(Constants.KEY_ORDER_PRICE, binding.textCheckoutBottomTotalAmount.getText().toString());
                order.put(Constants.KEY_PAYMENT_METHOD, paymentMethod);
                order.put(Constants.KEY_SERVING_METHOD, servingMode);
                order.put(Constants.KEY_TIMESTAMP, Timestamp.now());
                order.put(Constants.KEY_TABLE_NUM, tableNum);
                registerOrderToDb(order);
            }
        } else if (servingMode.equals(Constants.KEY_DELIVERY_MODE)){
            String destination = String.valueOf(binding.textCheckoutDeliveryDestinationName.getText());
            if (!destination.isEmpty()) {
                Map<String, Object> order = new HashMap<>();
                order.put(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID));
                order.put(Constants.KEY_CARTS, foodsInCart);
                order.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                order.put(Constants.KEY_ORDER_PRICE, binding.textCheckoutBottomTotalAmount.getText().toString());
                order.put(Constants.KEY_PAYMENT_METHOD, paymentMethod);
                order.put(Constants.KEY_SERVING_METHOD, servingMode);
                order.put(Constants.KEY_TIMESTAMP, Timestamp.now());
                order.put(Constants.KEY_DESTINATION, destination);
                registerOrderToDb(order);
            } else {
                Intent intent = new Intent(CheckoutActivity.this, DestinationSelectActivity.class);
                startActivityForResult(intent, DESTINATION_RESULT);
            }
        }
        preferenceManager.clearString(Constants.KEY_TABLE_NUM);
    }

    private void promptBankingDetails(BankingInfoCallback callback) {
        DialogPromptBankingInfoBinding bindingBankingPrompt = DialogPromptBankingInfoBinding.inflate(getLayoutInflater());

        Dialog dialogBankInfo = new Dialog(CheckoutActivity.this);
        dialogBankInfo.setContentView(bindingBankingPrompt.getRoot());
        dialogBankInfo.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogBankInfo.setCancelable(false);
        dialogBankInfo.show();

        bindingBankingPrompt.buttonDialogBankingCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogBankInfo.dismiss();
                callback.onInfoValidated(false);
            }
        });

        bindingBankingPrompt.buttonDialogBankingSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cardNum = bindingBankingPrompt.etDialogBankingBankNum.getText().toString();
                String cvc = bindingBankingPrompt.etDialogBankingBankCvc.getText().toString();
                boolean infoValid = isCardInfoValid(cardNum, cvc);
                if (!infoValid) {
                    Toast.makeText(CheckoutActivity.this, "Card Info Invalid", Toast.LENGTH_SHORT).show();
                    dialogBankInfo.dismiss();
                    callback.onInfoValidated(false);
                } else {
                    dialogBankInfo.dismiss();
                    callback.onInfoValidated(true);
                }
            }
        });
    }

    private void promptOtp(OtpCallback callback) {
        DialogPromptBankOtpBinding bindingOtp = DialogPromptBankOtpBinding.inflate(getLayoutInflater());

        Dialog dialogOtp = new Dialog(CheckoutActivity.this);
        dialogOtp.setContentView(bindingOtp.getRoot());
        dialogOtp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogOtp.setCancelable(false);
        dialogOtp.show();

        createNotificationChannel();
        generateOtp();
        showOtpNotification();
        otpCountdown(bindingOtp);

        bindingOtp.buttonDialogOtpCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogOtp.dismiss();
                callback.onOtpValidated(false);
            }
        });

        bindingOtp.buttonDialogOtpResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNotificationChannel();
                generateOtp();
                showOtpNotification();
                otpCountdown(bindingOtp);
            }
        });

        bindingOtp.buttonDialogOtpConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bindingOtp.etDialogBankingOtp.getText().toString().equals(String.valueOf(otpNum))) {
                    Toast.makeText(CheckoutActivity.this, "Wrong OTP Number", Toast.LENGTH_SHORT).show();
                } else {
                    callback.onOtpValidated(true);
                }
            }
        });
    }

    private void generateOtp() {
        Random random = new Random();
        otpNum = random.nextInt(9000) + 1000;
    }

    private void showOtpNotification() {
        boolean permissionNoti = checkNotificationPermission();
        if (permissionNoti) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(CheckoutActivity.this, channelId)
                    .setSmallIcon(R.drawable.textsms_24)
                    .setContentText("OTP Number")
                    .setContentText(String.valueOf(otpNum))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(CheckoutActivity.this);
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(CheckoutActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(
                        CheckoutActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );

                return false;
            }
        } else {
            return true;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    channelId,
                    description,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationChannel.enableVibration(true); // Allow vibration for notifications

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private void otpCountdown(DialogPromptBankOtpBinding bindingOtp) {
        bindingOtp.buttonDialogOtpCountdown.setVisibility(View.VISIBLE);
        bindingOtp.buttonDialogOtpResend.setVisibility(View.GONE);
        bindingOtp.buttonDialogOtpResend.setEnabled(false);

        new CountDownTimer(60000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                NumberFormat f = new DecimalFormat("00");
                long sec = (millisUntilFinished / 1000) % 60;
                bindingOtp.buttonDialogOtpCountdown.setText(f.format(sec));
            }

            @Override
            public void onFinish() {
                bindingOtp.buttonDialogOtpCountdown.setVisibility(View.GONE);
                bindingOtp.buttonDialogOtpResend.setVisibility(View.VISIBLE);
                bindingOtp.buttonDialogOtpResend.setEnabled(true);
            }
        }.start();
    }

    private boolean isCardInfoValid(String cardNum, String cvc) {
        boolean validCardNum = isValidBankNumber(cardNum);
        if (cvc.length() < 3 || cvc.length() > 4) {
            return true;
        } else {
            return validCardNum;
        }
    }

    private static boolean isValidBankNumber(String bankAccountNumber)
    {
        // Regex to check valid BANK ACCOUNT NUMBER Code
        String regex = "^[0-9]{9,18}$";

        Pattern p = Pattern.compile(regex);

        if (bankAccountNumber == null) {
            return false;
        } else if (bankAccountNumber.length() != 16) {
            return false;
        }

        Matcher m = p.matcher(bankAccountNumber);

        return m.matches();
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
                                preferenceManager.putBoolean(Constants.KEY_ORDERING, true);
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

        if (preferenceManager.contains(Constants.KEY_TABLE_NUM)) {
            servingMode = Constants.KEY_DINE_IN_MODE;
            binding.layoutCheckoutDineInMode.setVisibility(View.VISIBLE);
            binding.layoutCheckoutDeliveryMode.setVisibility(View.GONE);
        } else {
            servingMode = Constants.KEY_DELIVERY_MODE;
            binding.layoutCheckoutDineInMode.setVisibility(View.GONE);
            binding.layoutCheckoutDeliveryMode.setVisibility(View.VISIBLE);
        }
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

    private interface BankingInfoCallback {
        void onInfoValidated(boolean isValid);
    }

    private interface OtpCallback {
        void onOtpValidated(boolean isValid);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showOtpNotification();
            } else {
                Toast.makeText(this, "Notification is required to receive OTP number.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}