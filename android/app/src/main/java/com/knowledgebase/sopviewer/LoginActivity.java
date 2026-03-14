package com.knowledgebase.sopviewer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button signInButton, ssoButton;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    // Backend URL for the Google OAuth start point
    private static final String GOOGLE_AUTH_URL = RetrofitClient.BASE_URL + "auth/google";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton  = findViewById(R.id.signInButton);
        ssoButton     = findViewById(R.id.ssoButton);

        signInButton.setOnClickListener(v -> loginWithEmail());
        ssoButton.setOnClickListener(v -> loginWithGoogle());

        // Auto-navigate if already signed in
        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Email / Password login
    // ────────────────────────────────────────────────────────────────────
    private void loginWithEmail() {
        String email    = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        showProgress("Signing in…");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    syncWithBackend(tokenTask.getResult().getToken());
                                } else {
                                    dismissProgressDialog();
                                    Toast.makeText(this, "Failed to get ID token",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        dismissProgressDialog();
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Authentication failed";
                        Toast.makeText(this, "Sign-in failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ────────────────────────────────────────────────────────────────────
    // Google SSO — opens the backend's Google OAuth page in a Chrome Custom Tab.
    // The backend handles all Google OAuth, then redirects back via deep link
    // sopviewer://auth?token=... which is caught by AuthCallbackActivity.
    // ────────────────────────────────────────────────────────────────────
    private void loginWithGoogle() {
        CustomTabsIntent customTab = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
        customTab.launchUrl(this, Uri.parse(GOOGLE_AUTH_URL));
    }

    // ────────────────────────────────────────────────────────────────────
    // Shared backend sync — called after Firebase sign-in (email flow)
    // ────────────────────────────────────────────────────────────────────
    void syncWithBackend(String token) {
        String bearerToken = "Bearer " + token;
        RetrofitClient.getApiService().login(bearerToken).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                dismissProgressDialog();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(LoginActivity.this, "Signed in successfully",
                            Toast.LENGTH_SHORT).show();
                    navigateToMain();
                } else {
                    mAuth.signOut();
                    String msg = "Backend sync failed: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String body = response.errorBody().string();
                            int key = body.indexOf("\"message\":\"");
                            if (key >= 0) {
                                int start = key + 12;
                                int end = body.indexOf("\"", start);
                                if (end > start) msg = body.substring(start, end);
                            }
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                dismissProgressDialog();
                mAuth.signOut();
                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────
    void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
