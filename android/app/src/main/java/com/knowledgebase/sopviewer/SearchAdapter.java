package com.knowledgebase.sopviewer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private final List<RecentDoc> searchResults;
    private Context context;

    public SearchAdapter(List<RecentDoc> searchResults) {
        this.searchResults = searchResults;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentDoc doc = searchResults.get(position);
        holder.title.setText(doc.getTitle());
        holder.description.setText(doc.getDescription());
        holder.date.setText(doc.getDate());

        // Set tag text (using description prefix or static for now, ideally add tag
        // field to RecentDoc)
        // For now, let's infer tag or set a default if not present
        String type = doc.getFileType() != null && !doc.getFileType().isEmpty() ? doc.getFileType().toUpperCase() + " Document" : "Document";
        holder.tag.setText(type);

        // holder.image.setImageResource(doc.getImageResId()); // Removed in new layout

        // Update favorite UI
        updateFavoriteIcon(holder, doc);

        holder.btnBookmark.setOnClickListener(v -> {
            boolean newStatus = !doc.isFavorite();
            doc.setFavorite(newStatus);
            updateFavoriteIcon(holder, doc);
            // TODO: Sync with backend API
        });

        // Setup other buttons if needed
        holder.btnDownload.setOnClickListener(v ->
                DownloadSheet.show(holder.itemView.getContext(), doc));

        android.view.View.OnClickListener openDetail = v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(),
                    DocumentDetailActivity.class);
            intent.putExtra("id", doc.getId());
            intent.putExtra("title", doc.getTitle());
            intent.putExtra("description", doc.getDescription());
            intent.putExtra("date", doc.getDate());
            intent.putExtra("file_url", doc.getFileUrl());
            intent.putExtra("file_type", doc.getFileType());
            intent.putExtra("category", doc.getCategory());
            intent.putExtra("version", doc.getVersion());
            holder.itemView.getContext().startActivity(intent);
        };
        holder.iconEye.setOnClickListener(openDetail);
        holder.itemView.setOnClickListener(openDetail);
    }

    private void updateFavoriteIcon(ViewHolder holder, RecentDoc doc) {
        if (doc.isFavorite()) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark); // Filled
            holder.btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_blue)));
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_border); // Outlined
            holder.btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(),
                            R.color.text_secondary)));
        }
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnBookmark, btnDownload, iconEye;
        TextView title, description, date, tag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // image = itemView.findViewById(R.id.searchResultImage); // Removed in new
            // layout if not needed, or update ID
            title = itemView.findViewById(R.id.searchResultTitle);
            description = itemView.findViewById(R.id.searchResultDescription);
            date = itemView.findViewById(R.id.searchResultDate);
            tag = itemView.findViewById(R.id.searchResultTag);
            btnBookmark = itemView.findViewById(R.id.searchResultBookmark);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            iconEye = itemView.findViewById(R.id.iconEye);
        }
    }
}
