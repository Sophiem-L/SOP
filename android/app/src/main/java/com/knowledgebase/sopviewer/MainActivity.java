package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;

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
    private boolean pendingRefresh = false;

    // Search related views
    private View topBarArea;
    private View homeContentArea;
    private ConstraintLayout searchResultContainer;
    private RecyclerView recyclerSearchResults;
    private TextView tvNoResults;
    private EditText searchEditText;
    private ImageView btnClearSearch;
    private SearchAdapter searchAdapter;
    private List<RecentDoc> searchResultsList = new ArrayList<>();

    // Pagination dots
    private View dot1, dot2;

    // Debounce for home search
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 400;

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

        findViewById(R.id.fab).setOnClickListener(
                v -> startActivityForResult(new Intent(MainActivity.this, CreateDocumentActivity.class), 1001));

        // Initialize Search Views
        topBarArea = findViewById(R.id.topBarArea);
        homeContentArea = findViewById(R.id.homeContentArea);
        searchResultContainer = findViewById(R.id.searchResultContainer);
        recyclerSearchResults = findViewById(R.id.recyclerSearchResults);
        tvNoResults = findViewById(R.id.tvNoResults);
        searchEditText = findViewById(R.id.searchEditText);
        btnClearSearch = findViewById(R.id.btnClearSearch);

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            SettingsMenuHelper.showSettingsMenu(MainActivity.this, v);
        });

        setupSearchLogic();

        // Initialize Pagination Dots
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);

        recyclerRecent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(recyclerRecent.getLayoutManager());
                    if (centerView != null) {
                        int position = recyclerRecent.getLayoutManager().getPosition(centerView);
                        updateDots(position);
                    }
                }
            }
        });

        findViewById(R.id.btnSeeAll).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        fetchDataFromBackend();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensure home is selected in bottom navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }

        if (pendingRefresh) {
            // A new document was just created — force a full refresh
            pendingRefresh = false;
            refreshData();
        } else if (!isDataLoaded) {
            // Initial load or previous load failed
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
        final int[] completedCalls = { 0 };
        final int totalCalls = 2;

        // Fetch documents
        RetrofitClient.getApiService().getDocuments(token, "", "recent", null).enqueue(new Callback<List<Document>>() {
            @Override
            public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Document doc : response.body()) {
                        String description = doc.getDescription() != null ? doc.getDescription()
                                : "No description available";
                        String date = "Updated: "
                                + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A");
                        String fileUrl = "";
                        String fileType = "";
                        String version = "1.0.0";
                        if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
                            DocumentVersion v = doc.getVersions().get(0);
                            fileUrl = v.getFileUrl() != null ? v.getFileUrl() : "";
                            fileType = v.getFileType() != null ? v.getFileType() : "";
                            version = v.getVersionNumber() != null ? v.getVersionNumber() : "1.0.0";
                        }
                        String category = (doc.getCategory() != null && doc.getCategory().getName() != null)
                                ? doc.getCategory().getName()
                                : "Uncategorized";
                        RecentDoc recentDoc = new RecentDoc(
                                doc.getId(), doc.getTitle(), description, date,
                                R.drawable.file_logo, doc.getIsFavorite() > 0,
                                fileUrl, fileType, category, version);
                        recentDoc.setStatus(doc.getStatus());
                        recentDocs.add(recentDoc);
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
        RetrofitClient.getApiService().getArticles(token, "", "recent").enqueue(new Callback<List<Article>>() {
            @Override
            public void onResponse(Call<List<Article>> call, Response<List<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Article article : response.body()) {
                        recentDocs.add(new RecentDoc(
                                article.getId(), article.getTitle(), article.getContent(),
                                "Article", R.drawable.file_logo, false,
                                "", "article", "Article", ""));
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
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Set a flag — let onResume (which fires right after) do the actual refresh.
            // Calling refreshData() here AND in onResume caused a race condition where
            // recentDocs was cleared twice mid-load and the new document never appeared.
            pendingRefresh = true;
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
                    for (Category c : response.body()) {
                        android.util.Log.d("MainActivity", "Cat: " + c.getName() + " Count: " + c.getDocumentsCount());
                    }

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
                if (cat.getDocumentsCount() == 0)
                    continue;

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

    private void setupSearchLogic() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any in-flight debounce before queueing the next
                if (searchDebounceRunnable != null)
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showHomeContent();
                } else {
                    showSearchResultContainer();
                    searchDebounceRunnable = () -> performHomeSearch(query);
                    searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_MS);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            showHomeContent();
        });

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchAdapter(searchResultsList);
        recyclerSearchResults.setAdapter(searchAdapter);
    }

    private void showHomeContent() {
        homeContentArea.setVisibility(View.VISIBLE);
        searchResultContainer.setVisibility(View.GONE);
        btnClearSearch.setVisibility(View.GONE);
    }

    private void showSearchResultContainer() {
        homeContentArea.setVisibility(View.GONE);
        searchResultContainer.setVisibility(View.VISIBLE);
        btnClearSearch.setVisibility(View.VISIBLE);
    }

    private void performHomeSearch(String query) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = "Bearer " + task.getResult().getToken();
                RetrofitClient.getApiService().getDocuments(token, query, "recent", null)
                        .enqueue(new Callback<List<Document>>() {
                            @Override
                            public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    searchResultsList.clear();
                                    for (Document doc : response.body()) {
                                        String fileUrl = "", fileType = "", ver = "1.0.0";
                                        if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
                                            DocumentVersion v = doc.getVersions().get(0);
                                            fileUrl = v.getFileUrl() != null ? v.getFileUrl() : "";
                                            fileType = v.getFileType() != null ? v.getFileType() : "";
                                            ver = v.getVersionNumber() != null ? v.getVersionNumber() : "1.0.0";
                                        }
                                        String cat = (doc.getCategory() != null && doc.getCategory().getName() != null)
                                                ? doc.getCategory().getName()
                                                : "Uncategorized";
                                        RecentDoc sr = new RecentDoc(
                                                doc.getId(), doc.getTitle(),
                                                doc.getDescription() != null ? doc.getDescription() : "No description",
                                                doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10)
                                                        : "N/A",
                                                R.drawable.file_logo, doc.getIsFavorite() > 0,
                                                fileUrl, fileType, cat, ver);
                                        sr.setStatus(doc.getStatus());
                                        searchResultsList.add(sr);
                                    }
                                    searchAdapter.notifyDataSetChanged();
                                    tvNoResults.setVisibility(searchResultsList.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            }

                            @Override
                            public void onFailure(Call<List<Document>> call, Throwable t) {
                                android.util.Log.e("MainActivity", "Search failed: " + t.getMessage());
                            }
                        });
            }
        });
    }

    private void updateDots(int position) {
        if (dot1 != null && dot2 != null) {
            if (position == 0) {
                dot1.setBackgroundResource(R.drawable.bg_dot_active);
                dot2.setBackgroundResource(R.drawable.bg_dot_inactive);
            } else {
                dot1.setBackgroundResource(R.drawable.bg_dot_inactive);
                dot2.setBackgroundResource(R.drawable.bg_dot_active);
            }
        }
    }
}
