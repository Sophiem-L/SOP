package com.knowledgebase.sopviewer;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class SubmitSuggestionActivity extends AppCompatActivity {

    private EditText etSummary;
    private EditText etComments;
    private AppCompatButton btnSubmit;
    private ImageView btnBack;
    private LinearLayout btnAttach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_suggestion);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        etSummary = findViewById(R.id.etSummary);
        etComments = findViewById(R.id.etComments);
        btnAttach = findViewById(R.id.btnAttach);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Setup back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Setup attachment button (mock)
        if (btnAttach != null) {
            btnAttach.setOnClickListener(
                    v -> Toast.makeText(this, "Attachment feature coming soon", Toast.LENGTH_SHORT).show());
        }

        // Setup submit button
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String summary = etSummary.getText().toString().trim();
                String comments = etComments.getText().toString().trim();

                if (summary.isEmpty()) {
                    Toast.makeText(this, "Please provide a summary", Toast.LENGTH_SHORT).show();
                    return;
                }

                // In a real app, you would send this to the API
                Toast.makeText(this, "Suggestion submitted successfully!", Toast.LENGTH_LONG).show();
                finish();
            });
        }
    }
}
