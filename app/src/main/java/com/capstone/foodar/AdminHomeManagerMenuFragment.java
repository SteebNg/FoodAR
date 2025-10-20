package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.capstone.foodar.Adapter.AdminHomeManageMenuFoodCategoryListAdapter;
import com.capstone.foodar.Adapter.HomeAllMenuListAdapter;
import com.capstone.foodar.Model.Food;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.FragmentAdminHomeManagerMenuBinding;
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
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AdminHomeManagerMenuFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdminHomeManagerMenuFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public AdminHomeManagerMenuFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AdminHomeManagerMenuFragment.
     */
    public static AdminHomeManagerMenuFragment newInstance(String param1, String param2) {
        AdminHomeManagerMenuFragment fragment = new AdminHomeManagerMenuFragment();
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

    private FragmentAdminHomeManagerMenuBinding binding;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private ArrayList<String> foodCategories;
    private AdminHomeManageMenuFoodCategoryListAdapter foodCategoryAdapter;
    private String currentFoodCategory;
    private ArrayList<Food> foods;
    private ArrayList<Food> filteredFoods;
    private HomeAllMenuListAdapter foodAdapter;
    private static final String ALL = "All";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminHomeManagerMenuBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();
        getFoodCategory();
        getFoods();
        setListeners();

        // Inflate the layout for this fragment
        return view;
    }

    private void setListeners() {
        binding.buttonFragmentAdminHomeManageMenuAddFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), AdminAddFoodActivity.class);
                startActivity(intent);
            }
        });
    }

    private void getFoods() {
        db.collection(Constants.KEY_FOODS)
                .whereEqualTo(Constants.KEY_LOCATION_ID, preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (!task.isSuccessful() && task.getResult().isEmpty()) {
                            return;
                        }

                        int[] numOfFoodProcessed = {0};

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (document.exists()) {
                                loadFoods(document, numOfFoodProcessed, task.getResult().size());
                            }
                        }
                    }
                });
    }

    private void loadFoods(QueryDocumentSnapshot document, int[] numOfFoodProcessed, int numAllFoods) {
        Food food = new Food();
        food.foodId = document.getId();
        food.foodPrice = document.getDouble(Constants.KEY_FOOD_PRICE);
        food.foodName = document.getString(Constants.KEY_FOOD_NAME);
        food.foodRating = document.getDouble(Constants.KEY_FOODS_RATING);
        food.foodCategory = document.getString(Constants.KEY_FOOD_CATEGORY);
        numOfFoodProcessed[0]++;
        getFoodImage(food, numOfFoodProcessed, numAllFoods);
    }

    private void getFoodImage(Food food, int[] numOfFoodProcessed, int numAllFoods) {
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
                        foods.add(food);
                        filteredFoods.add(food);

                        if (numOfFoodProcessed[0] == numAllFoods) {
                            loadFoodRecycler();
                        }
                    }
                });
    }

    private void loadFoodRecycler() {
        foodAdapter = getFoodAdapter();
        binding.recyclerAdminHomeManageMenuFood.setAdapter(foodAdapter);
        foodAdapter.notifyDataSetChanged();
    }

    private HomeAllMenuListAdapter getFoodAdapter() {
        HomeAllMenuListAdapter adapter = new HomeAllMenuListAdapter(foods, getContext());

        adapter.setOnItemClickListener(new HomeAllMenuListAdapter.OnItemClickListener() {
            @Override
            public void onClick(Food food) {
                // TODO
            }
        });

        return adapter;
    }

    private void getFoodCategory() {
        db.collection(Constants.KEY_LOCATIONS)
                .document(preferenceManager.getString(Constants.KEY_LOCATION_ID))
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        DocumentSnapshot document = task.getResult();
                        if (document == null || !document.exists()) {
                            return;
                        }

                        Object foodCategoriesFromDb = document.get(Constants.KEY_FOOD_CATEGORY);
                        if (foodCategoriesFromDb instanceof List) {
                            try {
                                List<?> foodCategoriesCheckedTemp = (List<?>) foodCategoriesFromDb;
                                ArrayList<String> foodCategoriesChecked = (ArrayList<String>) foodCategoriesCheckedTemp;
                                addCategoriesFromArray(foodCategoriesChecked);
                            } catch (ClassCastException e) {
                                Log.e("Food Category Retrieval", "Casting Failed");
                            }
                        }
                    }
                });
    }

    private void addCategoriesFromArray(ArrayList<String> foodCategoriesFromDb) {
        foodCategories.add("All");
        foodCategories.addAll(foodCategoriesFromDb);
        addArrayToAdapter();
    }

    private void addArrayToAdapter() {
        foodCategoryAdapter = getFoodCategoryAdapter();
        binding.recyclerAdminHomeManageMenuFoodCategory.setAdapter(foodCategoryAdapter);
        foodCategoryAdapter.notifyDataSetChanged();
    }

    private AdminHomeManageMenuFoodCategoryListAdapter getFoodCategoryAdapter() {
        AdminHomeManageMenuFoodCategoryListAdapter adapter = new AdminHomeManageMenuFoodCategoryListAdapter(foodCategories, getContext());

        adapter.setOnItemClickListener(new AdminHomeManageMenuFoodCategoryListAdapter.OnItemClickListener() {
            @Override
            public void onClick(String foodCategory) {
                currentFoodCategory = foodCategory;
                filterFood();
            }
        });

        return adapter;
    }

    private void filterFood() {
        ArrayList<Food> foodsToBeInFiltered = new ArrayList<>();

        if (currentFoodCategory.equalsIgnoreCase(ALL)) {
            filteredFoods = foods;
        } else {
            for (Food food : filteredFoods) {
                if (food.foodCategory.equalsIgnoreCase(currentFoodCategory)) {
                    foodsToBeInFiltered.add(food);
                }
            }
        }

        foodAdapter.filterAllMenuList(foodsToBeInFiltered);
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        preferenceManager = new PreferenceManager(getContext());
        foodCategories = new ArrayList<>();
        currentFoodCategory = "All";
        foods = new ArrayList<>();
        filteredFoods = new ArrayList<>();
    }
}