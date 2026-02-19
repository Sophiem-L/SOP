package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView userName, userEmail, userRoleBadge, userPhone;
    private View btnSettings, editProfileItem, changePasswordItem, manageDownloadsItem, viewActivityLogItem,
            notificationPrefsItem, logoutItem;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            startActivity(new Intent(ProfileActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Setup bottom navigation
        setupBottomNavigation();

        // Load user data
        loadUserData();

        // Setup click listeners
        setupClickListeners();
    }

    private void initViews() {
        // Header views
        // Actions and Menu
        btnSettings = findViewById(R.id.btnSettings);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userPhone = findViewById(R.id.userPhone);
        userRoleBadge = findViewById(R.id.userRoleBadge);

        editProfileItem = findViewById(R.id.editProfileItem);
        changePasswordItem = findViewById(R.id.changePasswordItem);
        manageDownloadsItem = findViewById(R.id.manageDownloadsItem);
        viewActivityLogItem = findViewById(R.id.viewActivityLogItem);
        notificationPrefsItem = findViewById(R.id.notificationPrefsItem);
        logoutItem = findViewById(R.id.logoutItem);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_profile);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.navigation_bookmarks) {
                startActivity(new Intent(this, BookmarksActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.navigation_profile) {
                return true;
            } else if (id == R.id.navigation_search) {
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.navigation_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Load user data from Firebase
            userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Group 8");
            userEmail.setText(currentUser.getEmail());
            userPhone.setText("+1 (555) 123-4567");
            userRoleBadge.setText("Manager");
        } else {
            // Fallback user data
            userName.setText("Group 8");
            userEmail.setText("john.doe@example.com");
            userPhone.setText("+1 (555) 123-4567");
            userRoleBadge.setText("Manager");
        }
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> {
            // TODO: Open settings
        });

        editProfileItem.setOnClickListener(v -> {
            // TODO: Open edit profile
        });

        changePasswordItem.setOnClickListener(v -> {
            // TODO: Open change password
        });

        manageDownloadsItem.setOnClickListener(v -> {
            // TODO: Open downloads
        });

        viewActivityLogItem.setOnClickListener(v -> {
            // TODO: Open activity logs
        });

        notificationPrefsItem.setOnClickListener(v -> {
            // TODO: Open notification settings
        });

        logoutItem.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Navigate to home when back is pressed
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
