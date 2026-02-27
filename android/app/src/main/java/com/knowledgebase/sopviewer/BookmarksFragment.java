package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookmarksFragment extends Fragment {

    private RecyclerView recyclerBookmarks;
    private LinearLayout emptyState;
    private RecentAdapter bookmarkAdapter;
    private FirebaseAuth mAuth;
    private ProgressBar progressLoading;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bookmarks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        recyclerBookmarks = view.findViewById(R.id.recyclerBookmarks);
        emptyState = view.findViewById(R.id.emptyState);
        progressLoading = view.findViewById(R.id.progressLoading);

        view.findViewById(R.id.btnSettings)
                .setOnClickListener(v -> SettingsMenuHelper.showSettingsMenu(requireActivity(), v));

        fetchBookmarks();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Invalidate cache so bookmarks are fresh when the user comes back
        DataCache.getInstance().clear(DataCache.KEY_BOOKMARKS);
        fetchBookmarks();
    }

    private void fetchBookmarks() {
        if (progressLoading != null)
            progressLoading.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recyclerBookmarks.setVisibility(View.GONE);

        List<RecentDoc> cached = DataCache.getInstance().get(DataCache.KEY_BOOKMARKS);
        if (cached != null) {
            if (progressLoading != null)
                progressLoading.setVisibility(View.GONE);
            setupRecyclerView(cached, "cached");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                return;
            String token = "Bearer " + task.getResult().getToken();
            RetrofitClient.getApiService().getFavorites(token).enqueue(new Callback<List<Document>>() {
                @Override
                public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                    if (!isAdded())
                        return;
                    if (response.isSuccessful() && response.body() != null) {
                        List<RecentDoc> docs = new ArrayList<>();
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
                            docs.add(new RecentDoc(doc.getId(), doc.getTitle(), desc, date,
                                    R.drawable.file_logo, doc.getIsFavorite() > 0, fileUrl, fileType, cat, ver));
                        }
                        DataCache.getInstance().put(DataCache.KEY_BOOKMARKS, docs);
                        setupRecyclerView(docs, task.getResult().getToken());
                    }
                }

                @Override
                public void onFailure(Call<List<Document>> call, Throwable t) {
                    if (!isAdded())
                        return;
                    if (progressLoading != null)
                        progressLoading.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load bookmarks", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupRecyclerView(List<RecentDoc> docs, String token) {
        if (!isAdded())
            return;
        if (progressLoading != null)
            progressLoading.setVisibility(View.GONE);
        if (docs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerBookmarks.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerBookmarks.setVisibility(View.VISIBLE);
            if (bookmarkAdapter == null) {
                bookmarkAdapter = new RecentAdapter(docs, token);
                recyclerBookmarks.setLayoutManager(new LinearLayoutManager(requireContext()));
            }
            // Always set the adapter to the recycler
            recyclerBookmarks.setAdapter(bookmarkAdapter);
            bookmarkAdapter.notifyDataSetChanged();
        }
    }
}
