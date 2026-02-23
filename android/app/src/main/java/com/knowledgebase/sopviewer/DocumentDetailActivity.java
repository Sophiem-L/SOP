package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DocumentDetailActivity extends AppCompatActivity {

    private static final String TAG = "PDFLoader";

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // Keep fileUrl as a field so the fallback button can use it
    private String resolvedFileUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        String title    = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String date     = getIntent().getStringExtra("date");
        String fileUrl  = getIntent().getStringExtra("file_url");
        String fileType = getIntent().getStringExtra("file_type");
        String category = getIntent().getStringExtra("category");
        String version  = getIntent().getStringExtra("version");

        // Resolve the URL once for the whole activity
        resolvedFileUrl = resolveUrl(fileUrl);
        Log.d(TAG, "file_url from intent: " + fileUrl);
        Log.d(TAG, "resolved URL: " + resolvedFileUrl);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView headerTitle = findViewById(R.id.headerTitle);
        if (title != null) headerTitle.setText(title);

        TextView docCategory = findViewById(R.id.docCategory);
        docCategory.setText(category != null && !category.isEmpty() ? category : "Uncategorized");

        TextView docFileType = findViewById(R.id.docFileType);
        docFileType.setText(fileType != null && !fileType.isEmpty() ? fileType.toUpperCase() : "DOC");

        TextView docContent = findViewById(R.id.docContent);
        docContent.setText(description != null && !description.isEmpty() ? description : "No description available.");

        TextView docVersionInfo = findViewById(R.id.docVersionInfo);
        String versionText = "Version: " + (version != null && !version.isEmpty() ? version : "1.0.0");
        if (date != null && !date.isEmpty()) versionText += "\n" + date;
        docVersionInfo.setText(versionText);

        // Download icon always opens externally
        ImageView btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> openFileExternal(resolvedFileUrl));

        LinearLayout pdfContainer      = findViewById(R.id.pdfContainer);
        ProgressBar  pdfLoadingBar     = findViewById(R.id.pdfLoadingBar);
        LinearLayout pdfPagesContainer = findViewById(R.id.pdfPagesContainer);
        MaterialButton btnOpen         = findViewById(R.id.btnOpenDocument);

        boolean isPdf = "pdf".equalsIgnoreCase(fileType);

        if (isPdf && !resolvedFileUrl.isEmpty()) {
            pdfContainer.setVisibility(View.VISIBLE);
            btnOpen.setVisibility(View.GONE);
            // Pre-wire the fallback button in case rendering fails
            btnOpen.setOnClickListener(v -> openFileExternal(resolvedFileUrl));
            loadPdfInline(resolvedFileUrl, pdfPagesContainer, pdfLoadingBar, btnOpen);
        } else {
            pdfContainer.setVisibility(View.GONE);
            btnOpen.setVisibility(View.VISIBLE);
            if (resolvedFileUrl.isEmpty()) {
                btnOpen.setEnabled(false);
                btnOpen.setText("No file available");
            } else {
                btnOpen.setOnClickListener(v -> openFileExternal(resolvedFileUrl));
            }
        }

        android.widget.Button btnSuggestions = findViewById(R.id.btnSuggestions);
        if (btnSuggestions != null) {
            btnSuggestions.setOnClickListener(v ->
                    startActivity(new Intent(this, SubmitSuggestionActivity.class)));
        }
    }

    /** Convert any localhost variant to the emulator host alias. */
    private String resolveUrl(String url) {
        if (url == null || url.isEmpty()) return "";
        return url
                .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                .replace("http://localhost/",       "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/")
                .replace("http://127.0.0.1/",      "http://10.0.2.2:8000/");
    }

    private void loadPdfInline(String url, LinearLayout pagesContainer,
                               ProgressBar loadingBar, MaterialButton fallbackBtn) {

        String cacheFileName = "pdf_" + Math.abs(url.hashCode()) + ".pdf";
        File cachedFile = new File(getCacheDir(), cacheFileName);
        Log.d(TAG, "Cache path: " + cachedFile.getAbsolutePath());

        if (cachedFile.exists() && cachedFile.length() > 0) {
            Log.d(TAG, "Cache hit — rendering from local file (" + cachedFile.length() + " bytes)");
            renderPdfFromFile(cachedFile, pagesContainer, loadingBar, fallbackBtn);
            return;
        }

        Log.d(TAG, "Downloading from: " + url);
        httpClient.newCall(new Request.Builder().url(url).build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "onFailure: " + e.getMessage(), e);
                        runOnUiThread(() -> {
                            loadingBar.setVisibility(View.GONE);
                            fallbackBtn.setVisibility(View.VISIBLE);
                            Toast.makeText(DocumentDetailActivity.this,
                                    "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        // Wrap everything so no IOException leaks and spins the loader forever
                        try {
                            if (!response.isSuccessful() || response.body() == null) {
                                int code = response.code();
                                Log.e(TAG, "HTTP error: " + code + " for " + url);
                                runOnUiThread(() -> {
                                    loadingBar.setVisibility(View.GONE);
                                    fallbackBtn.setVisibility(View.VISIBLE);
                                    Toast.makeText(DocumentDetailActivity.this,
                                            "Could not load PDF (HTTP " + code + ")", Toast.LENGTH_LONG).show();
                                });
                                return;
                            }

                            byte[] bytes = response.body().bytes();
                            Log.d(TAG, "Downloaded " + bytes.length + " bytes");

                            try (FileOutputStream fos = new FileOutputStream(cachedFile)) {
                                fos.write(bytes);
                            }

                            renderPdfFromFile(cachedFile, pagesContainer, loadingBar, fallbackBtn);

                        } catch (Exception e) {
                            Log.e(TAG, "onResponse error: " + e.getMessage(), e);
                            // Delete partial cache file
                            cachedFile.delete();
                            runOnUiThread(() -> {
                                loadingBar.setVisibility(View.GONE);
                                fallbackBtn.setVisibility(View.VISIBLE);
                                Toast.makeText(DocumentDetailActivity.this,
                                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
    }

    private void renderPdfFromFile(File pdfFile, LinearLayout pagesContainer,
                                   ProgressBar loadingBar, MaterialButton fallbackBtn) {
        new Thread(() -> {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                    pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                 PdfRenderer renderer = new PdfRenderer(pfd)) {

                int pageCount = renderer.getPageCount();
                Log.d(TAG, "PdfRenderer opened — " + pageCount + " pages");

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int paddingPx   = (int) (48 * getResources().getDisplayMetrics().density);
                int pageWidth   = screenWidth - paddingPx;

                Bitmap[] bitmaps = new Bitmap[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    int pageHeight = (int) (pageWidth * page.getHeight() / (float) page.getWidth());
                    Bitmap bmp = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.WHITE);
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();
                    bitmaps[i] = bmp;
                    Log.d(TAG, "Rendered page " + (i + 1) + "/" + pageCount);
                }

                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    loadingBar.setVisibility(View.GONE);
                    pagesContainer.setVisibility(View.VISIBLE);

                    int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
                    for (Bitmap bmp : bitmaps) {
                        ImageView img = new ImageView(DocumentDetailActivity.this);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(0, 0, 0, marginPx);
                        img.setLayoutParams(lp);
                        img.setAdjustViewBounds(true);
                        img.setImageBitmap(bmp);
                        pagesContainer.addView(img);
                    }
                    Log.d(TAG, "All pages displayed");
                });

            } catch (Throwable e) {
                Log.e(TAG, "Render error: " + e.getMessage(), e);
                // Delete corrupt cache so next open re-downloads
                pdfFile.delete();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    loadingBar.setVisibility(View.GONE);
                    fallbackBtn.setVisibility(View.VISIBLE);
                    Toast.makeText(DocumentDetailActivity.this,
                            "Cannot render PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void openFileExternal(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "No file available", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }
}
