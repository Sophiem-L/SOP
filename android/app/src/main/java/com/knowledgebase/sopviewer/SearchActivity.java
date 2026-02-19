package com.knowledgebase.sopviewer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView searchResults;
    private SearchAdapter searchAdapter;
    private List<RecentDoc> searchResultsList;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    // Filters
    private TextView filterHrPolicies, filterItGuidelines, filterFinance, filterCc;

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

        // Load initial data
        loadInitialData();
    }

    private boolean initViews() {
        searchInput = findViewById(R.id.searchInput);
        searchResults = findViewById(R.id.searchResults);
        bottomNav = findViewById(R.id.bottom_navigation);

        filterHrPolicies = findViewById(R.id.filterHrPolicies);
        filterItGuidelines = findViewById(R.id.filterItGuidelines);
        filterFinance = findViewById(R.id.filterFinance);
        filterCc = findViewById(R.id.filterCc);

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
        return true;
    }

    private void setupFilters() {
        android.view.View.OnClickListener filterListener = v -> {
            if (v instanceof TextView) {
                String filterText = ((TextView) v).getText().toString();
                searchInput.setText(filterText);
                performSearch(filterText);
            }
        };

        if (filterHrPolicies != null)
            filterHrPolicies.setOnClickListener(filterListener);
        if (filterItGuidelines != null)
            filterItGuidelines.setOnClickListener(filterListener);
        if (filterFinance != null)
            filterFinance.setOnClickListener(filterListener);
        if (filterCc != null)
            filterCc.setOnClickListener(filterListener);
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

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Show default results when search is empty
                    showDefaultResults();
                } else {
                    // Perform search with query
                    performSearch(query);
                }
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
                    loadSearchResults(idToken, null);
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
                    loadSearchResults(idToken, query);
                }
            });
        }
    }

    private void loadSearchResults(String token, String query) {
        searchResultsList.clear();

        // Fetch documents
        RetrofitClient.getApiService().getDocuments(token, query).enqueue(new retrofit2.Callback<List<Document>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Document>> call, retrofit2.Response<List<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Document doc : response.body()) {
                        String description = doc.getDescription() != null ? doc.getDescription()
                                : "No description available";
                        String date = "Updated: "
                                + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A");
                        searchResultsList.add(new RecentDoc(
                                doc.getId(),
                                doc.getTitle(),
                                description,
                                date,
                                R.drawable.file_logo,
                                doc.getIsFavorite() > 0));
                    }
                    searchAdapter.notifyDataSetChanged();
                } else {
                    android.util.Log.e("SearchActivity", "Documents Error: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Document>> call, Throwable t) {
                android.util.Log.e("SearchActivity", "Failed to load documents: " + t.getMessage());
            }
        });

        // Fetch articles
        RetrofitClient.getApiService().getArticles(token, query).enqueue(new retrofit2.Callback<List<Article>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Article>> call, retrofit2.Response<List<Article>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Article article : response.body()) {
                        searchResultsList.add(new RecentDoc(
                                article.getId(),
                                article.getTitle(),
                                article.getContent(),
                                "Article",
                                R.drawable.file_logo,
                                false));
                    }
                    searchAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Article>> call, Throwable t) {
                android.util.Log.e("SearchActivity", "Failed to load articles: " + t.getMessage());
            }
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
