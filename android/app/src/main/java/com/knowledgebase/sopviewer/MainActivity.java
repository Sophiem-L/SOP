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

        private PagerSnapHelper snapHelper = new PagerSnapHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        recyclerRecent = findViewById(R.id.recyclerRecent);
        recyclerFolders = findViewById(R.id.recyclerFolders);

        // Attach SnapHelper
        snapHelper.attachToRecyclerView(recyclerRecent);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(
                R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_home);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_bookmarks) {
                startActivity(new Intent(MainActivity.this, BookmarksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_home) {
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
        fetchDataFromBackend();
    }

    private void fetchDataFromBackend() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    loadRecentContent(idToken);
                    loadCategories(idToken);
                }
            });
        }
    }

    private void loadRecentContent(String token) {
        final List<RecentDoc> recentDocs = new ArrayList<>();

        // Fetch new documents first
        RetrofitClient.getApiService().getDocuments(token).enqueue(new Callback<List<Document>>() {
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
                                doc.getIsFavorite() == 1));
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Docs Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                // Now fetch old articles
                loadOldArticles(recentDocs, token);
            }

            @Override
            public void onFailure(Call<List<Document>> call, Throwable t) {
                loadOldArticles(recentDocs, token);
            }
        });
    }

    private void loadOldArticles(final List<RecentDoc> recentDocs, final String token) {
        RetrofitClient.getApiService().getArticles(token).enqueue(new Callback<List<Article>>() {
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
                } else {
                    Toast.makeText(MainActivity.this, "Articles Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                updateRecentAdapter(recentDocs, token);
            }

            @Override
            public void onFailure(Call<List<Article>> call, Throwable t) {
                updateRecentAdapter(recentDocs, token);
            }
        });
    }

    private void updateRecentAdapter(List<RecentDoc> recentDocs, String token) {
        recentAdapter = new RecentAdapter(recentDocs, token);
        recyclerRecent.setLayoutManager(
                new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        recyclerRecent.setAdapter(recentAdapter);

        // Auto scroll removed - users can now scroll manually
    }

    
    @Override
    protected void onPause() {
        super.onPause();
    }

    private void loadCategories(String token) {
        RetrofitClient.getApiService().getCategories(token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FolderDoc> folders = new ArrayList<>();
                    int[] colors = {
                            R.color.bg_purple_light,
                            R.color.bg_pink_light,
                            R.color.bg_green_light,
                            R.color.bg_orange_light
                    };
                    int i = 0;
                    for (Category cat : response.body()) {
                        String lastEdited = cat.getUpdatedAt() != null ? cat.getUpdatedAt().substring(0, 10) : "N/A";
                        folders.add(new FolderDoc(
                                cat.getName(),
                                cat.getDocumentsCount(),
                                "Last edited: " + lastEdited,
                                colors[i % colors.length]));
                        i++;
                    }

                    // Add static ones only if empty to show the design intent
                    if (folders.isEmpty()) {
                        folders.add(new FolderDoc("Policies", 32, "Last edited Feb 01, 2023", R.color.bg_purple_light));
                        folders.add(
                                new FolderDoc("HR Document", 10, "Last edited Mar 06, 2023", R.color.bg_pink_light));
                        folders.add(new FolderDoc("Security", 5, "Last edited Jan 25, 2023", R.color.bg_green_light));
                    }

                    folderAdapter = new FolderAdapter(folders, MainActivity.this);
                    recyclerFolders.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                    recyclerFolders.setAdapter(folderAdapter);
                } else {
                    Toast.makeText(MainActivity.this, "Cats Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
