package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerRecent;
    private RecyclerView recyclerFolders;
    private RecentAdapter recentAdapter;
    private FolderAdapter folderAdapter;
    private FirebaseAuth mAuth;
    private boolean isDataLoaded = false;
    private final List<RecentDoc> recentDocs = new ArrayList<>();
    private final List<FolderDoc> folders = new ArrayList<>();

    private final PagerSnapHelper snapHelper = new PagerSnapHelper();

    // Search views
    private View homeContentArea;
    private ConstraintLayout searchResultContainer;
    private RecyclerView recyclerSearchResults;
    private TextView tvNoResults;
    private EditText searchEditText;
    private ImageView btnClearSearch;
    private SearchAdapter searchAdapter;
    private final List<RecentDoc> searchResultsList = new ArrayList<>();

    // Pagination dots
    private View dot1, dot2;

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;
    private static final long SEARCH_DEBOUNCE_MS = 400;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        recyclerRecent = view.findViewById(R.id.recyclerRecent);
        recyclerFolders = view.findViewById(R.id.recyclerFolders);
        recyclerRecent.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recyclerFolders.setLayoutManager(new LinearLayoutManager(requireContext()));
        snapHelper.attachToRecyclerView(recyclerRecent);

        view.findViewById(R.id.btnSeeAll)
                .setOnClickListener(v -> startActivity(new Intent(requireContext(), AllDocumentsActivity.class)));

        view.findViewById(R.id.btnSettings)
                .setOnClickListener(v -> SettingsMenuHelper.showSettingsMenu(requireActivity(), v));

        homeContentArea = view.findViewById(R.id.homeContentArea);
        searchResultContainer = view.findViewById(R.id.searchResultContainer);
        recyclerSearchResults = view.findViewById(R.id.recyclerSearchResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);
        searchEditText = view.findViewById(R.id.searchEditText);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        dot1 = view.findViewById(R.id.dot1);
        dot2 = view.findViewById(R.id.dot2);

        recyclerRecent.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snap = snapHelper.findSnapView(rv.getLayoutManager());
                    if (snap != null)
                        updateDots(rv.getLayoutManager().getPosition(snap));
                }
            }
        });

        setupSearchLogic();
        // fetchData() is also called in onResume if !isDataLoaded,
        // so we don't need it here unless we want to trigger it immediately.
        // However, keeping it in onResume is more robust for tab switching.
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity host = (MainActivity) requireActivity();
            if (host.pendingHomeRefresh) {
                host.pendingHomeRefresh = false;
                refreshData();
            } else if (!isDataLoaded) {
                fetchData();
            } else {
                // IMPORTANT: If data is already loaded (isDataLoaded == true),
                // the View was still recreated by the navigation component.
                // We MUST re-attach the adapters to the new RecyclerView instances.
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.getIdToken(false).addOnCompleteListener(task -> {
                        if (task.isSuccessful() && isAdded()) {
                            String token = "Bearer " + task.getResult().getToken();
                            updateRecentAdapter(token);
                            updateFoldersAdapter();
                        }
                    });
                }
            }
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void fetchData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;
        SseManager.getInstance().start(user);
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = "Bearer " + task.getResult().getToken();
                loadRecentContent(token);
                loadCategories(token);
                isDataLoaded = true;
            }
        });
    }

    public void refreshData() {
        isDataLoaded = false;
        recentDocs.clear();
        folders.clear();
        DataCache.getInstance().clearAll();
        fetchData();
    }

    private void loadRecentContent(String token) {
        recentDocs.clear();
        final int[] done = { 0 };

        RetrofitClient.getApiService().getDocuments(token, "", "recent", null)
                .enqueue(new Callback<List<Document>>() {
                    @Override
                    public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Document doc : response.body()) {
                                String desc = doc.getDescription() != null ? doc.getDescription() : "No description";
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
                                RecentDoc rd = new RecentDoc(doc.getId(), doc.getTitle(), desc, date,
                                        R.drawable.file_logo, doc.getIsFavorite() > 0, fileUrl, fileType, cat, ver);
                                rd.setStatus(doc.getStatus());
                                recentDocs.add(rd);
                            }
                        }
                        if (++done[0] == 2)
                            updateRecentAdapter(token);
                    }

                    @Override
                    public void onFailure(Call<List<Document>> call, Throwable t) {
                        if (++done[0] == 2)
                            updateRecentAdapter(token);
                    }
                });

        RetrofitClient.getApiService().getArticles(token, "", "recent")
                .enqueue(new Callback<List<Article>>() {
                    @Override
                    public void onResponse(Call<List<Article>> call, Response<List<Article>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (Article a : response.body()) {
                                recentDocs.add(new RecentDoc(a.getId(), a.getTitle(), a.getContent(),
                                        "Article", R.drawable.file_logo, false, "", "article", "Article", ""));
                            }
                        }
                        if (++done[0] == 2)
                            updateRecentAdapter(token);
                    }

                    @Override
                    public void onFailure(Call<List<Article>> call, Throwable t) {
                        if (++done[0] == 2)
                            updateRecentAdapter(token);
                    }
                });
    }

    private void updateRecentAdapter(String token) {
        if (!isAdded())
            return;
        if (recentAdapter == null) {
            recentAdapter = new RecentAdapter(recentDocs, token);
        }
        // Always set the adapter to the recycler, because the recycler instance
        // is new every time the fragment view is recreated.
        recyclerRecent.setAdapter(recentAdapter);
        recentAdapter.notifyDataSetChanged();
    }

    private void loadCategories(String token) {
        List<Category> cached = DataCache.getInstance().get(DataCache.KEY_MAIN_CATEGORIES);
        if (cached != null) {
            processCategoriesData(cached);
            return;
        }

        RetrofitClient.getApiService().getCategories(token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DataCache.getInstance().put(DataCache.KEY_MAIN_CATEGORIES, response.body());
                    processCategoriesData(response.body());
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (isAdded())
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processCategoriesData(List<Category> categories) {
        if (!isAdded())
            return;
        try {
            folders.clear();
            int[] colors = { R.color.bg_purple_light, R.color.bg_pink_light, R.color.bg_green_light,
                    R.color.bg_orange_light, R.color.bg_blue_light };
            int i = 0;
            for (Category cat : categories) {
                if (cat.getDocumentsCount() == 0)
                    continue;
                String updatedAt = cat.getUpdatedAt();
                String last = (updatedAt != null && updatedAt.length() >= 10) ? updatedAt.substring(0, 10) : "N/A";
                folders.add(new FolderDoc(cat.getName(), cat.getDocumentsCount(),
                        "Last edited: " + last, colors[i++ % colors.length]));
            }
            updateFoldersAdapter();
        } catch (Exception e) {
            if (isAdded())
                Toast.makeText(requireContext(), "Data Error: Categories", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFoldersAdapter() {
        if (!isAdded())
            return;
        if (folderAdapter == null) {
            folderAdapter = new FolderAdapter(folders, requireActivity());
        }
        // Always set the adapter to the recycler
        recyclerFolders.setAdapter(folderAdapter);
        folderAdapter.notifyDataSetChanged();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private void setupSearchLogic() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchDebounceRunnable != null)
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showHomeContent();
                } else {
                    showSearchResults();
                    searchDebounceRunnable = () -> performSearch(query);
                    searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_MS);
                }
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
            showHomeContent();
        });

        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchAdapter = new SearchAdapter(searchResultsList);
        recyclerSearchResults.setAdapter(searchAdapter);
    }

    private void showHomeContent() {
        homeContentArea.setVisibility(View.VISIBLE);
        searchResultContainer.setVisibility(View.GONE);
        btnClearSearch.setVisibility(View.GONE);
    }

    private void showSearchResults() {
        homeContentArea.setVisibility(View.GONE);
        searchResultContainer.setVisibility(View.VISIBLE);
        btnClearSearch.setVisibility(View.VISIBLE);
    }

    private void performSearch(String query) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                return;
            String token = "Bearer " + task.getResult().getToken();
            RetrofitClient.getApiService().getDocuments(token, query, "recent", null)
                    .enqueue(new Callback<List<Document>>() {
                        @Override
                        public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                            if (!isAdded())
                                return;
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
                                    RecentDoc sr = new RecentDoc(doc.getId(), doc.getTitle(),
                                            doc.getDescription() != null ? doc.getDescription() : "No description",
                                            doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10) : "N/A",
                                            R.drawable.file_logo, doc.getIsFavorite() > 0, fileUrl, fileType, cat, ver);
                                    sr.setStatus(doc.getStatus());
                                    searchResultsList.add(sr);
                                }
                                searchAdapter.notifyDataSetChanged();
                                tvNoResults.setVisibility(searchResultsList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Document>> call, Throwable t) {
                            android.util.Log.e("HomeFragment", "Search failed: " + t.getMessage());
                        }
                    });
        });
    }

    private void updateDots(int position) {
        if (dot1 == null || dot2 == null)
            return;
        if (position == 0) {
            dot1.setBackgroundResource(R.drawable.bg_dot_active);
            dot2.setBackgroundResource(R.drawable.bg_dot_inactive);
        } else {
            dot1.setBackgroundResource(R.drawable.bg_dot_inactive);
            dot2.setBackgroundResource(R.drawable.bg_dot_active);
        }
    }
}
