package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView userName, userEmail, userRoleBadge, userPhone, userJobTitle, userDept;
    private View btnSettings, editProfileItem, changePasswordItem, manageDownloadsItem, viewActivityLogItem,
            notificationPrefsItem, logoutItem;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Show saved data immediately so name and phone are correct even before refetch
                    Intent data = result.getData();
                    if (data.hasExtra("name")) userName.setText(data.getStringExtra("name"));
                    if (data.hasExtra("job_title")) userJobTitle.setText(data.getStringExtra("job_title"));
                    if (data.hasExtra("phone")) userPhone.setText(data.getStringExtra("phone"));
                    loadUserDataFromBackend();
                }
            });

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
        loadUserDataFromBackend();

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
        userJobTitle = findViewById(R.id.userJobTitle);
        userDept = findViewById(R.id.userDept);
        userRoleBadge = findViewById(R.id.userRoleBadge);

        editProfileItem = findViewById(R.id.editProfileItem);
        changePasswordItem = findViewById(R.id.changePasswordItem);
        manageDownloadsItem = findViewById(R.id.manageDownloadsItem);
        viewActivityLogItem = findViewById(R.id.viewActivityLogItem);
        notificationPrefsItem = findViewById(R.id.notificationPrefsItem);
        logoutItem = findViewById(R.id.logoutItem);
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_profile);
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
            } else if (id == R.id.navigation_profile) {
                return true;
            } else if (id == R.id.navigation_search) {
                Intent intent = new Intent(this, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_notifications) {
                Intent intent = new Intent(this, NotificationsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private void loadUserDataFromBackend() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            return;

        currentUser.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = "Bearer " + task.getResult().getToken();
                RetrofitClient.getApiService().getProfile(token).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            User backendUser = response.body();
                            updateUIWithBackendData(backendUser);
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        // Silent failure: keep current UI
                    }
                });
            }
        });
    }

    private void updateUIWithBackendData(User user) {
        // Name: prefer full_name, then name
        String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : (user.getName() != null ? user.getName() : "");
        userName.setText(displayName);
        // Optional fields: show value or clear so we don't show stale data
        userJobTitle.setText(user.getJobTitle() != null ? user.getJobTitle() : "");
        userPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        if (user.getDepartment() != null && user.getDepartment().getName() != null)
            userDept.setText(user.getDepartment().getName());
        else
            userDept.setText("");
        // Role badge from first role if present
        if (userRoleBadge != null) {
            if (user.getRoles() != null && !user.getRoles().isEmpty() && user.getRoles().get(0).getName() != null)
                userRoleBadge.setText(user.getRoles().get(0).getName());
            else
                userRoleBadge.setText("");
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Group 8");
            userEmail.setText(currentUser.getEmail());
            // TODO: Fetch full user info from backend including phone/job_title
            // For now, these are static in the mockup, but EditProfile will update them
            // locally once real API is used
        }
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> {
            SettingsMenuHelper.showSettingsMenu(ProfileActivity.this, v);
        });

        editProfileItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("name", userName.getText().toString());
            intent.putExtra("job_title", userJobTitle.getText().toString());
            intent.putExtra("phone", userPhone.getText().toString());
            editProfileLauncher.launch(intent);
        });

        changePasswordItem.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
        });

        manageDownloadsItem.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageDownloadsActivity.class));
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
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_profile);
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
