package com.knowledgebase.sopviewer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView userName, userEmail, userRoleBadge, userPhone, userJobTitle, userDept;
    private View editProfileItem, changePasswordItem, manageDownloadsItem;
    private FirebaseAuth mAuth;

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
