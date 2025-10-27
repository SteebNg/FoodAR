package com.capstone.foodar;

import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.capstone.foodar.Adapter.TopFoodsAdapter;
import com.capstone.foodar.Model.FoodStats;
import com.capstone.foodar.PreferenceManager.Constants;
import com.capstone.foodar.PreferenceManager.PreferenceManager;
import com.capstone.foodar.databinding.FragmentAdminHomeRevenueStatsFragementBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminHomeRevenueStatsFragment extends Fragment {

    private FragmentAdminHomeRevenueStatsFragementBinding binding;
    private FirebaseFirestore db;
    private String locationId;
    private PreferenceManager preferenceManager;

    private ArrayList<DocumentSnapshot> allOrders;
    private ArrayList<DocumentSnapshot> filteredOrders;

    private int selectedYear = Calendar.getInstance().get(Calendar.YEAR);
    private int selectedMonth = -1; // -1 means all months

    private NumberFormat currencyFormat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminHomeRevenueStatsFragementBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        init();
        setupSpinners();
        loadOrderHistory();

        return view;
    }

    private void init() {
        db = FirebaseFirestore.getInstance();
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("ms", "MY"));
        allOrders = new ArrayList<>();
        filteredOrders = new ArrayList<>();
        preferenceManager = new PreferenceManager(getContext());
        locationId = preferenceManager.getString(Constants.KEY_LOCATION_ID);
    }

    private void setupSpinners() {
        // Year spinner
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = currentYear; i >= currentYear - 5; i--) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerYear.setAdapter(yearAdapter);

        // Month spinner
        List<String> months = new ArrayList<>();
        months.add("All Months");
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        Collections.addAll(months, monthNames);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerMonth.setAdapter(monthAdapter);

        binding.spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = Integer.parseInt(years.get(position));
                filterOrders();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position - 1; // -1 for "All Months"
                filterOrders();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadOrderHistory() {
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection(Constants.KEY_ORDER_HISTORY)
                .whereEqualTo(Constants.KEY_LOCATION_ID, locationId)
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allOrders.clear();
                    allOrders.addAll(queryDocumentSnapshots.getDocuments());
                    filterOrders();
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("Firebase get data", "Failed: " + e);
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void filterOrders() {
        filteredOrders.clear();

        Calendar calendar = Calendar.getInstance();
        for (DocumentSnapshot doc : allOrders) {
            Timestamp timestamp = doc.getTimestamp(Constants.KEY_TIMESTAMP);
            if (timestamp != null) {
                Date date = timestamp.toDate();
                calendar.setTime(date);

                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);

                if (year == selectedYear) {
                    if (selectedMonth == -1 || month == selectedMonth) {
                        filteredOrders.add(doc);
                    }
                }
            }
        }

        updateStatistics();
    }

    private void updateStatistics() {
        if (filteredOrders.isEmpty()) {
            clearStatistics();
            return;
        }

        double totalSales = 0;
        double cashSales = 0;
        double bankSales = 0;
        Map<String, FoodStats> foodStatsMap = new HashMap<>();
        Map<String, Double> dailySales = new HashMap<>();
        Map<Integer, Double> monthlySales = new HashMap<>();

        for (DocumentSnapshot doc : filteredOrders) {
            Double orderPrice = doc.getDouble(Constants.KEY_ORDER_PRICE);
            String paymentMethod = doc.getString(Constants.KEY_PAYMENT_METHOD);
            Timestamp timestamp = doc.getTimestamp(Constants.KEY_TIMESTAMP);

            if (orderPrice != null) {
                totalSales += orderPrice;

                if (Constants.KEY_CASH.equals(paymentMethod)) {
                    cashSales += orderPrice;
                } else if (Constants.KEY_BANKING.equals(paymentMethod)) {
                    bankSales += orderPrice;
                }

                if (timestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dateKey = sdf.format(timestamp.toDate());
                    dailySales.put(dateKey, dailySales.getOrDefault(dateKey, 0.0) + orderPrice);

                    Calendar cal = Calendar.getInstance();
                    cal.setTime(timestamp.toDate());
                    int month = cal.get(Calendar.MONTH);
                    monthlySales.put(month, monthlySales.getOrDefault(month, 0.0) + orderPrice);
                }
            }

            List<Map<String, Object>> carts = (List<Map<String, Object>>) doc.get(Constants.KEY_CARTS);
            if (carts != null) {
                for (Map<String, Object> cart : carts) {
                    String foodId = (String) cart.get(Constants.KEY_FOOD_ID);
                    String foodName = (String) cart.get(Constants.KEY_FOOD_NAME);
                    Object foodAmountObj = cart.get(Constants.KEY_FOOD_AMOUNT);
                    Object foodPriceObj = cart.get(Constants.KEY_FOOD_PRICE);

                    int foodAmount = 0;
                    if (foodAmountObj instanceof Long) {
                        foodAmount = ((Long) foodAmountObj).intValue();
                    } else if (foodAmountObj instanceof Integer) {
                        foodAmount = (Integer) foodAmountObj;
                    }

                    double foodPrice = 0;
                    if (foodPriceObj instanceof Double) {
                        foodPrice = (Double) foodPriceObj;
                    } else if (foodPriceObj instanceof Long) {
                        foodPrice = ((Long) foodPriceObj).doubleValue();
                    }

                    if (foodId != null && foodName != null) {
                        FoodStats stats = foodStatsMap.getOrDefault(foodId,
                                new FoodStats(foodId, foodName));
                        stats.quantity += foodAmount;
                        stats.totalSales += foodPrice * foodAmount;
                        foodStatsMap.put(foodId, stats);
                    }
                }
            }
        }

        double avgOrderValue = totalSales / filteredOrders.size();

        binding.tvTotalOrders.setText(String.valueOf(filteredOrders.size()));
        binding.tvAvgOrderValue.setText(currencyFormat.format(avgOrderValue));
        binding.tvCashSales.setText(currencyFormat.format(cashSales));
        binding.tvBankSales.setText(currencyFormat.format(bankSales));

        setupPaymentPieChart(cashSales, bankSales);
        setupTopFoodsPieChart(foodStatsMap);
        setupDailySalesLineChart(dailySales);
        setupMonthlySalesBarChart(monthlySales);
        setupTopFoodsRecyclerView(foodStatsMap);
    }

    private void clearStatistics() {
        binding.tvTotalOrders.setText("0");
        binding.tvAvgOrderValue.setText("RM 0.00");
        binding.tvCashSales.setText("RM 0.00");
        binding.tvBankSales.setText("RM 0.00");

        binding.pieChartPayment.clear();
        binding.pieChartTopFoods.clear();
        binding.lineChartDailySales.clear();
        binding.barChartMonthlySales.clear();
        binding.rvTopFoods.setAdapter(null);
    }

    private void setupPaymentPieChart(double cashSales, double bankSales) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) cashSales, Constants.KEY_CASH));
        entries.add(new PieEntry((float) bankSales, Constants.KEY_BANKING));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{
                ContextCompat.getColor(getContext(), android.R.color.holo_green_light),
                ContextCompat.getColor(getContext(), android.R.color.holo_blue_light)
        });
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return currencyFormat.format(value);
            }
        });

        binding.pieChartPayment.setData(data);
        binding.pieChartPayment.getDescription().setEnabled(false);
        binding.pieChartPayment.setDrawEntryLabels(true);
        binding.pieChartPayment.setEntryLabelTextSize(12f);
        binding.pieChartPayment.setEntryLabelColor(Color.BLACK);
        binding.pieChartPayment.getLegend().setEnabled(true);
        binding.pieChartPayment.animateY(1000);
        binding.pieChartPayment.invalidate();
    }

    private void setupTopFoodsPieChart(Map<String, FoodStats> foodStatsMap) {
        List<FoodStats> sortedFoods = new ArrayList<>(foodStatsMap.values());
        Collections.sort(sortedFoods, (a, b) -> Integer.compare(b.quantity, a.quantity));

        List<PieEntry> entries = new ArrayList<>();
        int[] colors = new int[]{
                Color.rgb(255, 102, 102),
                Color.rgb(102, 178, 255),
                Color.rgb(178, 255, 102),
                Color.rgb(255, 178, 102),
                Color.rgb(178, 102, 255)
        };

        for (int i = 0; i < Math.min(5, sortedFoods.size()); i++) {
            FoodStats stats = sortedFoods.get(i);
            entries.add(new PieEntry(stats.quantity, stats.foodName));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        binding.pieChartTopFoods.setData(data);
        binding.pieChartTopFoods.getDescription().setEnabled(false);
        binding.pieChartTopFoods.setDrawEntryLabels(true);
        binding.pieChartTopFoods.setEntryLabelTextSize(10f);
        binding.pieChartTopFoods.setEntryLabelColor(Color.BLACK);
        binding.pieChartTopFoods.getLegend().setEnabled(false);
        binding.pieChartTopFoods.animateY(1000);
        binding.pieChartTopFoods.invalidate();
    }

    private void setupDailySalesLineChart(Map<String, Double> dailySales) {
        List<String> dates = new ArrayList<>(dailySales.keySet());
        Collections.sort(dates);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            entries.add(new Entry(i, dailySales.get(dates.get(i)).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Sales");
        dataSet.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark));
        dataSet.setCircleColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_light));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);

        binding.lineChartDailySales.setData(lineData);
        binding.lineChartDailySales.getDescription().setEnabled(false);
        binding.lineChartDailySales.getLegend().setEnabled(true);

        XAxis xAxis = binding.lineChartDailySales.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);

        binding.lineChartDailySales.animateX(1000);
        binding.lineChartDailySales.invalidate();
    }

    private void setupMonthlySalesBarChart(Map<Integer, Double> monthlySales) {
        List<BarEntry> entries = new ArrayList<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int i = 0; i < 12; i++) {
            double sales = monthlySales.getOrDefault(i, 0.0);
            entries.add(new BarEntry(i, (float) sales));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Sales");
        dataSet.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_dark));
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.8f);

        binding.barChartMonthlySales.setData(barData);
        binding.barChartMonthlySales.getDescription().setEnabled(false);
        binding.barChartMonthlySales.getLegend().setEnabled(true);

        XAxis xAxis = binding.barChartMonthlySales.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthNames));
        xAxis.setGranularity(1f);

        binding.barChartMonthlySales.animateY(1000);
        binding.barChartMonthlySales.invalidate();
    }

    private void setupTopFoodsRecyclerView(Map<String, FoodStats> foodStatsMap) {
        List<FoodStats> sortedFoods = new ArrayList<>(foodStatsMap.values());
        Collections.sort(sortedFoods, (a, b) -> Double.compare(b.totalSales, a.totalSales));

        TopFoodsAdapter adapter = new TopFoodsAdapter(sortedFoods, currencyFormat);
        binding.rvTopFoods.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}