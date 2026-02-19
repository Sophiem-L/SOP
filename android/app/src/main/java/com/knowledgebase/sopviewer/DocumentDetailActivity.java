package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DocumentDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_detail);

        // Get views
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView headerTitle = findViewById(R.id.headerTitle);
        TextView docContent = findViewById(R.id.docContent);
        TextView docVersionInfo = findViewById(R.id.docVersionInfo);
        ImageView btnDownload = findViewById(R.id.btnDownload);
        android.widget.Button btnSuggestions = findViewById(R.id.btnSuggestions);

        // Setup back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup Download button
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> android.widget.Toast
                    .makeText(this, "Downloading document...", android.widget.Toast.LENGTH_SHORT).show());
        }

        // Setup Suggestions button
        if (btnSuggestions != null) {
            btnSuggestions.setOnClickListener(v -> {
                Intent intent = new Intent(this, SubmitSuggestionActivity.class);
                startActivity(intent);
            });
        }

        // Load data from Intent
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String date = getIntent().getStringExtra("date");

        if (title != null && headerTitle != null) {
            headerTitle.setText(title);
        }

        if (description != null && docContent != null) {
            docContent.setText(description);
        }

        if (date != null && docVersionInfo != null) {
            docVersionInfo.setText("Last Updated: " + date);
        }
    }
}
