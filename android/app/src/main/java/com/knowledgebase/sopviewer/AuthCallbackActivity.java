package com.knowledgebase.sopviewer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles the deep link redirect from the backend's Google OAuth flow.
 *
 * Deep link format:  sopviewer://auth?token=FIREBASE_CUSTOM_TOKEN
 *
 * Flow:
 *   1. Backend redirects here after successful Google OAuth
 *   2. Extract custom token from the URI
 *   3. Sign into Firebase with the custom token
 *   4. Get a Firebase ID token
 *   5. Sync with the backend API (same as email flow)
 *   6. Navigate to MainActivity
 */
public class AuthCallbackActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        Uri data = getIntent().getData();
        if (data != null && "sopviewer".equals(data.getScheme())) {
            String customToken = data.getQueryParameter("token");
            if (customToken != null && !customToken.isEmpty()) {
                handleCustomToken(customToken);
                return;
            }
        }

        // No valid token — go back to login
        Toast.makeText(this, "Sign-in failed: no token received", Toast.LENGTH_LONG).show();
        goToLogin();
    }

    private void handleCustomToken(String customToken) {
        showProgress("Verifying with Firebase…");

        mAuth.signInWithCustomToken(customToken)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        updateProgress("Syncing with server…");
                        mAuth.getCurrentUser().getIdToken(true)
                                .addOnCompleteListener(this, tokenTask -> {
                                    if (tokenTask.isSuccessful()) {
                                        syncWithBackend(tokenTask.getResult().getToken());
                                    } else {
                                        dismissProgress();
                                        mAuth.signOut();
                                        Toast.makeText(this, "Failed to get Firebase token",
                                                Toast.LENGTH_SHORT).show();
                                        goToLogin();
                                    }
                                });
                    } else {
                        dismissProgress();
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Firebase sign-in failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        goToLogin();
                    }
                });
    }

    private void syncWithBackend(String firebaseIdToken) {
        String bearerToken = "Bearer " + firebaseIdToken;
        RetrofitClient.getApiService().login(bearerToken).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                dismissProgress();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(AuthCallbackActivity.this, "Signed in successfully",
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
                    Toast.makeText(AuthCallbackActivity.this, msg, Toast.LENGTH_LONG).show();
                    goToLogin();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                dismissProgress();
                mAuth.signOut();
                Toast.makeText(AuthCallbackActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                goToLogin();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void updateProgress(String message) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(message);
        }
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
