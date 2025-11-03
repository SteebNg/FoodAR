package com.capstone.foodar;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Adapter.AdminCurrentOrderTableListAdapter;
import com.capstone.foodar.Adapter.AdminHomeTableCurrentOrderListAdapter;
import com.capstone.foodar.Model.CurrentOrder;
import com.capstone.foodar.Model.FoodInCart;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.FragmentAdminHomeCurrentOrderBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AdminHomeCurrentOrderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdminHomeCurrentOrderFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public AdminHomeCurrentOrderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AdminHomeCurrentOrderFragment.
     */
    public static AdminHomeCurrentOrderFragment newInstance(String param1, String param2) {
        AdminHomeCurrentOrderFragment fragment = new AdminHomeCurrentOrderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private FragmentAdminHomeCurrentOrderBinding binding;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private PreferenceManager preferenceManager;
    private ArrayList<CurrentOrder> orders;
    private AdminCurrentOrderTableListAdapter adapter;
    private Query currentOrdersQuery;
    private ListenerRegistration currentOrdersListener;
    private final int ACTIVITY_PROFILE = 21;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminHomeCurrentOrderBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();
        setListeners();
        setLocation();
        setCurrentOrdersListener();
        setProfile();

        // Inflate the layout for this fragment
        return view;
    }

    private void setProfile() {
        binding.textAdminHomeProfileName.setText(preferenceManager.getString(Constants.KEY_USERNAME));

        storageRef.child(Constants.KEY_USERS_LIST
                + "/"
                + preferenceManager.getString(Constants.KEY_USER_ID)
                + "/"
                + Constants.KEY_PROFILE_IMAGE)
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getContext()).load(uri).into(binding.imageAdminHomeProfilePic);
                    }
                });
    }

    private void setCurrentOrdersListener() {
         currentOrdersListener = currentOrdersQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("Snapshot Listener", error);
                    return;
                }

                if (value != null) {
                    if (value.getMetadata().isFromCache()) {
                        Log.d("Read Source", "Cache");
                    } else {
                        Log.d("Read Source", "Server");
                    }

                    orders.clear();

                    List<CurrentOrder> newOrders = new ArrayList<>();

                    int totalOrders = value.size();
                    if (totalOrders == 0) {
                        addOrderToRecycler();
                        return;
                    }

                    int[] orderProcessed = {0};
                    for (QueryDocumentSnapshot document : value) {
                        CurrentOrder order = new CurrentOrder();
                        order.currentOrderId = document.getId();
                        order.servingMethod = document.getString(Constants.KEY_SERVING_METHOD);
                        order.destination = document.getString(Constants.KEY_DESTINATION);
                        order.paymentMethod = document.getString(Constants.KEY_PAYMENT_METHOD);
                        order.tableNum = document.getString(Constants.KEY_TABLE_NUM);
                        order.orderTotalPrice = document.getDouble(Constants.KEY_ORDER_PRICE);
                        order.status = document.getString(Constants.KEY_ORDER_STATUS);
                        order.foods = new ArrayList<>();
                        order.timestamp = document.getTimestamp(Constants.KEY_TIMESTAMP);

                        newOrders.add(order);

                        List<Map<String, Object>> cartItems = (List<Map<String, Object>>) document.get(Constants.KEY_CARTS);

                        if (cartItems != null) {
                            int [] foodsProcessed = {0};
                            int expectedFoodsSize = cartItems.size();
                            for (Map<String, Object> cartItem : cartItems) {
                                FoodInCart food = new FoodInCart();

                                food.FoodOptions = (ArrayList<String>) cartItem.get(Constants.KEY_FOOD_OPTIONS);
                                food.FoodId = cartItem.get(Constants.KEY_FOOD_ID).toString();
                                food.FoodPrice = (double) cartItem.get(Constants.KEY_FOOD_PRICE);
                                Long foodAmount = (Long) cartItem.get(Constants.KEY_FOOD_AMOUNT);
                                if (foodAmount != null) {
                                    long lFoodAmount = foodAmount;
                                    food.FoodAmount = (int) lFoodAmount;
                                }
                                food.FoodName = cartItem.get(Constants.KEY_FOOD_NAME).toString();
                                food.Remarks = cartItem.get(Constants.KEY_REMARKS).toString();

                                getFoodImage(orderProcessed, foodsProcessed, totalOrders, expectedFoodsSize, food, order, newOrders);
                            }
                        } else {
                            orderProcessed[0]++;
                            if (orderProcessed[0] == totalOrders) {
                                orders.addAll(newOrders);
                                addOrderToRecycler();
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

    private void getFoodImage(int[] orderProcessed, int[] foodProcessed, int expectedOrderSize, int expectedFoodsSize,
                              FoodInCart food, CurrentOrder order, List<CurrentOrder> newOrders) {
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
//                            orders.add(order);
                            orderProcessed[0]++;
                            if (orderProcessed[0] == expectedOrderSize) {
                                orders.addAll(newOrders);
                                addOrderToRecycler();
                            }
                        }
                    }
                });
    }

    private void addOrderToRecycler() {
        adapter = getCurrentOrderRecycler();
        binding.recyclerAdminHomeCurrentOrder.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private AdminCurrentOrderTableListAdapter getCurrentOrderRecycler() {
        AdminCurrentOrderTableListAdapter currentOrderAdapter = new AdminCurrentOrderTableListAdapter(orders, getContext());

        currentOrderAdapter.setOnButtonClickListener(new AdminCurrentOrderTableListAdapter.OnButtonClickListener() {
            @Override
            public void onClick(CurrentOrder order, Button button) {
                String orderStatus = order.status;
                button.setEnabled(false);
                if (orderStatus.equals(Constants.KEY_ORDER_PENDING)) {
                    updateCurrentOrderStatus(Constants.KEY_PREPARING, order.currentOrderId, button);
                } else if (orderStatus.equals(Constants.KEY_PREPARING)) {
                    if (order.servingMethod.equals(Constants.KEY_DELIVERY_MODE)) {
                        updateCurrentOrderStatus(Constants.KEY_DELIVERING, order.currentOrderId, button);
                    } else {
                        updateCurrentOrderStatus(Constants.KEY_SERVING, order.currentOrderId, button);
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
        binding.swipeRefreshAdminHome.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                stopListeningForCurrentOrders();
                init();
                setListeners();
                setLocation();
                setCurrentOrdersListener();
                setProfile();
                binding.swipeRefreshAdminHome.setRefreshing(false);
            }
        });
        binding.imageAdminHomeProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ProfilePageActivity.class);
                startActivityForResult(intent, ACTIVITY_PROFILE);
            }
        });
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getContext());
        orders = new ArrayList<>();
        storageRef = FirebaseStorage.getInstance().getReference();

        currentOrdersQuery = db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.ASCENDING);
    }

    private void stopListeningForCurrentOrders() {
        if (currentOrdersListener != null) {
            currentOrdersListener.remove();
            currentOrdersListener = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopListeningForCurrentOrders();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_PROFILE && resultCode == RESULT_OK) {
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}