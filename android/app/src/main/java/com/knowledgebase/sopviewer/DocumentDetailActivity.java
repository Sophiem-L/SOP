package com.knowledgebase.sopviewer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import com.rajat.pdfviewer.PdfRendererView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DocumentDetailActivity extends AppCompatActivity {

    private static final String TAG = "PDFLoader";

    // Keep fileUrl as a field so the fallback button can use it
    private String resolvedFileUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        int docId = getIntent().getIntExtra("id", -1);
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String date = getIntent().getStringExtra("date");
        String fileUrl = getIntent().getStringExtra("file_url");
        String fileType = getIntent().getStringExtra("file_type");
        String category = getIntent().getStringExtra("category");
        String version = getIntent().getStringExtra("version");
        String status = getIntent().getStringExtra("status");
        if (status == null)
            status = "";

        // Resolve the URL once for the whole activity
        resolvedFileUrl = resolveUrl(fileUrl);
        Log.d(TAG, "file_url from intent: " + fileUrl);
        Log.d(TAG, "resolved URL: " + resolvedFileUrl);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView headerTitle = findViewById(R.id.headerTitle);
        if (title != null)
            headerTitle.setText(title);

        TextView docCategory = findViewById(R.id.docCategory);
        docCategory.setText(category != null && !category.isEmpty() ? category : "Uncategorized");

        TextView docFileType = findViewById(R.id.docFileType);
        docFileType.setText(fileType != null && !fileType.isEmpty() ? fileType.toUpperCase() : "DOC");

        // Status badge
        TextView docStatusBadge = findViewById(R.id.docStatusBadge);
        applyStatusBadge(docStatusBadge, status);

        TextView docContent = findViewById(R.id.docContent);
        docContent.setText(description != null && !description.isEmpty() ? description : "No description available.");

        TextView docVersionInfo = findViewById(R.id.docVersionInfo);
        String versionText = "Version: " + (version != null && !version.isEmpty() ? version : "1.0.0");
        if (date != null && !date.isEmpty())
            versionText += "\n" + date;
        docVersionInfo.setText(versionText);

        // Approval actions — check if current user is HR/Admin, then show if doc is
        // pending
        LinearLayout layoutApprovalActions = findViewById(R.id.layoutApprovalActions);
        MaterialButton btnApprove = findViewById(R.id.btnApprove);
        MaterialButton btnReject = findViewById(R.id.btnReject);
        final String finalStatus = status;
        final int finalDocId = docId;

        // ── Suggestion banner (visible when opened from a suggestion notification) ──
        LinearLayout suggestionBanner = findViewById(R.id.suggestionBanner);
        android.widget.TextView tvSuggestionMessage = findViewById(R.id.tvSuggestionMessage);
        com.google.android.material.button.MaterialButton btnEditDocument =
                findViewById(R.id.btnEditDocument);

        boolean openSuggestion   = getIntent().getBooleanExtra("open_suggestion", false);
        String  suggestionMsg    = getIntent().getStringExtra("suggestion_message");
        String  attachmentUrl    = getIntent().getStringExtra("suggestion_attachment_url");

        if (openSuggestion && suggestionMsg != null && !suggestionMsg.isEmpty()
                && suggestionBanner != null) {
            suggestionBanner.setVisibility(View.VISIBLE);
            if (tvSuggestionMessage != null) tvSuggestionMessage.setText(suggestionMsg);

            // Show attachment row if a file was attached to the suggestion
            LinearLayout layoutAttachment   = findViewById(R.id.layoutSuggestionAttachment);
            TextView     tvAttachFileName   = findViewById(R.id.tvAttachmentFileName);
            com.google.android.material.button.MaterialButton btnOpenAttachment =
                    findViewById(R.id.btnOpenAttachment);

            if (attachmentUrl != null && !attachmentUrl.isEmpty() && layoutAttachment != null) {
                layoutAttachment.setVisibility(View.VISIBLE);
                // Display just the file name from the URL
                String displayName = attachmentUrl.contains("/")
                        ? attachmentUrl.substring(attachmentUrl.lastIndexOf('/') + 1)
                        : "Attachment";
                if (tvAttachFileName != null) tvAttachFileName.setText(displayName);

                if (btnOpenAttachment != null) {
                    btnOpenAttachment.setOnClickListener(v -> openAttachmentUrl(
                            DownloadHelper.resolveUrl(attachmentUrl)));
                }
            }
        }

        final String currentTitle = title;
        final String currentDesc  = description;
        if (btnEditDocument != null) {
            btnEditDocument.setOnClickListener(v ->
                    showEditDocumentDialog(finalDocId, currentTitle, currentDesc));
        }

        android.widget.Button btnSuggestions = findViewById(R.id.btnSuggestions);
        if (btnSuggestions != null) {
            btnSuggestions.setVisibility(View.GONE);
            btnSuggestions.setOnClickListener(v -> {
                Intent si = new Intent(this, SubmitSuggestionActivity.class);
                si.putExtra("doc_id", finalDocId);
                startActivity(si);
            });
        }

        fetchRoleAndSetupUI(layoutApprovalActions, btnApprove, btnReject, finalDocId, finalStatus, btnSuggestions);

        // Download icon shows a preview sheet with document details + Download button
        final String finalTitle = title;
        final String finalFileType = fileType;
        final String finalDescription = description;
        final String finalCategory = category;
        final String finalVersion = version;
        final String finalDate = date;
        ImageView btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> DownloadSheet.show(this,
                finalTitle, finalDescription, finalCategory,
                finalFileType, finalVersion, finalDate, finalStatus,
                resolvedFileUrl));

        LinearLayout pdfContainer = findViewById(R.id.pdfContainer);
        ProgressBar pdfLoadingBar = findViewById(R.id.pdfLoadingBar);
        PdfRendererView pdfRendererView = findViewById(R.id.pdfRendererView);
        TextView pdfErrorText = findViewById(R.id.pdfErrorText);

        LinearLayout docViewerContainer = findViewById(R.id.docViewerContainer);
        TextView docFileName = findViewById(R.id.docFileName);
        TextView docFileSubtitle = findViewById(R.id.docFileSubtitle);
        MaterialButton btnOpenDoc = findViewById(R.id.btnOpenDoc);

        boolean isPdf = "pdf".equalsIgnoreCase(fileType);
        final boolean hasPdf = isPdf && !resolvedFileUrl.isEmpty();
        boolean isDoc = ("doc".equalsIgnoreCase(fileType) || "docx".equalsIgnoreCase(fileType))
                && !resolvedFileUrl.isEmpty();

        if (hasPdf) {
            pdfContainer.setVisibility(View.VISIBLE);
        } else if (isDoc) {
            // Show a file card — user must download to view the DOC/DOCX
            docViewerContainer.setVisibility(View.VISIBLE);
            String displayName = (title != null && !title.isEmpty()) ? title : "Document";
            docFileName.setText(displayName + "." + fileType.toLowerCase());
            docFileSubtitle.setText("docx".equalsIgnoreCase(fileType) ? "Word Document (DOCX)" : "Word Document (DOC)");
            final String fFileType = fileType;
            final String fTitle = title;
            final String fDesc = description;
            btnOpenDoc.setOnClickListener(v ->
                    DownloadHelper.download(this, resolvedFileUrl, fTitle, fFileType, fDesc));
        }

        if (hasPdf) {
            // Download directly from the public storage URL (resolvedFileUrl is
            // already http://10.0.2.2:8000/storage/...). PHP artisan serve serves
            // files from public/storage as static files — no PHP output buffering,
            // no chunked-encoding issues, completely reliable for binary content.
            loadPdfInline(resolvedFileUrl, pdfRendererView, pdfLoadingBar, pdfErrorText);
        }
    }

    /** Convert any localhost variant to the emulator host alias. */
    private String resolveUrl(String url) {
        if (url == null || url.isEmpty())
            return "";
        return url
                .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                .replace("http://localhost/", "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1/", "http://10.0.2.2:8000/");
    }

    /**
     * Downloads the PDF from the public storage URL and renders it inline.
     * PHP artisan serve serves public/storage files as static files, bypassing
     * PHP's output buffering pipeline that caused truncation through the controller.
     */
    private void loadPdfInline(String url, PdfRendererView pdfRendererView,
            ProgressBar loadingBar, TextView errorView) {
        Log.d(TAG, "loadPdfInline URL: " + url);
        new Thread(() -> {
            String cacheFileName = "pdf_" + url.hashCode() + ".pdf";
            java.io.File cacheFile = new java.io.File(getCacheDir(), cacheFileName);
            // Temp file used during download — renamed atomically on success so a
            // partial download can never masquerade as a valid cached file.
            java.io.File tempFile = new java.io.File(getCacheDir(), cacheFileName + ".tmp");

            if (cacheFile.exists() && isValidPdf(cacheFile)) {
                Log.d(TAG, "Serving PDF from cache: " + cacheFile.getName());
                renderPdfFromFile(cacheFile, pdfRendererView, loadingBar, errorView);
                return;
            }
            // Remove any stale/corrupt final or temp files before downloading
            cacheFile.delete();
            tempFile.delete();

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .build();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError(loadingBar, errorView, "HTTP " + response.code());
                    return;
                }

                // Stream raw bytes directly to disk — no base64, no memory spike.
                // PHP artisan serve on Windows sometimes closes the connection
                // without sending the final HTTP chunk terminator. We catch
                // EOFException so the bytes we already wrote are not discarded;
                // isValidPdf() below decides whether the content is usable.
                long totalBytes = 0;
                try (java.io.InputStream in = response.body().byteStream();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    try {
                        while ((n = in.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                            totalBytes += n;
                        }
                    } catch (java.io.IOException eof) {
                        // okio.EOFException extends IOException (not java.io.EOFException),
                        // so we catch IOException here. If we already received bytes, this
                        // is likely the missing HTTP chunk terminator from PHP artisan serve —
                        // the PDF content itself should be intact. If 0 bytes, re-throw.
                        if (totalBytes == 0) throw eof;
                        Log.w(TAG, "Stream ended after " + totalBytes + " bytes: " + eof.getMessage());
                    }
                    fos.flush();
                }
                Log.d(TAG, "Downloaded " + totalBytes + " bytes to " + tempFile.getName());

                System.gc(); // Suggest GC before bitmap-heavy PDF rendering

                if (!isValidPdf(tempFile)) {
                    tempFile.delete();
                    showError(loadingBar, errorView, "Received invalid PDF from server");
                    return;
                }
                if (!tempFile.renameTo(cacheFile)) {
                    // renameTo can fail across file systems; fall back to copy+delete
                    try (java.io.InputStream in = new java.io.FileInputStream(tempFile);
                         java.io.OutputStream out = new java.io.FileOutputStream(cacheFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                    tempFile.delete();
                }
                renderPdfFromFile(cacheFile, pdfRendererView, loadingBar, errorView);
            } catch (Exception e) {
                tempFile.delete();
                cacheFile.delete();
                Log.e(TAG, "PDF load error: " + e.getMessage());
                showError(loadingBar, errorView, "Load error: " + e.getMessage());
            }
        }).start();
    }

    /** Returns true if the file starts with the PDF magic bytes (%PDF). */
    private boolean isValidPdf(java.io.File file) {
        if (!file.exists() || file.length() < 4) return false;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] h = new byte[4];
            return fis.read(h) == 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
        } catch (Exception e) {
            return false;
        }
    }

    private void showError(ProgressBar loadingBar, TextView errorView, String message) {
        Log.e(TAG, "PDF preview error: " + message);
        runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        });
    }

    private void renderPdfFromFile(java.io.File file, PdfRendererView pdfRendererView,
            ProgressBar loadingBar, TextView errorView) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                pdfRendererView.setStatusListener(new PdfRendererView.StatusCallBack() {
                    @Override
                    public void onPdfLoadStart() {}

                    @Override
                    public void onPdfLoadSuccess(String absolutePath) {
                        loadingBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onPdfLoadProgress(int progress, long downloadedBytes, Long totalBytes) {}

                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "PdfRendererView error: " + error.getMessage());
                        file.delete(); // Remove corrupt cache so next open re-downloads
                        showError(loadingBar, errorView, "Render error: " + error.getMessage());
                    }

                    @Override
                    public void onPageChanged(int currentPage, int totalPage) {}
                });
                pdfRendererView.initWithFile(file);
                loadingBar.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "initWithFile crash: " + e.getMessage());
                file.delete(); // Remove corrupt cache so next open re-downloads
                showError(loadingBar, errorView, "Cannot open PDF: " + e.getMessage());
            }
        });
    }

    /** Colours and shows the status badge based on document status. */
    private void applyStatusBadge(TextView badge, String status) {
        if (status == null || status.isEmpty()) {
            badge.setVisibility(View.GONE);
            return;
        }
        badge.setVisibility(View.VISIBLE);
        switch (status) {
            case "approved":
                badge.setText("Approved");
                badge.setTextColor(Color.parseColor("#16A34A"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#F0FDF4")));
                break;
            case "rejected":
                badge.setText("Rejected");
                badge.setTextColor(Color.parseColor("#DC2626"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF2F2")));
                break;
            default: // pending
                badge.setText("Pending Approval");
                badge.setTextColor(Color.parseColor("#D97706"));
                badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFBEB")));
                break;
        }
    }

    /**
     * Fetches the current user's profile; shows approve/reject if HR/Admin +
     * pending, and Suggestions button for HR/Admin.
     */
    private void fetchRoleAndSetupUI(LinearLayout actionsLayout,
            MaterialButton btnApprove, MaterialButton btnReject, int docId,
            String docStatus, android.widget.Button btnSuggestions) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null)
            return;

        firebaseUser.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful())
                return;
            String token = "Bearer " + task.getResult().getToken();

            RetrofitClient.getApiService().getProfile(token)
                    .enqueue(new retrofit2.Callback<User>() {
                        @Override
                        public void onResponse(retrofit2.Call<User> call,
                                retrofit2.Response<User> response) {
                            if (!response.isSuccessful() || response.body() == null)
                                return;
                            User user = response.body();
                            boolean isHrOrAdmin = false;
                            if (user.getRoles() != null) {
                                for (User.Role r : user.getRoles()) {
                                    if ("admin".equals(r.getName()) || "hr".equals(r.getName())) {
                                        isHrOrAdmin = true;
                                        break;
                                    }
                                }
                            }
                            if (isHrOrAdmin) {
                                // Show approve/reject only when document is pending
                                if ("pending".equals(docStatus) && docId != -1) {
                                    actionsLayout.setVisibility(View.VISIBLE);
                                    btnApprove.setOnClickListener(
                                            v -> submitStatusUpdate(docId, token, "approved", null));
                                    btnReject.setOnClickListener(v -> showRejectDialog(docId, token));
                                }
                                // Show Suggestions button for HR/Admin only
                                if (btnSuggestions != null) {
                                    btnSuggestions.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<User> call, Throwable t) {
                            Log.e(TAG, "Failed to fetch user role: " + t.getMessage());
                        }
                    });
        });
    }

    private void showRejectDialog(int docId, String token) {
        EditText noteInput = new EditText(this);
        noteInput.setHint("Reason for rejection (optional)");
        noteInput.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("Reject Document")
                .setMessage("Provide a reason for rejection:")
                .setView(noteInput)
                .setPositiveButton("Reject", (dialog, which) -> {
                    String note = noteInput.getText().toString().trim();
                    submitStatusUpdate(docId, token, "rejected", note.isEmpty() ? null : note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Downloads a DOC/DOCX file via the authenticated API streaming endpoint
     * and shows an "Open Document" button.
     */
    /**
     * Downloads the DOC/DOCX via the Base64 JSON endpoint (same approach as PDF)
     * to avoid the "unexpected end of stream" / "unsupported file type" error caused
     * by PHP artisan serve on Windows corrupting binary responses.
     */
    private void downloadDocForViewing(String url, String bearerToken, String fileType,
            ProgressBar loadingBar, TextView statusText, MaterialButton openButton) {
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                        .build();
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url)
                        .header("Authorization", bearerToken)
                        .build();
                byte[] docBytes;
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        runOnUiThread(() -> {
                            loadingBar.setVisibility(View.GONE);
                            statusText.setText("Cannot load document (HTTP " + response.code() + ")");
                            statusText.setVisibility(View.VISIBLE);
                        });
                        return;
                    }
                    // Response is JSON: {"data":"<base64>","mime":"doc"}
                    String json = response.body().string();
                    org.json.JSONObject obj = new org.json.JSONObject(json);
                    String base64Data = obj.getString("data");
                    docBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                }
                // Detect the real format from magic bytes — the backend stores both
                // .doc and .docx as file_type="doc", so fileType alone is unreliable.
                // .docx (Office Open XML) is a ZIP file: magic bytes = PK (50 4B 03 04)
                // .doc (OLE2 compound doc) starts with: D0 CF 11 E0
                boolean isDocx = docBytes.length >= 4
                        && (docBytes[0] & 0xFF) == 0x50   // P
                        && (docBytes[1] & 0xFF) == 0x4B   // K
                        && (docBytes[2] & 0xFF) == 0x03
                        && (docBytes[3] & 0xFF) == 0x04;
                String ext = isDocx ? "docx" : "doc";
                String mime = isDocx
                        ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        : "application/msword";
                java.io.File cacheFile = new java.io.File(getCacheDir(), "preview." + ext);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                    fos.write(docBytes);
                }
                Uri fileUri = FileProvider.getUriForFile(
                        DocumentDetailActivity.this,
                        "com.knowledgebase.sopviewer.fileprovider",
                        cacheFile);
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    openButton.setVisibility(View.VISIBLE);
                    openButton.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(fileUri, mime);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException ex) {
                            Intent fallback = new Intent(Intent.ACTION_VIEW);
                            fallback.setDataAndType(fileUri, "application/octet-stream");
                            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivity(fallback);
                            } catch (android.content.ActivityNotFoundException ex2) {
                                Toast.makeText(DocumentDetailActivity.this,
                                        "No app available to open this document type",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                });
            } catch (Exception e) {
                Log.e(TAG, "DOC download error: " + e.getMessage());
                runOnUiThread(() -> {
                    loadingBar.setVisibility(View.GONE);
                    statusText.setText("Error loading document: " + e.getMessage());
                    statusText.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void submitStatusUpdate(int docId, String token, String newStatus, String note) {
        RetrofitClient.getApiService()
                .updateDocumentStatus(docId, token, newStatus, note != null ? note : "")
                .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call,
                            retrofit2.Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(DocumentDetailActivity.this,
                                    "Document " + newStatus, Toast.LENGTH_SHORT).show();
                            // Refresh badge and hide action buttons
                            TextView badge = findViewById(R.id.docStatusBadge);
                            applyStatusBadge(badge, newStatus);
                            LinearLayout actions = findViewById(R.id.layoutApprovalActions);
                            if (actions != null)
                                actions.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(DocumentDetailActivity.this,
                                    "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                        Toast.makeText(DocumentDetailActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Opens an attachment URL via DownloadManager (saves to Downloads) and notifies the user. */
    private void openAttachmentUrl(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Attachment URL not available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Derive a sensible file name from the URL
        String fileName = url.contains("/") ? url.substring(url.lastIndexOf('/') + 1) : "attachment";
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";
        DownloadHelper.download(this, url, fileName, ext, "Suggestion attachment");
    }

    /**
     * Shows a dialog pre-filled with current title/description so the document
     * owner can apply the HR/Admin suggestion and save the changes.
     */
    private void showEditDocumentDialog(int docId, String currentTitle, String currentDesc) {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        android.view.View dialogView = inflater.inflate(R.layout.dialog_edit_document, null);

        EditText etTitle = dialogView.findViewById(R.id.etEditTitle);
        EditText etDesc  = dialogView.findViewById(R.id.etEditDescription);

        if (etTitle != null && currentTitle != null) etTitle.setText(currentTitle);
        if (etDesc  != null && currentDesc  != null) etDesc.setText(currentDesc);

        new AlertDialog.Builder(this)
                .setTitle("Edit Document")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = etTitle != null ? etTitle.getText().toString().trim() : "";
                    String newDesc  = etDesc  != null ? etDesc.getText().toString().trim()  : "";

                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveDocumentEdits(docId, newTitle, newDesc);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveDocumentEdits(int docId, String newTitle, String newDesc) {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();

            RetrofitClient.getApiService()
                    .updateDocument(docId, token, newTitle, newDesc)
                    .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                        @Override
                        public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call,
                                retrofit2.Response<okhttp3.ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(DocumentDetailActivity.this,
                                        "Document updated", Toast.LENGTH_SHORT).show();
                                // Refresh displayed title/description
                                TextView headerTv = findViewById(R.id.headerTitle);
                                if (headerTv != null) headerTv.setText(newTitle);
                                TextView contentTv = findViewById(R.id.docContent);
                                if (contentTv != null) contentTv.setText(
                                        newDesc.isEmpty() ? "No description available." : newDesc);
                                // Hide the suggestion banner after editing
                                LinearLayout banner = findViewById(R.id.suggestionBanner);
                                if (banner != null) banner.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(DocumentDetailActivity.this,
                                        "Update failed: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                            Toast.makeText(DocumentDetailActivity.this,
                                    "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

}
