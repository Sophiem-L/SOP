package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookmarksActivity extends AppCompatActivity {

    private RecyclerView recyclerBookmarks;
    private LinearLayout emptyState;
    private RecentAdapter bookmarkAdapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is authenticated
        if (currentUser == null) {
            startActivity(new Intent(BookmarksActivity.this, WelcomeActivity.class));
            finish();
            return;
        }

        recyclerBookmarks = findViewById(R.id.recyclerBookmarks);
        emptyState = findViewById(R.id.emptyState);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_bookmarks);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.navigation_bookmarks) {
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

        fetchBookmarks();
    }

    private void fetchBookmarks() {
        // Check cache first
        List<RecentDoc> cachedBookmarks = DataCache.getInstance().get(DataCache.KEY_BOOKMARKS);
        if (cachedBookmarks != null) {
            android.util.Log.d("BookmarksActivity", "Using cached bookmarks (" + cachedBookmarks.size() + ")");
            setupRecyclerView(cachedBookmarks, "cached");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    RetrofitClient.getApiService().getFavorites(idToken).enqueue(new Callback<List<Document>>() {
                        @Override
                        public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                List<RecentDoc> bookmarkDocs = new ArrayList<>();
                                for (Document doc : response.body()) {
                                    String description = doc.getDescription() != null ? doc.getDescription()
                                            : "No description available";
                                    String date = "Updated: "
                                            + (doc.getUpdatedAt() != null ? doc.getUpdatedAt().substring(0, 10)
                                                    : "N/A");
                                    bookmarkDocs.add(new RecentDoc(
                                            doc.getId(),
                                            doc.getTitle(),
                                            description,
                                            date,
                                            R.drawable.file_logo,
                                            doc.getIsFavorite() > 0));
                                }

                                // Cache the bookmarks
                                DataCache.getInstance().put(DataCache.KEY_BOOKMARKS, bookmarkDocs);

                                setupRecyclerView(bookmarkDocs, task.getResult().getToken());
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Document>> call, Throwable t) {
                            Toast.makeText(BookmarksActivity.this, "Failed to load bookmarks", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
            });
        }
    }

    private void setupRecyclerView(List<RecentDoc> docs, String token) {
        if (docs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerBookmarks.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerBookmarks.setVisibility(View.VISIBLE);
            bookmarkAdapter = new RecentAdapter(docs, token);
            recyclerBookmarks.setLayoutManager(new LinearLayoutManager(this));
            recyclerBookmarks.setAdapter(bookmarkAdapter);
        }
    }
}
