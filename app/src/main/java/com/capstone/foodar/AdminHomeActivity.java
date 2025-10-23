package com.capstone.foodar;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.capstone.foodar.databinding.ActivityAdminHomeBinding;
import com.google.android.material.navigation.NavigationBarView;

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
        final Fragment fragmentRevenueStats = new AdminHomeRevenueStatsFragment();
        final Fragment fragmentGenTableQr = new AdminGenerateTableQrFragment();
        final FragmentManager fragmentManager = getSupportFragmentManager();

        final Fragment[] activeFragment = {fragmentCurrentOrder};

        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentCurrentOrder, "1").commit();
        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentManageMenu, "2").hide(fragmentManageMenu).commit();
        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentRevenueStats, "3").hide(fragmentRevenueStats).commit();
        fragmentManager.beginTransaction().add(R.id.frameAdminHome, fragmentGenTableQr, "3").hide(fragmentGenTableQr).commit();


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
                } else if (selectedItemId == R.id.genTableQrAdmin){
                    fragmentManager.beginTransaction().hide(activeFragment[0]).show(fragmentGenTableQr).commit();
                    activeFragment[0] = fragmentGenTableQr;
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