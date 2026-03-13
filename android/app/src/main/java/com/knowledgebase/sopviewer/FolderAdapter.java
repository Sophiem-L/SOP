package com.knowledgebase.sopviewer;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private final List<FolderDoc> folders;
    private final Context context;
    private String token;

    private final Set<Integer> expandedPositions = new HashSet<>();
    private final Map<Integer, List<RecentDoc>> loadedDocs = new HashMap<>();
    private final Set<Integer> loadingPositions = new HashSet<>();

    public FolderAdapter(List<FolderDoc> folders, Context context, String token) {
        this.folders = folders;
        this.context = context;
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderDoc folder = folders.get(position);
        holder.title.setText(folder.getTitle());
        holder.count.setText(folder.getDocCount() + " Documents");
        holder.date.setText(folder.getLastEdited());
        holder.container.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(context, folder.getColorResId())));
        holder.container.setClipToOutline(true);

        boolean expanded = expandedPositions.contains(position);
        boolean loading = loadingPositions.contains(position);
        List<RecentDoc> docs = loadedDocs.get(position);

        // Bind without animation
        applyDropdownState(holder, expanded, loading, docs, false);

        holder.container.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_ID) return;

            boolean isExpanded = expandedPositions.contains(pos);
            if (isExpanded) {
                expandedPositions.remove(pos);
            } else {
                expandedPositions.add(pos);
                if (!loadedDocs.containsKey(pos) && !loadingPositions.contains(pos)) {
                    fetchDocs(pos, folders.get(pos));
                }
            }
            applyDropdownState(holder,
                    expandedPositions.contains(pos),
                    loadingPositions.contains(pos),
                    loadedDocs.get(pos),
                    true);
        });
    }

    private void applyDropdownState(ViewHolder holder, boolean expanded,
            boolean loading, List<RecentDoc> docs, boolean animate) {
        if (animate) {
            TransitionManager.beginDelayedTransition(
                    (ViewGroup) holder.itemView,
                    new AutoTransition().setDuration(220));
        }

        holder.chevron.animate().rotation(expanded ? 90f : 0f).setDuration(220).start();

        if (expanded) {
            holder.divider.setVisibility(View.VISIBLE);
            holder.docListContainer.setVisibility(View.VISIBLE);
            if (loading) {
                holder.loadingDocs.setVisibility(View.VISIBLE);
                holder.docsInner.setVisibility(View.GONE);
            } else {
                holder.loadingDocs.setVisibility(View.GONE);
                holder.docsInner.setVisibility(View.VISIBLE);
                populateDocs(holder.docsInner, docs);
            }
        } else {
            holder.divider.setVisibility(View.GONE);
            holder.docListContainer.setVisibility(View.GONE);
        }
    }

    private void fetchDocs(int position, FolderDoc folder) {
        loadingPositions.add(position);

        RetrofitClient.getApiService()
                .getDocuments(token, "", "recent", folder.getCategoryId())
                .enqueue(new Callback<List<Document>>() {
                    @Override
                    public void onResponse(Call<List<Document>> call,
                            Response<List<Document>> response) {
                        loadingPositions.remove(position);
                        List<RecentDoc> docs = new ArrayList<>();
                        if (response.isSuccessful() && response.body() != null) {
                            for (Document doc : response.body()) {
                                String fileUrl = "", fileType = "", ver = "1.0.0";
                                if (doc.getVersions() != null && !doc.getVersions().isEmpty()) {
                                    DocumentVersion v = doc.getVersions().get(0);
                                    fileUrl = v.getFileUrl() != null ? v.getFileUrl() : "";
                                    fileType = v.getFileType() != null ? v.getFileType() : "";
                                    ver = v.getVersionNumber() != null ? v.getVersionNumber() : "1.0.0";
                                }
                                String cat = (doc.getCategory() != null
                                        && doc.getCategory().getName() != null)
                                                ? doc.getCategory().getName() : "Uncategorized";
                                RecentDoc rd = new RecentDoc(doc.getId(), doc.getTitle(),
                                        doc.getDescription() != null ? doc.getDescription() : "",
                                        doc.getUpdatedAt() != null
                                                ? doc.getUpdatedAt().substring(0, 10) : "",
                                        R.drawable.file_logo, doc.getIsFavorite() > 0,
                                        fileUrl, fileType, cat, ver);
                                rd.setStatus(doc.getStatus());
                                docs.add(rd);
                            }
                        }
                        loadedDocs.put(position, docs);
                        notifyItemChanged(position);
                    }

                    @Override
                    public void onFailure(Call<List<Document>> call, Throwable t) {
                        loadingPositions.remove(position);
                        loadedDocs.put(position, new ArrayList<>());
                        notifyItemChanged(position);
                    }
                });
    }

    private void populateDocs(LinearLayout docsInner, List<RecentDoc> docs) {
        docsInner.removeAllViews();

        if (docs == null || docs.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("No documents in this category");
            empty.setTextColor(ContextCompat.getColor(context, R.color.gray_text));
            empty.setPadding(dp(16), dp(20), dp(16), dp(20));
            docsInner.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < docs.size(); i++) {
            RecentDoc doc = docs.get(i);

            View row = inflater.inflate(R.layout.item_category_doc_row, docsInner, false);
            ((TextView) row.findViewById(R.id.rowTitle)).setText(doc.getTitle());

            String desc = doc.getDescription() != null && !doc.getDescription().isEmpty()
                    ? doc.getDescription() : "No description";
            ((TextView) row.findViewById(R.id.rowDesc)).setText(desc);

            // File type badge
            TextView badge = row.findViewById(R.id.rowFileType);
            String ft = doc.getFileType();
            if (ft != null && !ft.isEmpty()) {
                badge.setText(ft.toUpperCase());
                badge.setVisibility(View.VISIBLE);
            } else {
                badge.setVisibility(View.GONE);
            }

            row.setOnClickListener(v -> openDetail(doc));
            docsInner.addView(row);
        }
    }

    private void openDetail(RecentDoc doc) {
        Intent intent = new Intent(context, DocumentDetailActivity.class);
        intent.putExtra("id", doc.getId());
        intent.putExtra("title", doc.getTitle());
        intent.putExtra("description", doc.getDescription());
        intent.putExtra("date", doc.getDate());
        intent.putExtra("file_url", doc.getFileUrl());
        intent.putExtra("file_type", doc.getFileType());
        intent.putExtra("category", doc.getCategory());
        intent.putExtra("version", doc.getVersion());
        intent.putExtra("status", doc.getStatus());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, count, date;
        View container;
        ImageView chevron;
        View divider;
        View docListContainer;
        ProgressBar loadingDocs;
        LinearLayout docsInner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            title = itemView.findViewById(R.id.folderName);
            count = itemView.findViewById(R.id.docCount);
            date = itemView.findViewById(R.id.lastEdited);
            chevron = itemView.findViewById(R.id.chevron);
            divider = itemView.findViewById(R.id.divider);
            docListContainer = itemView.findViewById(R.id.docListContainer);
            loadingDocs = itemView.findViewById(R.id.loadingDocs);
            docsInner = itemView.findViewById(R.id.docsInner);
        }
    }
}
