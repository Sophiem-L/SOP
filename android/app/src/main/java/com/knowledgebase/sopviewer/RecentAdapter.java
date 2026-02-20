package com.knowledgebase.sopviewer;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.ViewHolder> {

    private List<RecentDoc> recentDocs;
    private String token;
    private int colorBrandBlue = -1;
    private int colorTextSecondary = -1;

    public RecentAdapter(List<RecentDoc> recentDocs, String token) {
        this.recentDocs = recentDocs;
        this.token = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentDoc doc = recentDocs.get(position);
        holder.title.setText(doc.getTitle());
        holder.description.setText(doc.getDescription());
        holder.date.setText(doc.getDate());
        holder.image.setImageResource(doc.getImageResId());

        // Update favorite UI
        updateFavoriteIcon(holder, doc);

        holder.btnBookmark.setOnClickListener(v -> {
            boolean newStatus = !doc.isFavorite();
            doc.setFavorite(newStatus);
            updateFavoriteIcon(holder, doc);

            // Sync with backend
            ApiService apiService = RetrofitClient.getApiService();
            String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
            apiService.toggleFavorite(doc.getId(), authHeader).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        String message = newStatus ? "Added to bookmarks" : "Removed from bookmarks";
                        Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();

                        // Clear bookmarks cache to force refresh when visiting BookmarksActivity
                        DataCache.getInstance().clear(DataCache.KEY_BOOKMARKS);
                    } else {
                        // Rollback on failure
                        doc.setFavorite(!newStatus);
                        updateFavoriteIcon(holder, doc);
                        Toast.makeText(holder.itemView.getContext(), "Failed: " + response.code(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // Rollback on failure
                    doc.setFavorite(!newStatus);
                    updateFavoriteIcon(holder, doc);
                    Toast.makeText(holder.itemView.getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Make the entire item clickable
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(),
                    DocumentDetailActivity.class);
            intent.putExtra("id", doc.getId());
            intent.putExtra("title", doc.getTitle());
            intent.putExtra("description", doc.getDescription());
            intent.putExtra("date", doc.getDate());
            holder.itemView.getContext().startActivity(intent);
        });

        if (holder.btnView != null) {
            holder.btnView.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(),
                        DocumentDetailActivity.class);
                intent.putExtra("id", doc.getId());
                intent.putExtra("title", doc.getTitle());
                intent.putExtra("description", doc.getDescription());
                intent.putExtra("date", doc.getDate());
                holder.itemView.getContext().startActivity(intent);
            });
        }
    }

    private void updateFavoriteIcon(ViewHolder holder, RecentDoc doc) {
        // Cache colors on first use to avoid repeated resource lookups
        if (colorBrandBlue == -1) {
            colorBrandBlue = holder.itemView.getContext().getResources().getColor(R.color.brand_blue);
            colorTextSecondary = holder.itemView.getContext().getResources().getColor(R.color.text_secondary);
        }

        if (doc.isFavorite()) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark); // Filled
            holder.btnBookmark.setImageTintList(ColorStateList.valueOf(colorBrandBlue));
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_border); // Outlined
            holder.btnBookmark.setImageTintList(ColorStateList.valueOf(colorTextSecondary));
        }
    }

    @Override
    public int getItemCount() {
        return recentDocs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnBookmark, btnView;
        TextView title, description, date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.docImage);
            title = itemView.findViewById(R.id.docTitle);
            description = itemView.findViewById(R.id.docDescription);
            date = itemView.findViewById(R.id.docType);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}
