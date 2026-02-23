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

    // PDF file details
    private LinearLayout layoutFileDetails;
    private TextView txtFileName, txtProgressPercent, txtFileSize;
    private ProgressBar progressBar;

    // DOC file details
    private LinearLayout layoutDocFileDetails;
    private TextView txtDocFileName, txtDocProgressPercent, txtDocFileSize;
    private ProgressBar progressBarDoc;

    private final ActivityResultLauncher<Intent> pdfPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handlePdfSelection(uri);
                }
            });

    private final ActivityResultLauncher<Intent> docPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handleDocSelection(uri);
                }
            });

    private LinearLayout btnTypePdf, btnTypeHtml;
    private ImageView imgPdf, imgHtml;
    private TextView txtPdf, txtHtml;
    private View sectionPdfUpload, sectionHtmlEditor;
    private EditText editTitle, editVersion, editNewCategory;
    private TextView textPublishedDate;
    private Spinner spinnerCategory;
    private final Calendar calendar = Calendar.getInstance();
    private Uri selectedPdfUri;
    private Uri selectedDocUri;
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
        updateDocumentTypeUI(true);

        // PDF file details
        layoutFileDetails = findViewById(R.id.layoutFileDetails);
        txtFileName = findViewById(R.id.txtFileName);
        txtProgressPercent = findViewById(R.id.txtProgressPercent);
        txtFileSize = findViewById(R.id.txtFileSize);
        progressBar = findViewById(R.id.progressBar);

        // DOC file details
        layoutDocFileDetails = findViewById(R.id.layoutDocFileDetails);
        txtDocFileName = findViewById(R.id.txtDocFileName);
        txtDocProgressPercent = findViewById(R.id.txtDocProgressPercent);
        txtDocFileSize = findViewById(R.id.txtDocFileSize);
        progressBarDoc = findViewById(R.id.progressBarDoc);

        TextView btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());

        // PDF browse
        findViewById(R.id.btnBrowseFiles).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pdfPickerLauncher.launch(intent);
        });

        // DOC browse
        findViewById(R.id.btnBrowseDocFiles).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            });
            docPickerLauncher.launch(intent);
        });

        editTitle = findViewById(R.id.editTitle);
        editVersion = findViewById(R.id.editVersion);
        textPublishedDate = findViewById(R.id.textPublishedDate);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        editNewCategory = findViewById(R.id.editNewCategory);

        findViewById(R.id.containerPublishedDate).setOnClickListener(v -> showDatePickerDialog());

        fetchCategories();

        Button btnPublish = findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(v -> publishDocument());

        Button btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnSaveDraft.setOnClickListener(v ->
                Toast.makeText(this, "Draft Saved", Toast.LENGTH_SHORT).show());

        Button btnPreview = findViewById(R.id.btnPreview);
        btnPreview.setOnClickListener(v ->
                Toast.makeText(this, "Opening Preview...", Toast.LENGTH_SHORT).show());
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
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                categories.clear();
                                categories.add(new Category("Select Category"));
                                categories.addAll(response.body());
                                updateCategoryAdapter();
                                spinnerCategory.setVisibility(View.VISIBLE);
                                editNewCategory.setVisibility(View.GONE);
                            } else {
                                spinnerCategory.setVisibility(View.GONE);
                                editNewCategory.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Category>> call, Throwable t) {
                            spinnerCategory.setVisibility(View.GONE);
                            editNewCategory.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } else {
            spinnerCategory.setVisibility(View.GONE);
            editNewCategory.setVisibility(View.VISIBLE);
        }
    }

    private void updateCategoryAdapter() {
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
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

        String categoryId = "";
        String categoryName = "";
        if (editNewCategory.getVisibility() == View.VISIBLE) {
            categoryName = editNewCategory.getText().toString().trim();
            if (categoryName.isEmpty()) {
                editNewCategory.setError("Category name is required");
                return;
            }
        } else {
            Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
            if (selectedCategory != null && selectedCategory.getId() > 0) {
                categoryId = String.valueOf(selectedCategory.getId());
            }
        }

        Uri selectedFileUri = isPdfMode ? selectedPdfUri : selectedDocUri;
        if (selectedFileUri == null) {
            Toast.makeText(this,
                    isPdfMode ? "Please select a PDF file" : "Please select a DOC file",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] fileBytes;
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            fileBytes = getBytes(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String mimeType = isPdfMode ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String fileName = isPdfMode ? "document.pdf" : "document.docx";
        String type = isPdfMode ? "pdf" : "doc";

        RequestBody titlePart = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody typePart = RequestBody.create(MediaType.parse("text/plain"), type);
        RequestBody categoryIdPart = RequestBody.create(MediaType.parse("text/plain"), categoryId);
        RequestBody categoryNamePart = RequestBody.create(MediaType.parse("text/plain"), categoryName);
        RequestBody contentPart = RequestBody.create(MediaType.parse("text/plain"), "");
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), fileBytes);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestFile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = "Bearer " + task.getResult().getToken();
                    RetrofitClient.getApiService()
                            .createDocument(idToken, titlePart, typePart, categoryIdPart,
                                    categoryNamePart, contentPart, filePart)
                            .enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(CreateDocumentActivity.this,
                                                "Document Published Successfully", Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK);
                                        finish();
                                    } else {
                                        Toast.makeText(CreateDocumentActivity.this,
                                                "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Toast.makeText(CreateDocumentActivity.this,
                                            "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
        }
    }

    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "MMM dd, yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
            textPublishedDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
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

    private void handlePdfSelection(Uri uri) {
        this.selectedPdfUri = uri;
        String fileName = getFileName(uri);
        long fileSize = getFileSize(uri);

        layoutFileDetails.setVisibility(View.VISIBLE);
        txtFileName.setText(fileName);
        progressBar.setProgress(100);
        txtProgressPercent.setText("100%");
        txtFileSize.setText(String.format("%.1f MB of 5 MB", fileSize / (1024.0 * 1024.0)));

        if (fileSize > 5 * 1024 * 1024) {
            Toast.makeText(this, "File too large! Max 5MB.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleDocSelection(Uri uri) {
        this.selectedDocUri = uri;
        String fileName = getFileName(uri);
        long fileSize = getFileSize(uri);

        layoutDocFileDetails.setVisibility(View.VISIBLE);
        txtDocFileName.setText(fileName);
        progressBarDoc.setProgress(100);
        txtDocProgressPercent.setText("100%");
        txtDocFileSize.setText(String.format("%.1f MB of 5 MB", fileSize / (1024.0 * 1024.0)));

        if (fileSize > 5 * 1024 * 1024) {
            Toast.makeText(this, "File too large! Max 5MB.", Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "Unknown";
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) fileName = cursor.getString(nameIndex);
            cursor.close();
        }
        return fileName;
    }

    private long getFileSize(Uri uri) {
        long fileSize = 0;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
            cursor.close();
        }
        return fileSize;
    }
}
