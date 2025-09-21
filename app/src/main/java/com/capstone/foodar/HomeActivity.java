package com.capstone.foodar;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.capstone.foodar.Adapter.HomeAllMenuListAdapter;
import com.capstone.foodar.Adapter.HomeOrderAgainListAdapter;
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private boolean isLoggedIn;
    private ArrayList<Food> orderAgainFoods;
    private ArrayList<Food> allMenuFoods;
    private HomeOrderAgainListAdapter orderAgainAdapter;
    private HomeAllMenuListAdapter allMenuAdapter;
    private PopupMenu popupMenu;
    private String currentFoodCategory;
    private final int LOCATION_ACTIVITY_RESULT = 1;
    private final int QR_CODE_ACTIVITY_RESULT = 5;
    private ArrayList<Food> filteredAllMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();
//        getDeepLinkData();
        // cause for some reason (searched the internet but no answers), google doesnt allow direct
        // edit to the hint fonts in the XML file. I dont know. Ask them.
        // changeSearchHintFont();
        setListeners();
        if (isLoggedIn) {
            loadOrderAgain();
        } else {
            binding.layoutHomeOrderAgain.setVisibility(View.GONE);
        }
        loadAllMenu();
        loadLocation();
    }

//    private void getDeepLinkData() {
//        Uri uri = getIntent().getData();
//
//        if (uri != null) {
//            List<String> params = uri.getPathSegments();
//            preferenceManager.putString(Constants.KEY_LOCATION_ID, params.get(0));
//            if (params.size() == 2) {
//                preferenceManager.putString(Constants.KEY_TABLE_NUM, params.get(1));
//            }
//        }
//    }

    private void loadLocation() {
        if (preferenceManager.contains(Constants.KEY_LOCATION_ID)) {
            db.collection(Constants.KEY_LOCATIONS)
                    .document(preferenceManager.getString(Constants.KEY_LOCATION_ID))
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult().exists()) {
                                String locationName = task.getResult().getString(Constants.KEY_LOCATION_NAME);
                                binding.textHomeLocation.setText(locationName);
                            }
                        }
                    });
        }
    }

    private void loadAllMenu() {
        if (preferenceManager.contains(Constants.KEY_LOCATION_ID)) {
            db.collection(Constants.KEY_FOODS)
                    .whereArrayContains(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.exists()) {
                                        allMenuFoods = new ArrayList<>();
                                        loadAllMenuList(document);
                                    }
                                }
                            }
                        }
                    });
        } else {
            db.collection(Constants.KEY_FOODS)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if (document.exists()) {
                                        allMenuFoods = new ArrayList<>();
                                        loadAllMenuList(document);
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void loadAllMenuList(DocumentSnapshot document) {
        Food food = new Food();
        food.foodId = document.getId();
        food.foodPrice = document.getDouble(Constants.KEY_FOOD_PRICE);
        food.foodName = document.getString(Constants.KEY_FOOD_NAME);
        food.foodRating = document.getDouble(Constants.KEY_FOODS_RATING);
        food.foodCategory = document.getString(Constants.KEY_FOOD_CATEGORY);
        getFoodAllMenuImage(food);
    }

    private void getFoodAllMenuImage(Food food) {
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
                        allMenuFoods.add(food);
                        filteredAllMenu.add(food);
                        loadAllMenuRecycler();
                    }
                });
    }

    private void loadAllMenuRecycler() {
        allMenuAdapter = getAllMenuAdapter(allMenuFoods);
        binding.recyclerHomeMenu.setAdapter(allMenuAdapter);
        allMenuAdapter.notifyDataSetChanged();
        setFoodCategoryPopupMenu();
    }

    private HomeAllMenuListAdapter getAllMenuAdapter(ArrayList<Food> foods) {
        HomeAllMenuListAdapter adapter = new HomeAllMenuListAdapter(foods, HomeActivity.this);

        adapter.setOnItemClickListener(new HomeAllMenuListAdapter.OnItemClickListener() {
            @Override
            public void onClick(Food food) {
                if (preferenceManager.contains(Constants.KEY_LOCATION_ID)) {
                    Intent intent;
                    if (isLoggedIn) {
                        intent = new Intent(HomeActivity.this, FoodDetailsActivity.class);
                        intent.putExtra(Constants.KEY_FOOD_ID, food.foodId);
                    } else {
                        intent = new Intent(HomeActivity.this, LoginActivity.class);
                    }
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(HomeActivity.this, LocationSelectActivity.class);
                    startActivityForResult(intent, LOCATION_ACTIVITY_RESULT);
                }
            }
        });

        return adapter;
    }

    private void loadOrderAgain() {
        db.collection(Constants.KEY_ORDER_HISTORY)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (document.exists()) {
                                    orderAgainFoods = new ArrayList<>();
                                    loadOrderAgainList(document);
                                }
                            }
                        } else if (task.getResult().isEmpty()) {
                            binding.layoutHomeOrderAgain.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadOrderAgainList(QueryDocumentSnapshot document) {
        Food food = new Food();
        food.foodId = document.getString(Constants.KEY_FOOD_ID);
        food.foodName = document.getString(Constants.KEY_FOOD_NAME);
        food.foodPrice = document.getDouble(Constants.KEY_FOOD_PRICE);
        food.foodRating = document.getDouble(Constants.KEY_FOODS_RATING);
        getFoodOrderAgainImage(food);
    }

    private void getFoodOrderAgainImage(Food food) {
        storageRef.child(Constants.KEY_FOODS
                        + "/"
                        + food.foodId
                        + "/"
                        + Constants.KEY_FOOD_IMAGE)
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        food.foodImage = uri;
                        orderAgainFoods.add(food);
                        loadOrderAgainRecycler();
                    }
                });
    }

    private void loadOrderAgainRecycler() {
        orderAgainAdapter = getOrderAgainAdapter(orderAgainFoods);
        binding.recyclerHomeOrderAgain.setAdapter(orderAgainAdapter);
        orderAgainAdapter.notifyDataSetChanged();
    }

    private HomeOrderAgainListAdapter getOrderAgainAdapter(ArrayList<Food> foods) {
        HomeOrderAgainListAdapter adapter = new HomeOrderAgainListAdapter(foods, HomeActivity.this);

        adapter.setOnItemClickListener(new HomeOrderAgainListAdapter.OnItemClickListener() {
            @Override
            public void onClick(Food food) {
                if (preferenceManager.contains(Constants.KEY_LOCATION_ID)) {
                    Intent intent;
                    if (isLoggedIn) {
                        intent = new Intent(HomeActivity.this, FoodDetailsActivity.class);
                        intent.putExtra(Constants.KEY_FOOD_ID, food.foodId);
                    } else {
                        intent = new Intent(HomeActivity.this, LoginActivity.class);
                    }
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(HomeActivity.this, LocationSelectActivity.class);
                    startActivityForResult(intent, LOCATION_ACTIVITY_RESULT);
                }
            }
        });

        return adapter;
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        currentFoodCategory = Constants.KEY_ALL_MENU;
        filteredAllMenu = new ArrayList<>();
        preferenceManager.clearString(Constants.KEY_TABLE_NUM);

        isLoggedIn = isLoggedIn();

        if (isLoggedIn) {
            loadUserProfileImage();
        } else {
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.account_circle_24, null);
            binding.buttonHomeProfile.setImageDrawable(drawable);
        }
    }

    private void loadUserProfileImage() {
        storageRef.child(Constants.KEY_USER_ID + "/" + Constants.KEY_PROFILE_IMAGE)
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Glide.with(getApplicationContext()).load(uri).into(binding.buttonHomeProfile);
                    }
                });
    }

    private boolean isLoggedIn() {
        return preferenceManager.contains(Constants.KEY_USER_ID);
    }

    private void setListeners() {
        binding.buttonHomeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (isLoggedIn) {
                    intent = new Intent(HomeActivity.this, ProfilePageActivity.class);
                } else {
                    intent = new Intent(HomeActivity.this, LoginActivity.class);
                }
                startActivity(intent);
            }
        });
        binding.searchBarHome.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAllMenu(newText);
                return true;
            }
        });
        binding.buttonHomeFoodCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // use all the categories String
                // then filter the recyclerView with the category
                popupMenu.show();
            }
        });
        binding.buttonHomeShoppingCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIfCurrentOrderExists();
            }
        });
        binding.refreshHome.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                init();
                setListeners();
                if (isLoggedIn) {
                    loadOrderAgain();
                } else {
                    binding.layoutHomeOrderAgain.setVisibility(View.GONE);
                }
                loadAllMenu();
                loadLocation();
                binding.refreshHome.setRefreshing(false);
            }
        });
        binding.textHomeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, LocationSelectActivity.class);
                startActivityForResult(intent, LOCATION_ACTIVITY_RESULT);
            }
        });
        binding.imageHomeLocationPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, LocationSelectActivity.class);
                startActivityForResult(intent, LOCATION_ACTIVITY_RESULT);
            }
        });
        binding.buttonHomeScanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, QrCodeScanActivity.class);
                startActivityForResult(intent, QR_CODE_ACTIVITY_RESULT);
            }
        });
    }

    private void checkIfCurrentOrderExists() {
        db.collection(Constants.KEY_CURRENT_ORDERS)
                .whereEqualTo(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Intent intent;
                        if (!task.isSuccessful()) {
                            return;
                        }

                        if (task.getResult().isEmpty()) {
                            if (isLoggedIn) {
                                if (preferenceManager.contains(Constants.KEY_LOCATION_ID)) {
                                    intent = new Intent(HomeActivity.this, CheckoutActivity.class);
                                    startActivity(intent);
                                } else {
                                    intent = new Intent(HomeActivity.this, LocationSelectActivity.class);
                                    startActivityForResult(intent, LOCATION_ACTIVITY_RESULT);
                                }
                            } else {
                                intent = new Intent(HomeActivity.this, LoginActivity.class);
                                startActivity(intent);
                            }
                        } else {
                            intent = new Intent(HomeActivity.this, OrderStatusClientActivity.class);
                            startActivity(intent);
                        }
                    }
                });
    }

    private void filterAllMenu(String query) {
        if (query.isEmpty() && allMenuAdapter != null) {
            allMenuAdapter.filterAllMenuList(allMenuFoods);
            filteredAllMenu = allMenuFoods;
        } else if (allMenuFoods != null && !allMenuFoods.isEmpty()){
            filteredAllMenu = new ArrayList<>();
            for (Food food : allMenuFoods) {
                if (food.foodName.toLowerCase().contains(query.toLowerCase())) {
                    filteredAllMenu.add(food);
                }
            }

            allMenuAdapter.filterAllMenuList(filteredAllMenu);
        }
    }

    private void setFoodCategoryPopupMenu() {
        ArrayList<String> foodCategories = getAllFoodCategories();

        popupMenu = new PopupMenu(HomeActivity.this, binding.buttonHomeFoodCategory);
        popupMenu.getMenuInflater().inflate(R.menu.menu_food_categories, popupMenu.getMenu());
        popupMenu.getMenu().add("All menu");

        for (String categories : foodCategories) {
            popupMenu.getMenu().add(categories);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                currentFoodCategory = (String) item.getTitle();
                filterMenuAccordingToCategory(currentFoodCategory);
                binding.textHomeFoodCategory.setText(currentFoodCategory);
                return true;
            }
        });
    }

    private void filterMenuAccordingToCategory(String category) {
        ArrayList<Food> foodFiltered = new ArrayList<>();

        if (category.equals(Constants.KEY_ALL_MENU)) {
            foodFiltered = allMenuFoods;
        } else {
            for (Food food : filteredAllMenu) {
                if (food.foodCategory.equals(category)) {
                    foodFiltered.add(food);
                }
            }
        }

        allMenuAdapter.filterAllMenuList(foodFiltered);
    }

    private ArrayList<String> getAllFoodCategories() {
        Set<String> uniqueCategories = new HashSet<>(); // hashset stores unique elements, thats why

        // iterate through the list and add  category to the set
        for (Food food : allMenuFoods) {
            uniqueCategories.add(food.foodCategory);
        }

        // cast the set back to an ArrayList
        return new ArrayList<>(uniqueCategories);
    }

//    private void changeSearchHintFont() {
//        SearchView searchView = binding.searchBarHome;
//        int id = searchView.getId();
//        TextView textView = searchView.se
//        Typeface face = Typeface.createFromAsset(getAssets(), "font/alata.ttf");
//        textView.setTypeface(face);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_ACTIVITY_RESULT && resultCode == RESULT_OK) {
            binding.textHomeLocation.setText(preferenceManager.getString(Constants.KEY_LOCATION_NAME));
        } else if (requestCode == QR_CODE_ACTIVITY_RESULT && resultCode == RESULT_OK) {
            loadLocation();
        }
    }
}