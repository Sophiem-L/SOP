package com.knowledgebase.sopviewer;

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

public class AllDocumentsActivity extends AppCompatActivity {

    private EditText searchInput;
    private RecyclerView recyclerDocuments;
    private SearchAdapter adapter;
    private List<RecentDoc> documentList;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private View emptyState;
    private LinearLayout quickFilterContainer;
    private TextView btnSortRecent, btnSortViewed;

    private String currentSort = "recent";
    private Integer currentCategoryId = null;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_documents);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        initViews();
        setupSearch();
        setupSorting();
        loadDynamicFilters();
        loadDocuments(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            NavBadgeHelper.updateNotificationBadge(bottomNav, SseManager.getInstance().getUnreadCount());
        }
    }

    private void initViews() {
        searchInput = findViewById(R.id.searchInput);
        recyclerDocuments = findViewById(R.id.recyclerDocuments);
        bottomNav = findViewById(R.id.bottom_navigation);
        quickFilterContainer = findViewById(R.id.quickFilterContainer);
        emptyState = findViewById(R.id.emptyState);
        btnSortRecent = findViewById(R.id.btnSortRecent);
        btnSortViewed = findViewById(R.id.btnSortViewed);

        documentList = new ArrayList<>();
        adapter = new SearchAdapter(documentList);
        recyclerDocuments.setLayoutManager(new LinearLayoutManager(this));
        recyclerDocuments.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        bottomNav.setSelectedItemId(R.id.navigation_home);
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

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                String query = s.toString().trim();
                debounceRunnable = () -> loadDocuments(query.isEmpty() ? null : query);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
        });
    }

    private void setupSorting() {
        if (btnSortRecent != null) {
            btnSortRecent.setOnClickListener(v -> {
                currentSort = "recent";
                updateSortUI();
                loadDocuments(searchInput.getText().toString().trim());
            });
        }
        if (btnSortViewed != null) {
            btnSortViewed.setOnClickListener(v -> {
                currentSort = "viewed";
                updateSortUI();
                loadDocuments(searchInput.getText().toString().trim());
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

    private void loadDynamicFilters() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();

            List<Category> cached = DataCache.getInstance().get(DataCache.KEY_MAIN_CATEGORIES);
            if (cached != null) {
                populateFilters(cached);
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
                                android.util.Log.e("AllDocuments", "Failed to load categories: " + t.getMessage());
                            }
                        });
            }
        });
    }

    private void populateFilters(List<Category> categories) {
        if (quickFilterContainer == null) return;
        quickFilterContainer.removeAllViews();

        for (Category cat : categories) {
            if (cat.getDocumentsCount() == 0) continue;

            TextView tv = new TextView(this);
            tv.setText(cat.getName());
            tv.setTag(cat.getId());
            tv.setBackgroundResource(R.drawable.bg_rounded_card);
            tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.bg_light)));
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
                    currentCategoryId = null;
                    resetChipHighlights();
                } else {
                    currentCategoryId = catId;
                    resetChipHighlights();
                    tv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.brand_blue)));
                    tv.setTextColor(getResources().getColor(R.color.white));
                }
                loadDocuments(searchInput.getText().toString().trim());
            });

            quickFilterContainer.addView(tv);
        }
    }

    private void resetChipHighlights() {
        for (int i = 0; i < quickFilterContainer.getChildCount(); i++) {
            View child = quickFilterContainer.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        getResources().getColor(R.color.bg_light)));
                ((TextView) child).setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
    }

    private void loadDocuments(String query) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();
            fetchDocuments(token, query);
        });
    }

    private void fetchDocuments(String token, String query) {
        documentList.clear();
        adapter.notifyDataSetChanged();
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        RetrofitClient.getApiService()
                .getDocuments(token, query != null ? query : "", currentSort, currentCategoryId)
                .enqueue(new retrofit2.Callback<List<Document>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Document>> call,
                            retrofit2.Response<List<Document>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Document doc : response.body()) {
                                String description = doc.getDescription() != null
                                        ? doc.getDescription() : "No description available";
                                String date = "Updated: " + (doc.getUpdatedAt() != null
                                        ? doc.getUpdatedAt().substring(0, 10) : "N/A");
                                String fileUrl = "", fileType = "", ver = "1.0.0";
                                if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
                                    DocumentVersion v = doc.getVersions().get(0);
                                    fileUrl = v.getFileUrl() != null ? v.getFileUrl() : "";
                                    fileType = v.getFileType() != null ? v.getFileType() : "";
                                    ver = v.getVersionNumber() != null ? v.getVersionNumber() : "1.0.0";
                                }
                                String cat = (doc.getCategory() != null && doc.getCategory().getName() != null)
                                        ? doc.getCategory().getName() : "Uncategorized";
                                RecentDoc rd = new RecentDoc(
                                        doc.getId(), doc.getTitle(), description, date,
                                        R.drawable.file_logo, doc.getIsFavorite() > 0,
                                        fileUrl, fileType, cat, ver);
                                rd.setStatus(doc.getStatus());
                                documentList.add(rd);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (emptyState != null) {
                            emptyState.setVisibility(documentList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<Document>> call, Throwable t) {
                        android.util.Log.e("AllDocuments", "Failed to load documents: " + t.getMessage());
                        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
