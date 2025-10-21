package com.capstone.foodar;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminHomeCurrentOrderBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();
        setListeners();
        setLocation();
        setCurrentOrdersListener();

        // Inflate the layout for this fragment
        return view;
    }

    private void setCurrentOrdersListener() {
        currentOrdersQuery.addSnapshotListener(MetadataChanges.EXCLUDE, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("Snapshot Listener", error);
                    return;
                }

                if (value != null) {
                    if (value.getMetadata().isFromCache()) {
                        Log.d("Read Source", "Cache");
                        return;
                    } else {
                        Log.d("Read Source", "Server");
                    }

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
                init();
                setListeners();
                setLocation();
                setCurrentOrdersListener();
                binding.swipeRefreshAdminHome.setRefreshing(false);
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
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING);
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
}