package com.knowledgebase.sopviewer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;

    public NotificationAdapter(List<Notification> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvContent.setText(notification.getContent());
        holder.tvTime.setText(notification.getTime());

        String status = notification.getStatus();
        if (status.isEmpty()) {
            holder.tvBadge.setVisibility(View.GONE);
        } else {
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.tvBadge.setText(status);
            int color;
            if ("Suggestion".equals(status)) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_orange);
            } else if ("Review".equals(status)) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_pink);
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.brand_blue);
            }
            holder.tvBadge.getBackground().setTint(color);
        }

        // Tap the card → open the linked document (if any)
        Integer docId = notification.getDocumentId();
        if (docId != null) {
            holder.itemView.setOnClickListener(v -> {
                Context ctx = holder.itemView.getContext();
                Intent intent = new Intent(ctx, DocumentDetailActivity.class);
                intent.putExtra("id", docId);
                intent.putExtra("title", notification.getTitle());
                intent.putExtra("description", notification.getContent());
                intent.putExtra("date", notification.getTime());
                intent.putExtra("file_url", "");
                intent.putExtra("file_type", "");
                intent.putExtra("category", "");
                intent.putExtra("version", "");
                intent.putExtra("notification_type", notification.getType());
                if ("suggestion".equals(notification.getType())) {
                    intent.putExtra("suggestion_message", notification.getMessage());
                    intent.putExtra("open_suggestion", true);
                    intent.putExtra("suggestion_attachment_url", notification.getAttachmentUrl());
                }
                ctx.startActivity(intent);
            });
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime, tvBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBadge = itemView.findViewById(R.id.tvBadge);
        }
    }
}
