package com.knowledgebase.sopviewer;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfflineAccessActivity extends AppCompatActivity {

    private static final String PREFS_OFFLINE_SETTINGS = "offline_settings";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download_sops";
    private static final String KEY_WIFI_ONLY = "download_wifi_only";

    private ProgressBar storageProgress;
    private TextView storageText;
    private SwitchCompat switchAutoDownload;
    private SwitchCompat switchWifiOnly;
    private LinearLayout listContainer;
    private View emptyState;

    private SharedPreferences offlinePrefs;
    private SharedPreferences downloadMetaPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_access);

        offlinePrefs = getSharedPreferences(PREFS_OFFLINE_SETTINGS, Context.MODE_PRIVATE);
        downloadMetaPrefs = getSharedPreferences(DownloadHelper.PREFS_NAME, Context.MODE_PRIVATE);

        storageProgress = findViewById(R.id.storageProgress);
        storageText = findViewById(R.id.storageText);
        switchAutoDownload = findViewById(R.id.switchAutoDownload);
        switchWifiOnly = findViewById(R.id.switchWifiOnly);
        listContainer = findViewById(R.id.listContainer);
        emptyState = findViewById(R.id.emptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Settings gear is reserved for future use
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show());

        // Load saved toggle states
        switchAutoDownload.setChecked(offlinePrefs.getBoolean(KEY_AUTO_DOWNLOAD, false));
        switchWifiOnly.setChecked(offlinePrefs.getBoolean(KEY_WIFI_ONLY, false));

        switchAutoDownload.setOnCheckedChangeListener((btn, checked) ->
                offlinePrefs.edit().putBoolean(KEY_AUTO_DOWNLOAD, checked).apply());

        switchWifiOnly.setOnCheckedChangeListener((btn, checked) ->
                offlinePrefs.edit().putBoolean(KEY_WIFI_ONLY, checked).apply());

        refreshStorage();
        refreshDocumentList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStorage();
        refreshDocumentList();
    }

    // -------------------------------------------------------------------------
    // Storage calculation
    // -------------------------------------------------------------------------

    private void refreshStorage() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            StatFs statFs = new StatFs(downloadsDir.getPath());
            long totalBytes = statFs.getTotalBytes();
            long freeBytes = statFs.getAvailableBytes();
            long usedBytes = totalBytes - freeBytes;

            int progressPct = totalBytes > 0 ? (int) (usedBytes * 100L / totalBytes) : 0;
            storageProgress.setProgress(progressPct);
            storageText.setText(formatSize(usedBytes) + " Used of " + formatSize(totalBytes));
        } catch (Exception e) {
            storageText.setText("Storage info unavailable");
        }
    }

    // -------------------------------------------------------------------------
    // Document list
    // -------------------------------------------------------------------------

    private void refreshDocumentList() {
        listContainer.removeAllViews();

        List<OfflineDoc> docs = loadOfflineDocs();

        if (docs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            listContainer.setVisibility(View.VISIBLE);

            LayoutInflater inflater = LayoutInflater.from(this);
            for (OfflineDoc doc : docs) {
                View item = inflater.inflate(R.layout.item_offline_document, listContainer, false);
                bindDocItem(item, doc);
                listContainer.addView(item);
            }
        }
    }

    private List<OfflineDoc> loadOfflineDocs() {
        List<OfflineDoc> result = new ArrayList<>();
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor cursor = dm.query(new DownloadManager.Query());

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                int status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long totalBytes = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                String metaJson = downloadMetaPrefs.getString(String.valueOf(id), null);
                if (metaJson == null) continue;

                try {
                    org.json.JSONObject json = new org.json.JSONObject(metaJson);
                    boolean isOffline = json.optBoolean("offline", false);
                    if (!isOffline) continue;

                    String title = json.optString("title", "Document");
                    String fileType = json.optString("fileType", "");
                    boolean offlineEnabled = json.optBoolean("offlineEnabled", true);

                    result.add(new OfflineDoc(id, title, fileType, totalBytes,
                            status, offlineEnabled));
                } catch (org.json.JSONException ignored) {
                }
            }
            cursor.close();
        }
        return result;
    }

    private void bindDocItem(View item, OfflineDoc doc) {
        TextView tvTitle = item.findViewById(R.id.docTitle);
        TextView tvSize = item.findViewById(R.id.docSize);
        SwitchCompat toggle = item.findViewById(R.id.switchOffline);
        ImageView btnDelete = item.findViewById(R.id.btnDelete);
        ImageView iconFile = item.findViewById(R.id.iconFile);
        android.widget.ProgressBar iconLoading = item.findViewById(R.id.iconLoading);

        tvTitle.setText(doc.title);
        tvSize.setText(doc.totalBytes > 0 ? formatSize(doc.totalBytes) : "—");
        toggle.setChecked(doc.offlineEnabled);

        // Show spinner while downloading, download icon when ready
        if (doc.status == DownloadManager.STATUS_SUCCESSFUL) {
            iconFile.setVisibility(View.VISIBLE);
            iconLoading.setVisibility(View.GONE);
        } else {
            iconFile.setVisibility(View.GONE);
            iconLoading.setVisibility(View.VISIBLE);
        }

        toggle.setOnCheckedChangeListener((btn, checked) -> {
            doc.offlineEnabled = checked;
            updateOfflineEnabled(doc.id, checked);
        });

        btnDelete.setOnClickListener(v -> confirmDelete(doc));

        item.setOnClickListener(v -> openDoc(doc));
    }

    private void openDoc(OfflineDoc doc) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(doc.id);
            if (uri == null) {
                // File not yet complete — fall back to local file path
                openDocByLocalUri(doc);
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType(doc.fileType));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDocByLocalUri(OfflineDoc doc) {
        // Try resolving via the DownloadManager query for local URI
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(doc.id);
        Cursor c = dm.query(q);
        String localUri = null;
        int status = -1;
        if (c != null) {
            if (c.moveToFirst()) {
                localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
            }
            c.close();
        }
        if (status != DownloadManager.STATUS_SUCCESSFUL || localUri == null) {
            Toast.makeText(this, "File is still downloading, please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(localUri), mimeType(doc.fileType));
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

    private void updateOfflineEnabled(long downloadId, boolean enabled) {
        String key = String.valueOf(downloadId);
        String metaJson = downloadMetaPrefs.getString(key, null);
        if (metaJson == null) return;
        try {
            org.json.JSONObject json = new org.json.JSONObject(metaJson);
            json.put("offlineEnabled", enabled);
            downloadMetaPrefs.edit().putString(key, json.toString()).apply();
        } catch (org.json.JSONException ignored) {
        }
    }

    private void confirmDelete(OfflineDoc doc) {
        new AlertDialog.Builder(this)
                .setTitle("Remove offline document?")
                .setMessage("\"" + doc.title + "\" will be removed from offline storage.")
                .setPositiveButton("Remove", (dialog, which) -> deleteDoc(doc))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDoc(OfflineDoc doc) {
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.remove(doc.id);
        downloadMetaPrefs.edit().remove(String.valueOf(doc.id)).apply();
        Toast.makeText(this, "Removed from offline storage", Toast.LENGTH_SHORT).show();
        refreshStorage();
        refreshDocumentList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // -------------------------------------------------------------------------
    // Data model
    // -------------------------------------------------------------------------

    static class OfflineDoc {
        long id;
        String title;
        String fileType;
        long totalBytes;
        int status;
        boolean offlineEnabled;

        OfflineDoc(long id, String title, String fileType,
                long totalBytes, int status, boolean offlineEnabled) {
            this.id = id;
            this.title = title;
            this.fileType = fileType;
            this.totalBytes = totalBytes;
            this.status = status;
            this.offlineEnabled = offlineEnabled;
        }
    }
}
