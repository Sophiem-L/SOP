package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerRecent;
    private RecyclerView recyclerFolders;
    private RecentAdapter recentAdapter;
    private FolderAdapter folderAdapter;
    private FirebaseAuth mAuth;
    private boolean isDataLoaded = false; // Prevent unnecessary refetches
    private List<RecentDoc> recentDocs = new ArrayList<>();
    private List<FolderDoc> folders = new ArrayList<>();

    private PagerSnapHelper snapHelper = new PagerSnapHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                // User not logged in, redirect to LoginActivity
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Firebase error: " + e.getMessage());
            Toast.makeText(this, "Firebase initialization failed", Toast.LENGTH_SHORT).show();
            // Redirect to login if Firebase fails
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        recyclerRecent = findViewById(R.id.recyclerRecent);
        recyclerFolders = findViewById(R.id.recyclerFolders);

        // Set layout managers once (not on every update)
        recyclerRecent.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerFolders.setLayoutManager(new LinearLayoutManager(this));

        // Attach SnapHelper
        snapHelper.attachToRecyclerView(recyclerRecent);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_home);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_bookmarks) {
                Intent intent = new Intent(MainActivity.this, BookmarksActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_home) {
                return true;
            } else if (id == R.id.navigation_profile) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_search) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_notifications) {
                Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        findViewById(R.id.fab).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateDocumentActivity.class));
        });

        fetchDataFromBackend();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only fetch if not already loaded (saves unnecessary API calls)
        if (!isDataLoaded) {
            fetchDataFromBackend();
        }
    }

    private void fetchDataFromBackend() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    // Load data in parallel (not sequential)
                    loadRecentContent(idToken);
                    loadCategories(idToken);
                    isDataLoaded = true;
                }
            });
        }
    }

    // Public method to force refresh (can be called from pull-to-refresh)
    public void refreshData() {
        isDataLoaded = false;
        recentDocs.clear();
        folders.clear();
        DataCache.getInstance().clearAll(); // Clear cache on manual refresh
        fetchDataFromBackend();
    }

    private void loadRecentContent(String token) {
        recentDocs.clear();

        // Fetch documents and articles in parallel for better performance
        final int[] completedCalls = {0};
        final int totalCalls = 2;

        // Fetch documents
        RetrofitClient.getApiService().getDocuments(token, null).enqueue(new Callback<List<Document>>() {
            @Override
            public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Document doc : response.body()) {
                        String description = doc.getDescription() != null ? doc.getDescription()
                                : "No description available";
                        String date = "Updated: "
                                + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A");
                        recentDocs.add(new RecentDoc(
                                doc.getId(),
                                doc.getTitle(),
                                description,
                                date,
                                R.drawable.file_logo,
                                doc.getIsFavorite() > 0));
                    }
                }
                completedCalls[0]++;
                if (completedCalls[0] == totalCalls) {
                    updateRecentAdapter(token);
                }
            }

            @Override
            public void onFailure(Call<List<Document>> call, Throwable t) {
                completedCalls[0]++;
                if (completedCalls[0] == totalCalls) {
                    updateRecentAdapter(token);
                }
            }
        });

        // Fetch articles in parallel
        RetrofitClient.getApiService().getArticles(token, null).enqueue(new Callback<List<Article>>() {
            @Override
            public void onResponse(Call<List<Article>> call, Response<List<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Article article : response.body()) {
                        recentDocs.add(new RecentDoc(
                                article.getId(),
                                article.getTitle(),
                                article.getContent(),
                                "Static Article",
                                R.drawable.file_logo,
                                false));
                    }
                }
                completedCalls[0]++;
                if (completedCalls[0] == totalCalls) {
                    updateRecentAdapter(token);
                }
            }

            @Override
            public void onFailure(Call<List<Article>> call, Throwable t) {
                completedCalls[0]++;
                if (completedCalls[0] == totalCalls) {
                    updateRecentAdapter(token);
                }
            }
        });
    }

    private void updateRecentAdapter(String token) {
        if (recentAdapter == null) {
            // Create adapter only once
            recentAdapter = new RecentAdapter(recentDocs, token);
            recyclerRecent.setAdapter(recentAdapter);
        } else {
            // Update existing adapter (more efficient)
            recentAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void loadCategories(String token) {
        android.util.Log.d("MainActivity", "Loading categories with token...");

        // Check cache first
        List<Category> cachedCategories = DataCache.getInstance().get(DataCache.KEY_MAIN_CATEGORIES);
        if (cachedCategories != null) {
            android.util.Log.d("MainActivity", "Using cached categories (" + cachedCategories.size() + ")");
            processCategoriesData(cachedCategories);
            return;
        }

        RetrofitClient.getApiService().getCategories(token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    android.util.Log.d("MainActivity", "Categories received: " + response.body().size());

                    // Cache the response
                    DataCache.getInstance().put(DataCache.KEY_MAIN_CATEGORIES, response.body());

                    processCategoriesData(response.body());
                } else {
                    android.util.Log.e("MainActivity", "Cats Error Response: " + response.code());
                    Toast.makeText(MainActivity.this, "Cats Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                android.util.Log.e("MainActivity", "Failed to load categories: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processCategoriesData(List<Category> categories) {
        try {
            folders.clear();
            int[] colors = {
                    R.color.bg_purple_light, // Policies
                    R.color.bg_pink_light, // HR Document
                    R.color.bg_green_light, // Security
                    R.color.bg_orange_light, // Finance
                    R.color.bg_blue_light // Management
            };
            int i = 0;
            for (Category cat : categories) {
                String updatedAt = cat.getUpdatedAt();
                String lastEdited = "N/A";
                if (updatedAt != null && updatedAt.length() >= 10) {
                    lastEdited = updatedAt.substring(0, 10);
                }

                folders.add(new FolderDoc(
                        cat.getName(),
                        cat.getDocumentsCount(),
                        "Last edited: " + lastEdited,
                        colors[i % colors.length]));
                i++;
            }

            if (folders.isEmpty()) {
                folders.add(
                        new FolderDoc("Policies", 32, "Last edited Feb 01, 2023", R.color.bg_purple_light));
                folders.add(new FolderDoc("HR Document", 10, "Last edited Mar 06, 2023",
                        R.color.bg_pink_light));
                folders.add(
                        new FolderDoc("Security", 5, "Last edited Jan 25, 2023", R.color.bg_green_light));
                folders.add(
                        new FolderDoc("Finance", 8, "Last edited Jan 15, 2023", R.color.bg_orange_light));
                folders.add(
                        new FolderDoc("Management", 12, "Last edited Feb 10, 2023", R.color.bg_blue_light));
            }

            updateFoldersAdapter();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error parsing categories: " + e.getMessage());
            Toast.makeText(MainActivity.this, "Data Error: Categories", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFoldersAdapter() {
        if (folderAdapter == null) {
            // Create adapter only once
            folderAdapter = new FolderAdapter(folders, MainActivity.this);
            recyclerFolders.setAdapter(folderAdapter);
        } else {
            // Update existing adapter (more efficient)
            folderAdapter.notifyDataSetChanged();
        }
    }
}
