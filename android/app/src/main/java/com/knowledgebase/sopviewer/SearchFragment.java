package com.knowledgebase.sopviewer;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView searchResults;
    private SearchAdapter searchAdapter;
    private List<RecentDoc> searchResultsList;
    private FirebaseAuth mAuth;
    private View emptyStateSearch;
    private LinearLayout quickFilterContainer;
    private TextView btnSortRecent, btnSortViewed;

    private String currentSort = "recent";
    private Integer currentCategoryId = null;
    private final AtomicInteger pendingResponses = new AtomicInteger(0);

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 400;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        searchInput = view.findViewById(R.id.searchInput);
        searchResults = view.findViewById(R.id.searchResults);
        quickFilterContainer = view.findViewById(R.id.quickFilterContainer);
        emptyStateSearch = view.findViewById(R.id.emptyStateSearch);
        btnSortRecent = view.findViewById(R.id.btnSortRecent);
        btnSortViewed = view.findViewById(R.id.btnSortViewed);

        view.findViewById(R.id.btnFilter).setOnClickListener(v ->
                SettingsMenuHelper.showSettingsMenu(requireActivity(), v));

        searchResultsList = new ArrayList<>();
        searchAdapter = new SearchAdapter(searchResultsList);
        searchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchResults.setAdapter(searchAdapter);

        setupSorting();
        setupSearch();
        loadDynamicFilters();
        showDefaultResults();
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
                        .enqueue(new Callback<List<Category>>() {
                            @Override
                            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    DataCache.getInstance().put(DataCache.KEY_MAIN_CATEGORIES, response.body());
                                    if (isAdded()) populateFilters(response.body());
                                }
                            }

                            @Override
                            public void onFailure(Call<List<Category>> call, Throwable t) {}
                        });
            }
        });
    }

    private void populateFilters(List<Category> categories) {
        if (quickFilterContainer == null || !isAdded()) return;
        quickFilterContainer.removeAllViews();
        for (Category cat : categories) {
            if (cat.getDocumentsCount() == 0) continue;
            TextView tv = new TextView(requireContext());
            tv.setText(cat.getName());
            tv.setTag(cat.getId());
            tv.setBackgroundResource(R.drawable.bg_rounded_card);
            tv.setBackgroundTintList(ColorStateList.valueOf(
                    requireContext().getResources().getColor(R.color.bg_light)));
            tv.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            tv.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
            tv.setTextSize(14f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
                    tv.setBackgroundTintList(ColorStateList.valueOf(
                            requireContext().getResources().getColor(R.color.brand_blue)));
                    tv.setTextColor(requireContext().getResources().getColor(R.color.white));
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
                ((TextView) child).setBackgroundTintList(ColorStateList.valueOf(
                        requireContext().getResources().getColor(R.color.bg_light)));
                ((TextView) child).setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            }
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
        if (!isAdded()) return;
        if ("recent".equals(currentSort)) {
            btnSortRecent.setTextColor(requireContext().getResources().getColor(R.color.brand_blue));
            btnSortViewed.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
        } else {
            btnSortRecent.setTextColor(requireContext().getResources().getColor(R.color.text_secondary));
            btnSortViewed.setTextColor(requireContext().getResources().getColor(R.color.brand_blue));
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                String query = s.toString().trim();
                debounceRunnable = () -> {
                    if (query.isEmpty()) showDefaultResults();
                    else performSearch(query);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
        });
    }

    private void showDefaultResults() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                loadSearchResults("Bearer " + task.getResult().getToken(), null, currentSort);
        });
    }

    private void performSearch(String query) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful())
                loadSearchResults("Bearer " + task.getResult().getToken(), query, currentSort);
        });
    }

    private void loadSearchResults(String token, String query, String sort) {
        searchResultsList.clear();
        searchAdapter.notifyDataSetChanged();
        pendingResponses.set(3);
        updateEmptyState(false);

        RetrofitClient.getApiService().getDocuments(token, query, sort, currentCategoryId)
                .enqueue(new Callback<List<Document>>() {
                    @Override
                    public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            for (Document doc : response.body()) {
                                String desc = doc.getDescription() != null ? doc.getDescription() : "No description";
                                String date = "Updated: " + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A");
                                String fileUrl = "", fileType = "", ver = "1.0.0";
                                if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
                                    DocumentVersion v = doc.getVersions().get(0);
                                    fileUrl = v.getFileUrl() != null ? v.getFileUrl() : "";
                                    fileType = v.getFileType() != null ? v.getFileType() : "";
                                    ver = v.getVersionNumber() != null ? v.getVersionNumber() : "1.0.0";
                                }
                                String cat = (doc.getCategory() != null && doc.getCategory().getName() != null)
                                        ? doc.getCategory().getName() : "Uncategorized";
                                searchResultsList.add(new RecentDoc(doc.getId(), doc.getTitle(), desc, date,
                                        R.drawable.file_logo, doc.getIsFavorite() > 0, fileUrl, fileType, cat, ver));
                            }
                            searchAdapter.notifyDataSetChanged();
                        }
                        decrementPending();
                    }

                    @Override
                    public void onFailure(Call<List<Document>> call, Throwable t) { decrementPending(); }
                });

        RetrofitClient.getApiService().getArticles(token, query, sort)
                .enqueue(new Callback<List<Article>>() {
                    @Override
                    public void onResponse(Call<List<Article>> call, Response<List<Article>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            for (Article a : response.body()) {
                                searchResultsList.add(new RecentDoc(a.getId(), a.getTitle(), a.getContent(),
                                        "Article", R.drawable.file_logo, false, "", "article", "Article", ""));
                            }
                            searchAdapter.notifyDataSetChanged();
                        }
                        decrementPending();
                    }

                    @Override
                    public void onFailure(Call<List<Article>> call, Throwable t) { decrementPending(); }
                });

        RetrofitClient.getApiService().getSops(token, query, sort)
                .enqueue(new Callback<List<Sop>>() {
                    @Override
                    public void onResponse(Call<List<Sop>> call, Response<List<Sop>> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            for (Sop sop : response.body()) {
                                searchResultsList.add(new RecentDoc(sop.getId(), sop.getTitle(), sop.getSteps(),
                                        "SOP", R.drawable.file_logo, false, "", "sop", "SOP", ""));
                            }
                            searchAdapter.notifyDataSetChanged();
                        }
                        decrementPending();
                    }

                    @Override
                    public void onFailure(Call<List<Sop>> call, Throwable t) { decrementPending(); }
                });
    }

    private void decrementPending() {
        if (pendingResponses.decrementAndGet() <= 0) updateEmptyState(searchResultsList.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyStateSearch != null)
            emptyStateSearch.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
