package com.knowledgebase.sopviewer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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

        android.widget.Button btnSuggestions = findViewById(R.id.btnSuggestions);
        if (btnSuggestions != null) {
            btnSuggestions.setVisibility(View.GONE);
            btnSuggestions.setOnClickListener(v -> startActivity(new Intent(this, SubmitSuggestionActivity.class)));
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
        LinearLayout pdfPagesContainer = findViewById(R.id.pdfPagesContainer);
        TextView pdfErrorText = findViewById(R.id.pdfErrorText);
        LinearLayout docViewerContainer = findViewById(R.id.docViewerContainer);
        ProgressBar docLoadingBar = findViewById(R.id.docLoadingBar);
        TextView docStatusText = findViewById(R.id.docStatusText);
        MaterialButton btnOpenDoc = findViewById(R.id.btnOpenDoc);

        boolean isPdf = "pdf".equalsIgnoreCase(fileType);
        final boolean hasPdf = isPdf && !resolvedFileUrl.isEmpty();
        final boolean hasDoc = !isPdf && !resolvedFileUrl.isEmpty();

        // Show the preview container immediately so the loading spinner is visible
        // right away — the actual content loads asynchronously after the token is fetched.
        if (hasPdf) {
            pdfContainer.setVisibility(View.VISIBLE);
        } else if (hasDoc) {
            docViewerContainer.setVisibility(View.VISIBLE);
        }

        if ((hasPdf || hasDoc) && docId != -1) {
            // Use the authenticated streaming endpoint instead of the raw storage URL.
            // This avoids the Windows php artisan serve truncation bug and handles auth.
            String serveUrl = RetrofitClient.BASE_URL + "api/documents/" + docId + "/file";

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                showError(pdfLoadingBar, pdfErrorText, "Not signed in");
                return;
            }

            currentUser.getIdToken(false)
                    .addOnCompleteListener(tokenTask -> {
                        if (!tokenTask.isSuccessful()) {
                            if (hasPdf)
                                showError(pdfLoadingBar, pdfErrorText, "Authentication failed");
                            else
                                runOnUiThread(() -> {
                                    docLoadingBar.setVisibility(View.GONE);
                                    docStatusText.setText("Authentication failed");
                                    docStatusText.setVisibility(View.VISIBLE);
                                });
                            return;
                        }
                        String bearerToken = "Bearer " + tokenTask.getResult().getToken();
                        if (hasPdf) {
                            loadPdfInline(serveUrl, bearerToken, pdfPagesContainer, pdfLoadingBar, pdfErrorText);
                        } else {
                            downloadDocForViewing(serveUrl, bearerToken, fileType, docLoadingBar, docStatusText,
                                    btnOpenDoc);
                        }
                    });
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
     * Fetches the PDF from the API as a Base64 JSON payload and renders it inline.
     *
     * The backend returns {"data":"<base64>","mime":"pdf"} instead of raw bytes.
     * This avoids the "unexpected end of stream" error: PHP artisan serve on Windows
     * wraps every binary response in chunked transfer encoding whose final chunk
     * Android never receives correctly. JSON is plain text — artisan serve transfers
     * it without chunking issues — and Base64.decode() restores the original bytes.
     */
    private void loadPdfInline(String url, String bearerToken, LinearLayout container,
            ProgressBar loadingBar, TextView errorView) {
        Log.d(TAG, "loadPdfInline URL: " + url);
        new Thread(() -> {
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .header("Authorization", bearerToken)
                    .build();
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError(loadingBar, errorView, "HTTP " + response.code());
                    return;
                }
                // Response is JSON: {"data":"<base64 PDF bytes>","mime":"pdf"}
                String json = response.body().string();
                org.json.JSONObject obj = new org.json.JSONObject(json);
                String base64Data = obj.getString("data");
                byte[] pdfBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);

                java.io.File cacheFile = new java.io.File(getCacheDir(), "preview.pdf");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                    fos.write(pdfBytes);
                }
                renderPdfFromFile(cacheFile, container, loadingBar, errorView);
            } catch (Exception e) {
                Log.e(TAG, "PDF load error: " + e.getMessage());
                showError(loadingBar, errorView, "Load error: " + e.getMessage());
            }
        }).start();
    }

    private void showError(ProgressBar loadingBar, TextView errorView, String message) {
        Log.e(TAG, "PDF preview error: " + message);
        runOnUiThread(() -> {
            loadingBar.setVisibility(View.GONE);
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        });
    }

    private void renderPdfFromFile(java.io.File file, LinearLayout container,
            ProgressBar loadingBar, TextView errorView) {
        try {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                    file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();
            if (pageCount == 0) {
                renderer.close();
                pfd.close();
                showError(loadingBar, errorView, "PDF has no pages");
                return;
            }
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int padding = (int) (48 * getResources().getDisplayMetrics().density);
            int renderWidth = screenWidth - padding;

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                int renderHeight = (int) ((float) page.getHeight() / page.getWidth() * renderWidth);
                Bitmap bitmap = Bitmap.createBitmap(renderWidth, renderHeight,
                        Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                final Bitmap finalBitmap = bitmap;
                final boolean isLast = (i == pageCount - 1);
                runOnUiThread(() -> {
                    ImageView iv = new ImageView(DocumentDetailActivity.this);
                    iv.setImageBitmap(finalBitmap);
                    iv.setAdjustViewBounds(true);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, 0, 0, 8);
                    iv.setLayoutParams(lp);
                    container.addView(iv);
                    if (isLast)
                        loadingBar.setVisibility(View.GONE);
                });
            }
            renderer.close();
            pfd.close();
        } catch (Exception e) {
            Log.e(TAG, "PdfRenderer error: " + e.getMessage());
            showError(loadingBar, errorView, "Render error: " + e.getMessage());
        }
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
    private void downloadDocForViewing(String url, String bearerToken, String fileType,
            ProgressBar loadingBar, TextView statusText, MaterialButton openButton) {
        new Thread(() -> {
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestProperty("Authorization", bearerToken);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    conn.disconnect();
                    runOnUiThread(() -> {
                        loadingBar.setVisibility(View.GONE);
                        statusText.setText("Cannot load document (HTTP " + code + ")");
                        statusText.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                String ext = "doc".equalsIgnoreCase(fileType) ? "doc" : "docx";
                java.io.File cacheFile = new java.io.File(getCacheDir(), "preview." + ext);
                try (java.io.InputStream is = new java.io.BufferedInputStream(conn.getInputStream());
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1)
                        fos.write(buf, 0, n);
                } finally {
                    conn.disconnect();
                }
                Uri fileUri = FileProvider.getUriForFile(
                        DocumentDetailActivity.this,
                        "com.knowledgebase.sopviewer.fileprovider",
                        cacheFile);
                String mime = "doc".equalsIgnoreCase(fileType)
                        ? "application/msword"
                        : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
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

}
