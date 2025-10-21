package com.capstone.foodar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
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

        final Fragment fragmentCurrentOrder = new AdminHomeCurrentOrderFragment();
        final Fragment fragmentManageMenu = new AdminHomeManagerMenuFragment();
        final Fragment fragmentRevenueStats = new AdminHomeRevenueStatsFragement();
        final FragmentManager fragmentManager = getSupportFragmentManager();

        final Fragment[] activeFragment = {fragmentCurrentOrder};

        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentCurrentOrder, "1").commit();
        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentManageMenu, "2").hide(fragmentManageMenu).commit();
        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentRevenueStats, "3").hide(fragmentRevenueStats).commit();

        binding.naviAdminHome.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int selectedItemId = item.getItemId();

                if (selectedItemId == R.id.revenueAdmin) {
                    fragmentManager.beginTransaction().hide(activeFragment[0]).show(fragmentRevenueStats).commit();
                    activeFragment[0] = fragmentRevenueStats;
                    return true;
                } else if (selectedItemId == R.id.manageMenuAdmin) {
                    fragmentManager.beginTransaction().hide(activeFragment[0]).show(fragmentManageMenu).commit();
                    activeFragment[0] = fragmentManageMenu;
                    return true;
                } else {
                    fragmentManager.beginTransaction().hide(activeFragment[0]).show(fragmentCurrentOrder).commit();
                    activeFragment[0] = fragmentCurrentOrder;
                    return true;
                }
            }
        });
    }
}