package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editJobTitle, editPhone, editEmail;
    private TextView btnSave;
    private FirebaseAuth mAuth;
    private android.view.View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        editName = findViewById(R.id.editName);
        editJobTitle = findViewById(R.id.editJobTitle);
        editPhone = findViewById(R.id.editPhone);
        editEmail = findViewById(R.id.editEmail);
        btnSave = findViewById(R.id.btnSave);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Pre-fill fields from intent or Firebase
        editName.setText(getIntent().getStringExtra("name"));
        editJobTitle.setText(getIntent().getStringExtra("job_title"));
        editPhone.setText(getIntent().getStringExtra("phone"));
        editEmail.setText(user.getEmail());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String jobTitle = editJobTitle.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError("Name is required");
            return;
        }

        loadingOverlay.setVisibility(android.view.View.VISIBLE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String token = "Bearer " + task.getResult().getToken();

                    Map<String, String> fields = new HashMap<>();
                    fields.put("name", name);
                    fields.put("full_name", name);
                    fields.put("job_title", jobTitle);
                    fields.put("phone", phone);

                    RetrofitClient.getApiService().updateProfile(token, fields).enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            loadingOverlay.setVisibility(android.view.View.GONE);
                            if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                                Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                                User updated = response.body().getUser();
                                Intent resultData = new Intent();
                                String name = (updated.getFullName() != null && !updated.getFullName().isEmpty())
                                        ? updated.getFullName() : (updated.getName() != null ? updated.getName() : "");
                                resultData.putExtra("name", name);
                                resultData.putExtra("job_title", updated.getJobTitle() != null ? updated.getJobTitle() : "");
                                resultData.putExtra("phone", updated.getPhone() != null ? updated.getPhone() : "");
                                setResult(RESULT_OK, resultData);
                                finish();
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Update failed: " + (response.code() == 0 ? "parse error" : response.code()),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            loadingOverlay.setVisibility(android.view.View.GONE);
                            Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    loadingOverlay.setVisibility(android.view.View.GONE);
                }
            });
        }
    }
}
