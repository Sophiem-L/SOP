package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitSuggestionActivity extends AppCompatActivity {

    private EditText etSummary;
    private EditText etComments;
    private AppCompatButton btnSubmit;
    private ImageView btnBack;
    private LinearLayout btnAttach;
    private LinearLayout layoutAttachSelected;
    private TextView tvAttachName;
    private TextView tvAttachSize;
    private ImageView btnRemoveAttach;

    private int docId = -1;
    private Uri selectedFileUri = null;
    private String selectedFileName = "";

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        onFilePicked(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_suggestion);

        docId = getIntent().getIntExtra("doc_id", -1);

        btnBack              = findViewById(R.id.btnBack);
        etSummary            = findViewById(R.id.etSummary);
        etComments           = findViewById(R.id.etComments);
        btnAttach            = findViewById(R.id.btnAttach);
        layoutAttachSelected = findViewById(R.id.layoutAttachSelected);
        tvAttachName         = findViewById(R.id.tvAttachName);
        tvAttachSize         = findViewById(R.id.tvAttachSize);
        btnRemoveAttach      = findViewById(R.id.btnRemoveAttach);
        btnSubmit            = findViewById(R.id.btnSubmit);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> openFilePicker());
        }

        if (btnRemoveAttach != null) {
            btnRemoveAttach.setOnClickListener(v -> clearAttachment());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String summary  = etSummary.getText().toString().trim();
                String comments = etComments.getText().toString().trim();

                if (summary.isEmpty()) {
                    Toast.makeText(this, "Please provide a summary", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (docId == -1) {
                    Toast.makeText(this, "No document linked to this suggestion", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitToApi(summary, comments);
            });
        }
    }

    // ── File picker ───────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePicker.launch(Intent.createChooser(intent, "Select attachment"));
    }

    private void onFilePicked(Uri uri) {
        selectedFileUri = uri;

        // Resolve display name and size
        String name = resolveFileName(uri);
        long   size = resolveFileSize(uri);
        selectedFileName = name;

        if (tvAttachName != null) tvAttachName.setText(name);
        if (tvAttachSize != null) tvAttachSize.setText(size > 0 ? formatSize(size) : "");

        // Swap visibility: hide picker, show selected card
        if (btnAttach != null)            btnAttach.setVisibility(View.GONE);
        if (layoutAttachSelected != null) layoutAttachSelected.setVisibility(View.VISIBLE);
    }

    private void clearAttachment() {
        selectedFileUri  = null;
        selectedFileName = "";
        if (layoutAttachSelected != null) layoutAttachSelected.setVisibility(View.GONE);
        if (btnAttach != null)            btnAttach.setVisibility(View.VISIBLE);
    }

    private String resolveFileName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (name == null) {
            String path = uri.getPath();
            name = (path != null && path.contains("/")) ? path.substring(path.lastIndexOf('/') + 1) : "attachment";
        }
        return name;
    }

    private long resolveFileSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024));
    }

    // ── API submission ────────────────────────────────────────────────────────

    private void submitToApi(String summary, String comments) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting\u2026");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { resetButton(); return; }

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { resetButton(); return; }
            String token = "Bearer " + task.getResult().getToken();

            // Build multipart parts
            RequestBody summaryBody  = RequestBody.create(MediaType.parse("text/plain"), summary);
            RequestBody commentsBody = RequestBody.create(MediaType.parse("text/plain"), comments);

            MultipartBody.Part attachmentPart = null;
            if (selectedFileUri != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(selectedFileUri);
                    if (is != null) {
                        byte[] bytes = readAllBytes(is);
                        is.close();
                        String mime = getContentResolver().getType(selectedFileUri);
                        if (mime == null) mime = "application/octet-stream";
                        RequestBody fileBody = RequestBody.create(MediaType.parse(mime), bytes);
                        String safeName = selectedFileName.isEmpty() ? "attachment" : selectedFileName;
                        attachmentPart = MultipartBody.Part.createFormData("attachment", safeName, fileBody);
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this,
                            "Could not read file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    resetButton();
                    return;
                }
            }

            final MultipartBody.Part finalAttachment = attachmentPart;
            RetrofitClient.getApiService()
                    .submitSuggestion(docId, token, summaryBody, commentsBody, finalAttachment)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(SubmitSuggestionActivity.this,
                                        "Suggestion submitted successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(SubmitSuggestionActivity.this,
                                        "Failed to submit: " + response.code(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(SubmitSuggestionActivity.this,
                                    "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });
        });
    }

    private byte[] readAllBytes(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = is.read(chunk)) != -1) buffer.write(chunk, 0, n);
        return buffer.toByteArray();
    }

    private void resetButton() {
        runOnUiThread(() -> {
            if (btnSubmit != null) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Submit Suggestion");
            }
        });
    }
}
