package com.knowledgebase.sopviewer;

import android.content.Intent;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView userName, userEmail, userRoleBadge, userPhone, userJobTitle, userDept;
    private ShapeableImageView imgAvatar;
    private View editProfileItem, changePasswordItem, manageDownloadsItem;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .placeholder(R.drawable.ic_profile)
                            .into(imgAvatar);
                    uploadAvatar(uri);
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

        // Bind views
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

        loadUserData(currentUser);
        loadUserDataFromBackend();
        setupClickListeners();
    }

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
            if (user.getRoles() != null && !user.getRoles().isEmpty() && user.getRoles().get(0).getName() != null)
                userRoleBadge.setText(user.getRoles().get(0).getName());
            else
                userRoleBadge.setText("");
        }
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile)
                    .into(imgAvatar);
        }
    }

    private void uploadAvatar(Uri uri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                File tempFile = File.createTempFile("avatar", ".jpg", requireContext().getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempFile);
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) != -1) fos.write(buf, 0, read);
                fos.close();
                is.close();

                RequestBody reqBody = RequestBody.create(tempFile, MediaType.parse("image/*"));
                MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", tempFile.getName(), reqBody);

                RetrofitClient.getApiService().uploadAvatar(token, part).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
