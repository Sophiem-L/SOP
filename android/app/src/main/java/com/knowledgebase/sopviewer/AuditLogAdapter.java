package com.knowledgebase.sopviewer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.ViewHolder> {

    private List<AuditLog> auditLogs;

    public AuditLogAdapter(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audit_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AuditLog log = auditLogs.get(position);
        holder.tvUserName.setText(log.getUserName());
        holder.tvUserRole.setText(log.getUserRole());
        holder.tvTimestamp.setText(log.getTimestamp());
        holder.tvActionTitle.setText(log.getActionTitle());
        holder.tvAttachmentName.setText(log.getAttachmentName());
        holder.userAvatar.setImageResource(log.getAvatarRes());
    }

    @Override
    public int getItemCount() {
        return auditLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userAvatar;
        TextView tvUserName, tvUserRole, tvTimestamp, tvActionTitle, tvAttachmentName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvActionTitle = itemView.findViewById(R.id.tvActionTitle);
            tvAttachmentName = itemView.findViewById(R.id.tvAttachmentName);
        }
    }
}
