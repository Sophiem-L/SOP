package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.app.DatePickerDialog;
import java.util.Locale;
import java.text.SimpleDateFormat;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateDocumentActivity extends AppCompatActivity {

    private LinearLayout layoutFileDetails;
    private TextView txtFileName, txtProgressPercent, txtFileSize;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleFileSelection(uri);
                    }
                }
            });

    private LinearLayout btnTypePdf, btnTypeHtml;
    private ImageView imgPdf, imgHtml;
    private TextView txtPdf, txtHtml;
    private View sectionPdfUpload, sectionHtmlEditor;
    private EditText editTitle, editDocumentContent, editVersion;
    private TextView textPublishedDate;
    private Spinner spinnerCategory;
    private final Calendar calendar = Calendar.getInstance();
    private Uri selectedFileUri;
    private boolean isPdfMode = true;
    private List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_document);

        btnTypePdf = findViewById(R.id.btnTypePdf);
        btnTypeHtml = findViewById(R.id.btnTypeHtml);
        imgPdf = findViewById(R.id.imgPdf);
        imgHtml = findViewById(R.id.imgHtml);
        txtPdf = findViewById(R.id.txtPdf);
        txtHtml = findViewById(R.id.txtHtml);
        sectionPdfUpload = findViewById(R.id.sectionPdfUpload);
        sectionHtmlEditor = findViewById(R.id.sectionHtmlEditor);

        btnTypePdf.setOnClickListener(v -> updateDocumentTypeUI(true));
        btnTypeHtml.setOnClickListener(v -> updateDocumentTypeUI(false));

        // Initial state
        updateDocumentTypeUI(true);

        layoutFileDetails = findViewById(R.id.layoutFileDetails);
        txtFileName = findViewById(R.id.txtFileName);
        txtProgressPercent = findViewById(R.id.txtProgressPercent);
        txtFileSize = findViewById(R.id.txtFileSize);
        progressBar = findViewById(R.id.progressBar);

        TextView btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        findViewById(R.id.btnBrowseFiles).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            filePickerLauncher.launch(intent);
        });

        editTitle = findViewById(R.id.editTitle);
        editDocumentContent = findViewById(R.id.editDocumentContent);
        editVersion = findViewById(R.id.editVersion);
        textPublishedDate = findViewById(R.id.textPublishedDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        findViewById(R.id.containerPublishedDate).setOnClickListener(v -> showDatePickerDialog());

        // Initialize with default categories immediately so it's not empty
        categories.clear();
        categories.add(new Category("Select Category"));
        categories.add(new Category("Policies"));
        categories.add(new Category("HR Document"));
        categories.add(new Category("Security"));
        categories.add(new Category("Finance"));
        categories.add(new Category("Management"));
        updateCategoryAdapter();

        setupHtmlToolbar();

        fetchCategories();

        Button btnPublish = findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(v -> publishDocument());

        Button btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnSaveDraft.setOnClickListener(v -> {
            Toast.makeText(this, "Draft Saved", Toast.LENGTH_SHORT).show();
        });

        Button btnPreview = findViewById(R.id.btnPreview);
        btnPreview.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Preview...", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchCategories() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    RetrofitClient.getApiService().getCategories(idToken).enqueue(new Callback<List<Category>>() {
                        @Override
                        public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                categories.clear();
                                categories.add(new Category("Select Category"));
                                categories.addAll(response.body());
                                updateCategoryAdapter();
                            } else {
                                Toast.makeText(CreateDocumentActivity.this, "Server error: " + response.code(),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Category>> call, Throwable t) {
                            Toast.makeText(CreateDocumentActivity.this, "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
            });
        }
    }

    private void updateCategoryAdapter() {
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(CreateDocumentActivity.this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void publishDocument() {
        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            editTitle.setError("Title is required");
            return;
        }

        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        String categoryId = "";
        if (selectedCategory != null && selectedCategory.getId() > 0) {
            categoryId = String.valueOf(selectedCategory.getId());
        }

        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody typePart = RequestBody.create(MediaType.parse("text/plain"), isPdfMode ? "pdf" : "html");
        RequestBody categoryIdPart = RequestBody.create(MediaType.parse("text/plain"), categoryId);

        MultipartBody.Part filePart = null;
        RequestBody contentPart = null;

        if (isPdfMode) {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a PDF file", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                byte[] bytes = getBytes(inputStream);
                RequestBody requestFile = RequestBody.create(MediaType.parse("application/pdf"), bytes);
                filePart = MultipartBody.Part.createFormData("file", "document.pdf", requestFile);
                contentPart = RequestBody.create(MediaType.parse("text/plain"), "");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String content = editDocumentContent.getText().toString().trim();
            if (content.isEmpty()) {
                editDocumentContent.setError("Content is required");
                return;
            }
            contentPart = RequestBody.create(MediaType.parse("text/plain"), content);
            // filePart remains null for HTML
        }

        final MultipartBody.Part finalFilePart = filePart;
        final RequestBody finalContentPart = contentPart;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    RetrofitClient.getApiService()
                            .createDocument(idToken, titlePart, typePart, categoryIdPart, finalContentPart,
                                    finalFilePart)
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(CreateDocumentActivity.this, "Document Published Successfully",
                                                Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        Toast.makeText(CreateDocumentActivity.this, "Failed: " + response.code(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Toast.makeText(CreateDocumentActivity.this, "Error: " + t.getMessage(),
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            });
                }
            });
        }
    }

    private void setupHtmlToolbar() {
        findViewById(R.id.imgFormatHeading).setOnClickListener(v -> wrapSelection("<h1>", "</h1>"));
        findViewById(R.id.imgFormatBold).setOnClickListener(v -> wrapSelection("<b>", "</b>"));
        findViewById(R.id.imgFormatItalic).setOnClickListener(v -> wrapSelection("<i>", "</i>"));
        findViewById(R.id.imgFormatList).setOnClickListener(v -> wrapSelection("<ul>\n  <li>", "</li>\n</ul>"));
        findViewById(R.id.imgFormatLink).setOnClickListener(v -> wrapSelection("<a href=\"\">", "</a>"));
        findViewById(R.id.imgFormatImage)
                .setOnClickListener(v -> wrapSelection("<img src=\"", "\" alt=\"description\" />"));
        findViewById(R.id.imgFormatTable).setOnClickListener(v -> {
            String tableHtml = "\n<table border=\"1\">\n" +
                    "  <tr>\n" +
                    "    <td>Cell 1</td>\n" +
                    "    <td>Cell 2</td>\n" +
                    "  </tr>\n" +
                    "</table>\n";
            insertAtCursor(tableHtml);
        });
    }

    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateLabel() {
        String myFormat = "MMM dd, yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        textPublishedDate.setText(sdf.format(calendar.getTime()));
    }

    private void wrapSelection(String startTag, String endTag) {
        int start = editDocumentContent.getSelectionStart();
        int end = editDocumentContent.getSelectionEnd();
        String text = editDocumentContent.getText().toString();

        if (start != end) {
            String selectedText = text.substring(start, end);
            String newText = text.substring(0, start) + startTag + selectedText + endTag + text.substring(end);
            editDocumentContent.setText(newText);
            editDocumentContent.setSelection(start + startTag.length() + selectedText.length() + endTag.length());
        } else {
            String newText = text.substring(0, start) + startTag + endTag + text.substring(end);
            editDocumentContent.setText(newText);
            editDocumentContent.setSelection(start + startTag.length());
        }
    }

    private void insertAtCursor(String textToInsert) {
        int start = editDocumentContent.getSelectionStart();
        int end = editDocumentContent.getSelectionEnd();
        String text = editDocumentContent.getText().toString();

        String newText = text.substring(0, Math.min(start, end)) + textToInsert + text.substring(Math.max(start, end));
        editDocumentContent.setText(newText);
        editDocumentContent.setSelection(start + textToInsert.length());
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void updateDocumentTypeUI(boolean isPdf) {
        if (isPdf) {
            btnTypePdf.setBackgroundResource(R.drawable.bg_rounded_card);
            btnTypePdf.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_blue)));
            imgPdf.setColorFilter(getResources().getColor(R.color.white));
            txtPdf.setTextColor(getResources().getColor(R.color.white));
            txtPdf.setTypeface(null, android.graphics.Typeface.BOLD);

            btnTypeHtml.setBackgroundResource(R.drawable.bg_rounded_search);
            btnTypeHtml.setBackgroundTintList(null);
            imgHtml.setColorFilter(getResources().getColor(R.color.black));
            txtHtml.setTextColor(getResources().getColor(R.color.black));
            txtHtml.setTypeface(null, android.graphics.Typeface.NORMAL);

            sectionPdfUpload.setVisibility(View.VISIBLE);
            sectionHtmlEditor.setVisibility(View.GONE);
            isPdfMode = true;
        } else {
            btnTypeHtml.setBackgroundResource(R.drawable.bg_rounded_card);
            btnTypeHtml.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.brand_blue)));
            imgHtml.setColorFilter(getResources().getColor(R.color.white));
            txtHtml.setTextColor(getResources().getColor(R.color.white));
            txtHtml.setTypeface(null, android.graphics.Typeface.BOLD);

            btnTypePdf.setBackgroundResource(R.drawable.bg_rounded_search);
            btnTypePdf.setBackgroundTintList(null);
            imgPdf.setColorFilter(getResources().getColor(R.color.black));
            txtPdf.setTextColor(getResources().getColor(R.color.black));
            txtPdf.setTypeface(null, android.graphics.Typeface.NORMAL);

            sectionHtmlEditor.setVisibility(View.VISIBLE);
            sectionPdfUpload.setVisibility(View.GONE);
            isPdfMode = false;
        }
    }

    private void handleFileSelection(Uri uri) {
        this.selectedFileUri = uri;
        String fileName = "Unknown";
        long fileSize = 0;

        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (nameIndex != -1)
                fileName = cursor.getString(nameIndex);
            if (sizeIndex != -1)
                fileSize = cursor.getLong(sizeIndex);
            cursor.close();
        }

        layoutFileDetails.setVisibility(View.VISIBLE);
        txtFileName.setText(fileName);

        // Mock progress for UI demonstration
        progressBar.setProgress(100);
        txtProgressPercent.setText("100%");

        String sizeText = String.format("%.1f MB of 5 MB", fileSize / (1024.0 * 1024.0));
        txtFileSize.setText(sizeText);

        if (fileSize > 5 * 1024 * 1024) {
            Toast.makeText(this, "File too large! Max 5MB.", Toast.LENGTH_LONG).show();
        }
    }
}
