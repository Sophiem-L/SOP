package com.knowledgebase.sopviewer;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.HashMap;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button btnChangePassword;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "You must be signed in to change password.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentPasswordInput = findViewById(R.id.currentPassword);
        newPasswordInput = findViewById(R.id.newPassword);
        confirmPasswordInput = findViewById(R.id.confirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String current = currentPasswordInput.getText().toString().trim();
        String newPass = newPasswordInput.getText().toString().trim();
        String confirm = confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(current)) {
            currentPasswordInput.setError("Enter your current password");
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            newPasswordInput.setError("Enter a new password");
            return;
        }
        if (newPass.length() < 6) {
            newPasswordInput.setError("Password must be at least 6 characters");
            return;
        }
        if (!newPass.equals(confirm)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating password...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            dismissProgress();
            Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(credential)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        dismissProgress();
                        String msg = task.getException() != null ? task.getException().getMessage() : "Re-authentication failed";
                        if (msg.contains("wrong password") || msg.contains("invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS")) {
                            currentPasswordInput.setError("Incorrect current password");
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    // Re-auth succeeded; update password in Firebase first
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            syncPasswordToBackend(newPass);
                        } else {
                            dismissProgress();
                            String msg = updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed";
                            if (msg.contains("requires recent login")) {
                                Toast.makeText(ChangePasswordActivity.this, "Please sign out and sign in again, then try changing password.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ChangePasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                });
    }

    private void syncPasswordToBackend(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            dismissProgress();
            Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                dismissProgress();
                Toast.makeText(ChangePasswordActivity.this, "Password updated. Server sync skipped.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            String token = "Bearer " + task.getResult().getToken();
            Map<String, String> body = new HashMap<>();
            body.put("new_password", newPassword);
            RetrofitClient.getApiService().updatePassword(token, body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    dismissProgress();
                    if (response.isSuccessful()) {
                        Toast.makeText(ChangePasswordActivity.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "Password updated. Server sync failed.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    dismissProgress();
                    Toast.makeText(ChangePasswordActivity.this, "Password updated. Server sync failed.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
