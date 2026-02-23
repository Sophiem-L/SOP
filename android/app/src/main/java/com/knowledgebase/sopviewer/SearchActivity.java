package com.knowledgebase.sopviewer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView searchResults;
    private SearchAdapter searchAdapter;
    private List<RecentDoc> searchResultsList;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private View emptyStateSearch;
    private final AtomicInteger pendingResponses = new AtomicInteger(0);

    // Debounce for search
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 400;

    // Filters
    private android.widget.LinearLayout quickFilterContainer;
    private TextView btnSortRecent, btnSortViewed;
    private String currentSort = "recent";
    private Integer currentCategoryId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_advanced);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            startActivity(new Intent(SearchActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        // Initialize views
        if (!initViews()) {
            return;
        }

        // Setup filter listeners
        setupFilters();

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup search functionality
        setupSearch();

        // Setup sorting
        setupSorting();

        // Load dynamic filters
        loadDynamicFilters();

        // Load initial data
        loadInitialData();
    }

    private boolean initViews() {
        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        bottomNav = findViewById(R.id.bottom_navigation);
        quickFilterContainer = findViewById(R.id.quickFilterContainer);

        // Debug: Check if views are found
        if (searchInput == null)
            android.util.Log.e("SearchActivity", "searchInput is null");
        if (searchResults == null)
            android.util.Log.e("SearchActivity", "searchResults is null");
        if (bottomNav == null)
            android.util.Log.e("SearchActivity", "bottomNav is null");

        // Return early if critical views are missing
        if (searchInput == null || searchResults == null || bottomNav == null) {
            android.util.Log.e("SearchActivity", "Critical views are missing, finishing activity");
            finish();
            return false;
        }

        searchResultsList = new ArrayList<>();
        searchAdapter = new SearchAdapter(searchResultsList);
        searchResults.setLayoutManager(new LinearLayoutManager(this));
        searchResults.setAdapter(searchAdapter);

        btnSortRecent = findViewById(R.id.btnSortRecent);
        btnSortViewed = findViewById(R.id.btnSortViewed);
        emptyStateSearch = findViewById(R.id.emptyStateSearch);

        findViewById(R.id.btnFilter).setOnClickListener(v -> {
            SettingsMenuHelper.showSettingsMenu(SearchActivity.this, v);
        });

        return true;
    }

    private void setupFilters() {
        // Dynamic filters setup is handled in loadDynamicFilters()
    }

    private void loadDynamicFilters() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = "Bearer " + task.getResult().getToken();

                // Check cache first
                List<Category> cachedCategories = DataCache.getInstance().get(DataCache.KEY_MAIN_CATEGORIES);
                if (cachedCategories != null) {
                    populateFilters(cachedCategories);
                } else {
                    RetrofitClient.getApiService().getCategories(token)
                            .enqueue(new retrofit2.Callback<List<Category>>() {
                                @Override
                                public void onResponse(retrofit2.Call<List<Category>> call,
                                        retrofit2.Response<List<Category>> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        DataCache.getInstance().put(DataCache.KEY_MAIN_CATEGORIES, response.body());
                                        populateFilters(response.body());
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<List<Category>> call, Throwable t) {
                                    android.util.Log.e("SearchActivity",
                                            "Failed to load categories: " + t.getMessage());
                                }
                            });
                }
            }
        });
    }

    private void populateFilters(List<Category> categories) {
        if (quickFilterContainer == null)
            return;
        quickFilterContainer.removeAllViews();

        for (Category cat : categories) {
            if (cat.getDocumentsCount() == 0)
                continue;

            TextView tv = new TextView(this);
            tv.setText(cat.getName());
            tv.setTag(cat.getId());
            tv.setBackgroundResource(R.drawable.bg_rounded_card);
            tv.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.bg_light)));
            tv.setTextColor(getResources().getColor(R.color.text_secondary));
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            tv.setTextSize(14f);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dpToPx(8), 0);
            tv.setLayoutParams(params);

            tv.setOnClickListener(v -> {
                int catId = cat.getId();
                if (currentCategoryId != null && currentCategoryId == catId) {
                    // Deselect: tap same chip again to clear filter
                    currentCategoryId = null;
                    resetChipHighlights();
                } else {
                    // Select this category
                    currentCategoryId = catId;
                    resetChipHighlights();
                    tv.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_blue)));
                    tv.setTextColor(getResources().getColor(R.color.white));
                }
                performSearch(searchInput.getText().toString().trim());
            });

            quickFilterContainer.addView(tv);
        }
    }

    private void resetChipHighlights() {
        for (int i = 0; i < quickFilterContainer.getChildCount(); i++) {
            View child = quickFilterContainer.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.bg_light)));
                ((TextView) child).setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private void setupBottomNavigation() {
        bottomNav.setSelectedItemId(R.id.navigation_search);
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
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_search) {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_search);
        }
    }

    private void setupSorting() {
        if (btnSortRecent != null) {
            btnSortRecent.setOnClickListener(v -> {
                currentSort = "recent";
                updateSortUI();
                performSearch(searchInput.getText().toString().trim());
            });
        }
        if (btnSortViewed != null) {
            btnSortViewed.setOnClickListener(v -> {
                currentSort = "viewed";
                updateSortUI();
                performSearch(searchInput.getText().toString().trim());
            });
        }
    }

    private void updateSortUI() {
        if (currentSort.equals("recent")) {
            btnSortRecent.setTextColor(getResources().getColor(R.color.brand_blue));
            btnSortViewed.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            btnSortRecent.setTextColor(getResources().getColor(R.color.text_secondary));
            btnSortViewed.setTextColor(getResources().getColor(R.color.brand_blue));
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any pending search before scheduling a new one
                if (debounceRunnable != null)
                    debounceHandler.removeCallbacks(debounceRunnable);
                String query = s.toString().trim();
                debounceRunnable = () -> {
                    if (query.isEmpty()) {
                        showDefaultResults();
                    } else {
                        performSearch(query);
                    }
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadInitialData() {
        // Show default search results
        android.util.Log.d("SearchActivity", "loadInitialData: showing default results");
        showDefaultResults();
    }

    private void showDefaultResults() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    loadSearchResults(idToken, null, currentSort);
                }
            });
        }
    }

    private void performSearch(String query) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    loadSearchResults(idToken, query, currentSort);
                }
            });
        }
    }

    private void loadSearchResults(String token, String query, String sort) {
        searchResultsList.clear();
        searchAdapter.notifyDataSetChanged();
        pendingResponses.set(3); // Documents, Articles, SOPs
        updateEmptyState(false); // Hide while loading

        // Fetch documents
        RetrofitClient.getApiService().getDocuments(token, query, sort, currentCategoryId)
                .enqueue(new retrofit2.Callback<List<Document>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Document>> call,
                            retrofit2.Response<List<Document>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Document doc : response.body()) {
                                String description = doc.getDescription() != null ? doc.getDescription()
                                        : "No description available";
                                String date = "Updated: "
                                        + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A");
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
                                searchResultsList.add(new RecentDoc(
                                        doc.getId(), doc.getTitle(), description, date,
                                        R.drawable.file_logo, doc.getIsFavorite() > 0,
                                        fileUrl, fileType, cat, ver));
                            }
                            searchAdapter.notifyDataSetChanged();
                        }
                        decrementPendingResponses();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<Document>> call, Throwable t) {
                        android.util.Log.e("SearchActivity", "Failed to load documents: " + t.getMessage());
                        decrementPendingResponses();
                    }
                });

        // Fetch articles
        RetrofitClient.getApiService().getArticles(token, query, sort).enqueue(new retrofit2.Callback<List<Article>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Article>> call, retrofit2.Response<List<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Article article : response.body()) {
                        searchResultsList.add(new RecentDoc(
                                article.getId(), article.getTitle(), article.getContent(),
                                "Article", R.drawable.file_logo, false,
                                "", "article", "Article", ""));
                    }
                    searchAdapter.notifyDataSetChanged();
                }
                decrementPendingResponses();
            }

            @Override
            public void onFailure(retrofit2.Call<List<Article>> call, Throwable t) {
                android.util.Log.e("SearchActivity", "Failed to load articles: " + t.getMessage());
                decrementPendingResponses();
            }
        });

        // Fetch sops
        RetrofitClient.getApiService().getSops(token, query, sort).enqueue(new retrofit2.Callback<List<Sop>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Sop>> call, retrofit2.Response<List<Sop>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Sop sop : response.body()) {
                        searchResultsList.add(new RecentDoc(
                                sop.getId(), sop.getTitle(), sop.getSteps(),
                                "SOP", R.drawable.file_logo, false,
                                "", "sop", "SOP", ""));
                    }
                    searchAdapter.notifyDataSetChanged();
                }
                decrementPendingResponses();
            }

            @Override
            public void onFailure(retrofit2.Call<List<Sop>> call, Throwable t) {
                android.util.Log.e("SearchActivity", "Failed to load sops: " + t.getMessage());
                decrementPendingResponses();
            }
        });
    }

    private void decrementPendingResponses() {
        if (pendingResponses.decrementAndGet() <= 0) {
            updateEmptyState(searchResultsList.isEmpty());
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyStateSearch != null) {
            emptyStateSearch.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // Navigate to home when back is pressed
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
