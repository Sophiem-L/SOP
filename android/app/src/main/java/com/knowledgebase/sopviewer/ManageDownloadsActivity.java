package com.knowledgebase.sopviewer;

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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows documents cached when the user opens them (PDFs stored in app cache).
 * User can remove individual items or clear all to free space.
 */
public class ManageDownloadsActivity extends AppCompatActivity {

    private static final String PDF_CACHE_PREFIX = "pdf_";

    private RecyclerView recyclerDownloads;
    private View emptyState;
    private TextView summarySize;
    private CachedFileAdapter adapter;
    private final List<File> cacheFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_downloads);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        summarySize = findViewById(R.id.summarySize);
        recyclerDownloads = findViewById(R.id.recyclerDownloads);
        emptyState = findViewById(R.id.emptyState);

        adapter = new CachedFileAdapter(cacheFiles, this::onDeleteFile, this::formatSize);
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
        cacheFiles.clear();
        File cacheDir = getCacheDir();
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().startsWith(PDF_CACHE_PREFIX) && f.getName().endsWith(".pdf")) {
                        cacheFiles.add(f);
                    }
                }
            }
        }

        long totalBytes = 0;
        for (File f : cacheFiles) {
            totalBytes += f.length();
        }
        summarySize.setText("Total: " + formatSize(totalBytes) + " (" + cacheFiles.size() + " document" + (cacheFiles.size() == 1 ? "" : "s") + ")");

        adapter.notifyDataSetChanged();

        if (cacheFiles.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerDownloads.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerDownloads.setVisibility(View.VISIBLE);
        }
    }

    private void onDeleteFile(File file) {
        if (file.delete()) {
            Toast.makeText(this, "Removed cached document", Toast.LENGTH_SHORT).show();
            refreshList();
        } else {
            Toast.makeText(this, "Could not remove file", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearAll() {
        if (cacheFiles.isEmpty()) {
            Toast.makeText(this, "No cached documents to clear", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear all cached documents?")
                .setMessage("This will remove all cached documents. They will be downloaded again when you open them.")
                .setPositiveButton("Clear all", (dialog, which) -> clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAll() {
        int removed = 0;
        for (File f : new ArrayList<>(cacheFiles)) {
            if (f.delete()) removed++;
        }
        cacheFiles.clear();
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(View.VISIBLE);
        recyclerDownloads.setVisibility(View.GONE);
        summarySize.setText("Total: 0 B (0 documents)");
        Toast.makeText(this, "Cleared " + removed + " cached document(s)", Toast.LENGTH_SHORT).show();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static class CachedFileAdapter extends RecyclerView.Adapter<CachedFileAdapter.VH> {
        private final List<File> files;
        private final OnDeleteListener onDelete;
        private final SizeFormatter sizeFormatter;

        interface OnDeleteListener { void onDelete(File file); }
        interface SizeFormatter { String format(long bytes); }

        CachedFileAdapter(List<File> files, OnDeleteListener onDelete, SizeFormatter sizeFormatter) {
            this.files = files;
            this.onDelete = onDelete;
            this.sizeFormatter = sizeFormatter;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_download, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            File f = files.get(position);
            holder.label.setText(f.getName().startsWith(ManageDownloadsActivity.PDF_CACHE_PREFIX)
                    ? "Cached document " + (position + 1)
                    : f.getName());
            holder.sizeText.setText(sizeFormatter.format(f.length()));
            holder.btnDelete.setOnClickListener(v -> onDelete.onDelete(f));
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView label, sizeText;
            ImageView btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                label = itemView.findViewById(R.id.label);
                sizeText = itemView.findViewById(R.id.sizeText);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
