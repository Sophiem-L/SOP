package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerNotifications, recyclerAuditLogs;
    private NotificationAdapter notificationAdapter;
    private AuditLogAdapter auditLogAdapter;
    private TextView tabNotifications, tabAuditLogs;
    private View layoutNotifications, layoutAuditLogs;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerAuditLogs = findViewById(R.id.recyclerAuditLogs);
        layoutNotifications = findViewById(R.id.layoutNotifications);
        layoutAuditLogs = findViewById(R.id.layoutAuditLogs);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerAuditLogs.setLayoutManager(new LinearLayoutManager(this));

        setupBottomNavigation();

        // Setup Tabs
        tabNotifications = findViewById(R.id.tabNotifications);
        tabAuditLogs = findViewById(R.id.tabAuditLogs);

        tabNotifications.setOnClickListener(v -> setTabActive(true));
        tabAuditLogs.setOnClickListener(v -> setTabActive(false));

        // Initial state
        setTabActive(true);

        loadMockNotifications();

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            SettingsMenuHelper.showSettingsMenu(NotificationsActivity.this, v);
        });
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

    private void loadMockNotifications() {
        List<Notification> items = new ArrayList<>();
        items.add(new Notification("Document Title",
                "Mandatory protocols for handling sensitive company data and difcsdfjd sfidshfi", "2 hours ago",
                "New"));
        items.add(new Notification("Document Title",
                "Mandatory protocols for handling sensitive company data and difcsdfjd sfidshfi", "2 hours ago",
                "Urgent"));
        items.add(new Notification("Document Title",
                "Mandatory protocols for handling sensitive company data and difcsdfjd sfidshfi", "2 hours ago",
                "New"));
        items.add(new Notification("Document Title",
                "Mandatory protocols for handling sensitive company data and difcsdfjd sfidshfi", "2 hours ago",
                "Urgent"));
        items.add(new Notification("Document Title",
                "Mandatory protocols for handling sensitive company data and difcsdfjd sfidshfi", "2 hours ago",
                "New"));

        notificationAdapter = new NotificationAdapter(items);
        recyclerNotifications.setAdapter(notificationAdapter);

        loadMockAuditLogs();
    }

    private void loadMockAuditLogs() {
        List<AuditLog> logs = new ArrayList<>();
        logs.add(new AuditLog("Alice Johnson", "Project Manager", "2023-10-26 14:30",
                "Weekly Standup Summary", "Project Alpha Brief.docx", R.drawable.ic_profile));
        logs.add(new AuditLog("Bob Smith", "Software Engineer", "2023-10-25 10:00",
                "Debugging Session Notes", "Bug Report #123.pdf", R.drawable.ic_profile));
        logs.add(new AuditLog("Charlie Davis", "Compliance Officer", "2023-10-24 16:15",
                "Policy Update Review", "Security_SOP_v2.pdf", R.drawable.ic_profile));

        auditLogAdapter = new AuditLogAdapter(logs);
        recyclerAuditLogs.setAdapter(auditLogAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_notifications);
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
