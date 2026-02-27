package com.knowledgebase.sopviewer;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageDownloadsActivity extends AppCompatActivity {

    private RecyclerView recyclerDownloads;
    private View emptyState;
    private TextView summarySize;
    private DownloadItemAdapter adapter;
    private final List<DownloadEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_downloads);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        summarySize = findViewById(R.id.summarySize);
        recyclerDownloads = findViewById(R.id.recyclerDownloads);
        emptyState = findViewById(R.id.emptyState);

        adapter = new DownloadItemAdapter(entries, this::onDeleteEntry, this::openEntry);
        recyclerDownloads.setLayoutManager(new LinearLayoutManager(this));
        recyclerDownloads.setAdapter(adapter);

        findViewById(R.id.btnClearAll).setOnClickListener(v -> confirmClearAll());

        refreshList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        entries.clear();

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        SharedPreferences prefs = getSharedPreferences(DownloadHelper.PREFS_NAME, Context.MODE_PRIVATE);

        Cursor cursor = dm.query(new DownloadManager.Query());
        long totalBytes = 0;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                String dmTitle = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                long downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                String localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));

                // Read metadata stored by DownloadHelper
                String title = dmTitle != null ? dmTitle : "Document";
                String description = "";
                String fileType = "";

                String metaJson = prefs.getString(String.valueOf(id), null);
                if (metaJson != null) {
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(metaJson);
                        String t = json.optString("title");
                        if (!t.isEmpty()) title = t;
                        description = json.optString("description");
                        fileType = json.optString("fileType");
                    } catch (org.json.JSONException ignored) {
                    }
                }

                if (total > 0) totalBytes += total;
                entries.add(new DownloadEntry(id, title, description, fileType,
                        status, total, downloaded, localUri));
            }
            cursor.close();
        }

        int count = entries.size();
        summarySize.setText(count + " document" + (count == 1 ? "" : "s")
                + (totalBytes > 0 ? " · " + formatSize(totalBytes) : ""));

        adapter.notifyDataSetChanged();

        if (entries.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerDownloads.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerDownloads.setVisibility(View.VISIBLE);
        }
    }

    private void onDeleteEntry(DownloadEntry entry) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.remove(entry.id);
        getSharedPreferences(DownloadHelper.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(String.valueOf(entry.id)).apply();
        Toast.makeText(this, "Removed from downloads", Toast.LENGTH_SHORT).show();
        refreshList();
    }

    private void openEntry(DownloadEntry entry) {
        if (entry.status != DownloadManager.STATUS_SUCCESSFUL) {
            Toast.makeText(this, "File is not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(entry.id);
            if (uri == null) {
                Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType(entry.fileType));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private static String mimeType(String fileType) {
        if (fileType == null) return "*/*";
        switch (fileType.toLowerCase()) {
            case "pdf":  return "application/pdf";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":  return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":  return "text/plain";
            default:     return "*/*";
        }
    }

    private void confirmClearAll() {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No downloads to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear all downloads?")
                .setMessage("This will remove all downloaded documents.")
                .setPositiveButton("Clear all", (dialog, which) -> clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAll() {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        SharedPreferences.Editor editor = getSharedPreferences(
                DownloadHelper.PREFS_NAME, Context.MODE_PRIVATE).edit();
        for (DownloadEntry e : entries) {
            dm.remove(e.id);
            editor.remove(String.valueOf(e.id));
        }
        editor.apply();
        entries.clear();
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(View.VISIBLE);
        recyclerDownloads.setVisibility(View.GONE);
        summarySize.setText("0 documents");
        Toast.makeText(this, "All downloads cleared", Toast.LENGTH_SHORT).show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ---- Data model ----

    static class DownloadEntry {
        long id;
        String title, description, fileType, localUri;
        int status;
        long totalBytes, downloadedBytes;

        DownloadEntry(long id, String title, String description, String fileType,
                int status, long totalBytes, long downloadedBytes, String localUri) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.fileType = fileType;
            this.status = status;
            this.totalBytes = totalBytes;
            this.downloadedBytes = downloadedBytes;
            this.localUri = localUri;
        }
    }

    // ---- Adapter ----

    interface EntryAction { void run(DownloadEntry entry); }

    private class DownloadItemAdapter extends RecyclerView.Adapter<DownloadItemAdapter.VH> {
        private final List<DownloadEntry> items;
        private final EntryAction onDelete;
        private final EntryAction onOpen;

        DownloadItemAdapter(List<DownloadEntry> items, EntryAction onDelete, EntryAction onOpen) {
            this.items = items;
            this.onDelete = onDelete;
            this.onOpen = onOpen;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manage_download, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DownloadEntry entry = items.get(position);

            holder.title.setText(entry.title);
            holder.description.setText(!entry.description.isEmpty()
                    ? entry.description : "No description");
            holder.fileType.setText(!entry.fileType.isEmpty()
                    ? entry.fileType.toUpperCase() : "FILE");

            // Status / size
            switch (entry.status) {
                case DownloadManager.STATUS_SUCCESSFUL:
                    holder.statusText.setText(entry.totalBytes > 0
                            ? formatSize(entry.totalBytes) : "Done");
                    break;
                case DownloadManager.STATUS_RUNNING:
                    if (entry.totalBytes > 0) {
                        int pct = (int) (entry.downloadedBytes * 100L / entry.totalBytes);
                        holder.statusText.setText("Downloading " + pct + "%");
                    } else {
                        holder.statusText.setText("Downloading…");
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    holder.statusText.setText("Failed");
                    break;
                default:
                    holder.statusText.setText("Pending");
                    break;
            }

            holder.itemView.setOnClickListener(v -> onOpen.run(entry));
            holder.btnDelete.setOnClickListener(v -> onDelete.run(entry));
        }

        @Override
        public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView title, description, fileType, statusText;
            ImageView btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.label);
                description = itemView.findViewById(R.id.downloadDescription);
                fileType = itemView.findViewById(R.id.downloadFileType);
                statusText = itemView.findViewById(R.id.sizeText);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
