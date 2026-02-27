package com.knowledgebase.sopviewer;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

public class DownloadHelper {

    static final String PREFS_NAME = "download_meta";

    /** Resolves localhost / 127.0.0.1 to 10.0.2.2 for emulator compatibility. */
    public static String resolveUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        return url
                .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                .replace("http://localhost/", "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1/", "http://10.0.2.2:8000/");
    }

    /**
     * Enqueues a file download via Android's DownloadManager and saves document
     * metadata so the Manage Downloads screen can display title / description / type.
     */
    public static void download(Context context, String rawUrl,
            String docTitle, String fileType, String description) {
        String url = resolveUrl(rawUrl);
        if (url.isEmpty()) {
            Toast.makeText(context, "No file available to download", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = buildFileName(url, docTitle, fileType);

        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(docTitle != null && !docTitle.isEmpty() ? docTitle : fileName);
            request.setDescription("Downloading document…");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);

            long downloadId = dm.enqueue(request);
            saveMetadata(context, downloadId, docTitle, fileType, description);

            Toast.makeText(context, "Downloading " + fileName + "…", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Convenience overload for callers that don't have a description. */
    public static void download(Context context, String rawUrl,
            String docTitle, String fileType) {
        download(context, rawUrl, docTitle, fileType, "");
    }

    // -------------------------------------------------------------------------

    private static void saveMetadata(Context context, long downloadId,
            String title, String fileType, String description) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("title", title != null ? title : "");
            json.put("fileType", fileType != null ? fileType : "");
            json.put("description", description != null ? description : "");
            prefs.edit().putString(String.valueOf(downloadId), json.toString()).apply();
        } catch (org.json.JSONException ignored) {
        }
    }

    private static String buildFileName(String url, String docTitle, String fileType) {
        String lastSegment = Uri.parse(url).getLastPathSegment();
        if (lastSegment != null && lastSegment.contains(".")) {
            return lastSegment;
        }
        String ext = (fileType != null && !fileType.isEmpty())
                ? "." + fileType.toLowerCase()
                : ".pdf";
        String safe = (docTitle != null && !docTitle.isEmpty())
                ? docTitle.replaceAll("[^a-zA-Z0-9_\\-. ]", "_")
                : "document";
        return safe + ext;
    }
}
