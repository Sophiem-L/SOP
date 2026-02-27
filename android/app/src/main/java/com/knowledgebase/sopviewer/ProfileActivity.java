package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView userName, userEmail, userRoleBadge, userPhone, userJobTitle, userDept;
    private View btnSettings, editProfileItem, changePasswordItem, manageDownloadsItem,
            viewActivityLogItem, notificationPrefsItem, logoutItem;
    private ShapeableImageView imgAvatar;
    private View btnEditAvatar;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNav;

    // Returns from EditProfileActivity
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra("name")) userName.setText(data.getStringExtra("name"));
                    if (data.hasExtra("job_title")) userJobTitle.setText(data.getStringExtra("job_title"));
                    if (data.hasExtra("phone")) userPhone.setText(data.getStringExtra("phone"));
                    loadUserDataFromBackend();
                }
            });

    // Opens the system image picker; result goes to uploadAvatar()
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    // Show the selected image immediately (optimistic UI)
                    Glide.with(this).load(uri).circleCrop()
                            .placeholder(R.drawable.ic_profile).into(imgAvatar);
                    uploadAvatar(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(ProfileActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupBottomNavigation();
        loadUserData();
        loadUserDataFromBackend();
        setupClickListeners();
    }

    private void initViews() {
        btnSettings = findViewById(R.id.btnSettings);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        userPhone = findViewById(R.id.userPhone);
        userJobTitle = findViewById(R.id.userJobTitle);
        userDept = findViewById(R.id.userDept);
        userRoleBadge = findViewById(R.id.userRoleBadge);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnEditAvatar = findViewById(R.id.btnEditAvatar);

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

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "");
            userEmail.setText(currentUser.getEmail());
        }
    }

    private void loadUserDataFromBackend() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        currentUser.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = "Bearer " + task.getResult().getToken();
                RetrofitClient.getApiService().getProfile(token).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            updateUIWithBackendData(response.body());
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
        String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : (user.getName() != null ? user.getName() : "");
        userName.setText(displayName);
        userJobTitle.setText(user.getJobTitle() != null ? user.getJobTitle() : "");
        userPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        if (user.getDepartment() != null && user.getDepartment().getName() != null)
            userDept.setText(user.getDepartment().getName());
        else
            userDept.setText("");
        if (userRoleBadge != null) {
            if (user.getRoles() != null && !user.getRoles().isEmpty())
                userRoleBadge.setText(user.getRoles().get(0).getName());
            else
                userRoleBadge.setText("");
        }

        // Load saved avatar from backend
        String photoUrl = user.getProfilePhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            String resolved = photoUrl
                    .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                    .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/");
            Glide.with(this)
                    .load(resolved)
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(imgAvatar);
        }
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> SettingsMenuHelper.showSettingsMenu(ProfileActivity.this, v));

        // Tap either the avatar image or the pencil button to pick a new photo
        imgAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnEditAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        editProfileItem.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("name", userName.getText().toString());
            intent.putExtra("job_title", userJobTitle.getText().toString());
            intent.putExtra("phone", userPhone.getText().toString());
            editProfileLauncher.launch(intent);
        });

        changePasswordItem.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        manageDownloadsItem.setOnClickListener(v -> startActivity(new Intent(this, ManageDownloadsActivity.class)));
        viewActivityLogItem.setOnClickListener(v -> { /* TODO */ });
        notificationPrefsItem.setOnClickListener(v -> { /* TODO */ });

        logoutItem.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Reads the selected image URI into bytes and POSTs it to the backend
     * as multipart/form-data. Shows a toast on success/failure.
     */
    private void uploadAvatar(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        currentUser.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                return;
            }
            String token = "Bearer " + task.getResult().getToken();

            new Thread(() -> {
                try {
                    // Read bytes from the content URI
                    InputStream is = getContentResolver().openInputStream(imageUri);
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    byte[] chunk = new byte[4096];
                    int n;
                    while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
                    is.close();
                    byte[] imageBytes = buf.toByteArray();

                    String mimeType = getContentResolver().getType(imageUri);
                    if (mimeType == null) mimeType = "image/jpeg";
                    String fileName = mimeType.contains("png") ? "avatar.png" : "avatar.jpg";

                    RequestBody body = RequestBody.create(imageBytes, MediaType.parse(mimeType));
                    MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", fileName, body);

                    RetrofitClient.getApiService().uploadAvatar(token, part)
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> resp) {
                                    if (resp.isSuccessful()) {
                                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this,
                                                "Profile photo updated", Toast.LENGTH_SHORT).show());
                                    } else {
                                        runOnUiThread(() -> {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Upload failed (" + resp.code() + ")", Toast.LENGTH_SHORT).show();
                                            // Revert to default on failure
                                            Glide.with(ProfileActivity.this)
                                                    .load(R.drawable.ic_profile).circleCrop().into(imgAvatar);
                                        });
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this,
                                            "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
                                }
                            });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this,
                            "Failed to read image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_profile);
            NavBadgeHelper.updateNotificationBadge(bottomNav, SseManager.getInstance().getUnreadCount());
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }
}
