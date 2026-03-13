package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications, recyclerAuditLogs;
    private NotificationAdapter notificationAdapter;
    private AuditLogAdapter auditLogAdapter;
    private TextView tabNotifications, tabAuditLogs;
    private View layoutNotifications, layoutAuditLogs;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerAuditLogs     = findViewById(R.id.recyclerAuditLogs);
        layoutNotifications   = findViewById(R.id.layoutNotifications);
        layoutAuditLogs       = findViewById(R.id.layoutAuditLogs);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerAuditLogs.setLayoutManager(new LinearLayoutManager(this));

        // Adapter backed by the live list — populated after API call
        notificationAdapter = new NotificationAdapter(notificationList);
        recyclerNotifications.setAdapter(notificationAdapter);

        setupBottomNavigation();

        tabNotifications = findViewById(R.id.tabNotifications);
        tabAuditLogs     = findViewById(R.id.tabAuditLogs);
        tabNotifications.setOnClickListener(v -> setTabActive(true));
        tabAuditLogs.setOnClickListener(v -> setTabActive(false));

        setTabActive(true);
        loadNotifications();
        loadAuditLogs();

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                SettingsMenuHelper.showSettingsMenu(NotificationsActivity.this, v));
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_notifications);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_bookmarks) {
                Intent intent = new Intent(this, BookmarksActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_search) {
                Intent intent = new Intent(this, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_notifications) {
                return true;
            }
            return false;
        });
    }

    private void setTabActive(boolean isNotifications) {
        if (isNotifications) {
            tabNotifications.setBackgroundResource(R.drawable.bg_active_tab);
            tabNotifications.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabNotifications.setTypeface(null, android.graphics.Typeface.BOLD);

            tabAuditLogs.setBackground(null);
            tabAuditLogs.setTextColor(ContextCompat.getColor(this, R.color.gray_text));
            tabAuditLogs.setTypeface(null, android.graphics.Typeface.NORMAL);

            layoutNotifications.setVisibility(View.VISIBLE);
            layoutAuditLogs.setVisibility(View.GONE);
        } else {
            tabAuditLogs.setBackgroundResource(R.drawable.bg_active_tab);
            tabAuditLogs.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabAuditLogs.setTypeface(null, android.graphics.Typeface.BOLD);

            tabNotifications.setBackground(null);
            tabNotifications.setTextColor(ContextCompat.getColor(this, R.color.gray_text));
            tabNotifications.setTypeface(null, android.graphics.Typeface.NORMAL);

            layoutNotifications.setVisibility(View.GONE);
            layoutAuditLogs.setVisibility(View.VISIBLE);
        }
    }

    private void loadNotifications() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();

            RetrofitClient.getApiService().getNotifications(token)
                    .enqueue(new Callback<List<Notification>>() {
                        @Override
                        public void onResponse(Call<List<Notification>> call,
                                               Response<List<Notification>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                notificationList.clear();
                                notificationList.addAll(response.body());
                                notificationAdapter.notifyDataSetChanged();
                                // Mark all as read silently after the user sees them
                                markAllRead(token);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Notification>> call, Throwable t) {
                            android.util.Log.e("Notifications", "Load failed: " + t.getMessage());
                            Toast.makeText(NotificationsActivity.this,
                                    "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void markAllRead(String token) {
        RetrofitClient.getApiService().markAllNotificationsRead(token)
                .enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call,
                                           Response<okhttp3.ResponseBody> response) { }
                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) { }
                });
    }

    private void loadAuditLogs() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Initialize adapter with empty list while loading
        List<AuditLog> logs = new ArrayList<>();
        auditLogAdapter = new AuditLogAdapter(logs);
        recyclerAuditLogs.setAdapter(auditLogAdapter);

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();

            RetrofitClient.getApiService().getAuditLogs(token)
                    .enqueue(new Callback<List<AuditLog>>() {
                        @Override
                        public void onResponse(Call<List<AuditLog>> call,
                                               Response<List<AuditLog>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                logs.clear();
                                logs.addAll(response.body());
                                auditLogAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<AuditLog>> call, Throwable t) {
                            android.util.Log.e("AuditLogs", "Load failed: " + t.getMessage());
                        }
                    });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_notifications);
            // User is viewing notifications — clear the badge immediately
            NavBadgeHelper.updateNotificationBadge(bottomNav, 0);
        }
    }

    @Override
    public void onBackPressed() {
        // Navigate to home when back is pressed
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
}
