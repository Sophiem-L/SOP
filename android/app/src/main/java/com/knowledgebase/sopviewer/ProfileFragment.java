package com.knowledgebase.sopviewer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_LOCAL_AVATAR = "local_avatar_path";

    private TextView userName, userEmail, userRoleBadge, userPhone, userJobTitle, userDept;
    private ShapeableImageView imgAvatar;
    private View editProfileItem, changePasswordItem, manageDownloadsItem;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    File saved = saveImageLocally(uri);
                    if (saved != null) {
                        Glide.with(this).load(saved)
                                .signature(new com.bumptech.glide.signature.ObjectKey(saved.lastModified()))
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile).into(imgAvatar);
                        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putString(KEY_LOCAL_AVATAR, saved.getAbsolutePath()).apply();
                        uploadAvatar(saved);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra("name")) userName.setText(data.getStringExtra("name"));
                    if (data.hasExtra("job_title")) userJobTitle.setText(data.getStringExtra("job_title"));
                    if (data.hasExtra("phone")) userPhone.setText(data.getStringExtra("phone"));
                    loadUserDataFromBackend();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            requireActivity().startActivity(new Intent(requireContext(), WelcomeActivity.class));
            requireActivity().finish();
            return;
        }

        imgAvatar = view.findViewById(R.id.imgAvatar);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        userPhone = view.findViewById(R.id.userPhone);
        userJobTitle = view.findViewById(R.id.userJobTitle);
        userDept = view.findViewById(R.id.userDept);
        userRoleBadge = view.findViewById(R.id.userRoleBadge);
        editProfileItem = view.findViewById(R.id.editProfileItem);
        changePasswordItem = view.findViewById(R.id.changePasswordItem);
        manageDownloadsItem = view.findViewById(R.id.manageDownloadsItem);

        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                SettingsMenuHelper.showSettingsMenu(requireActivity(), v));
        view.findViewById(R.id.btnEditAvatar).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        loadLocalAvatar();
        loadUserData(currentUser);
        loadUserDataFromBackend();
        setupClickListeners();
    }

    // ── Avatar persistence ────────────────────────────────────────────────────

    /** Load the last locally-saved avatar from internal storage. */
    private void loadLocalAvatar() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String path = prefs.getString(KEY_LOCAL_AVATAR, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                Glide.with(this).load(file)
                        .signature(new com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile).into(imgAvatar);
            }
        }
    }

    /** Copy the picked URI into internal storage so the path survives navigation. */
    private File saveImageLocally(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            File file = new File(requireContext().getFilesDir(), "profile_avatar.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[4096];
            int read;
            while ((read = is.read(buf)) != -1) fos.write(buf, 0, read);
            fos.close();
            is.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    private void uploadAvatar(File imageFile) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();

            RequestBody reqBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", imageFile.getName(), reqBody);

            RetrofitClient.getApiService().uploadAvatar(token, part).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String json = response.body().string();
                            org.json.JSONObject obj = new org.json.JSONObject(json);
                            String photoUrl = obj.optString("profile_photo_url", "");
                            if (!photoUrl.isEmpty()) {
                                photoUrl = photoUrl
                                        .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                                        .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/");
                                final String finalUrl = photoUrl;
                                requireActivity().runOnUiThread(() ->
                                        Glide.with(ProfileFragment.this)
                                                .load(finalUrl)
                                                .circleCrop()
                                                .placeholder(R.drawable.ic_profile)
                                                .into(imgAvatar));
                            }
                        } catch (Exception ignored) {}
                        Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Upload failed: HTTP " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Upload error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadUserData(FirebaseUser user) {
        userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Group 8");
        userEmail.setText(user.getEmail());
    }

    private void loadUserDataFromBackend() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();
            RetrofitClient.getApiService().getProfile(token).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                    updateUIWithBackendData(response.body());
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {}
            });
        });
    }

    private void updateUIWithBackendData(User user) {
        String name = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : (user.getName() != null ? user.getName() : "");
        userName.setText(name);
        userJobTitle.setText(user.getJobTitle() != null ? user.getJobTitle() : "");
        userPhone.setText(user.getPhone() != null ? user.getPhone() : "");
        if (user.getDepartment() != null && user.getDepartment().getName() != null)
            userDept.setText(user.getDepartment().getName());
        else
            userDept.setText("");
        if (userRoleBadge != null) {
            if (user.getRoles() != null && !user.getRoles().isEmpty()
                    && user.getRoles().get(0).getName() != null)
                userRoleBadge.setText(user.getRoles().get(0).getName());
            else
                userRoleBadge.setText("");
        }

        // Server avatar takes priority over local copy only if upload succeeded previously
        String photoUrl = user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl()
                : user.getAvatarUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            photoUrl = photoUrl
                    .replace("http://localhost:8000/", "http://10.0.2.2:8000/")
                    .replace("http://127.0.0.1:8000/", "http://10.0.2.2:8000/");
            Glide.with(this).load(photoUrl).circleCrop()
                    .placeholder(R.drawable.ic_profile).into(imgAvatar);
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        editProfileItem.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            intent.putExtra("name", userName.getText().toString());
            intent.putExtra("job_title", userJobTitle.getText().toString());
            intent.putExtra("phone", userPhone.getText().toString());
            editProfileLauncher.launch(intent);
        });

        changePasswordItem.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        manageDownloadsItem.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ManageDownloadsActivity.class)));

        requireView().findViewById(R.id.logoutItem).setOnClickListener(v -> {
            mAuth.signOut();
            SseManager.getInstance().stop();
            requireActivity().startActivity(new Intent(requireContext(), MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            requireActivity().finish();
        });
    }
}
