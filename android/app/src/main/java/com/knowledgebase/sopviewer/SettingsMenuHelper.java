package com.knowledgebase.sopviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsMenuHelper {

    public static void showSettingsMenu(Activity activity, View anchor) {
        View popupView = LayoutInflater.from(activity).inflate(R.layout.layout_settings_popup, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        // Set elevation and background
        popupWindow.setElevation(10);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Click listeners
        popupView.findViewById(R.id.menuProfile).setOnClickListener(v -> {
            activity.startActivity(new Intent(activity, ProfileActivity.class));
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuOffline).setOnClickListener(v -> {
            Toast.makeText(activity, "Offline access coming soon", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuPolicy).setOnClickListener(v -> {
            Toast.makeText(activity, "Policy & Terms coming soon", Toast.LENGTH_SHORT).show();
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            popupWindow.dismiss();
            performLogout(activity);
        });

        // Show popup
        popupWindow.showAsDropDown(anchor, 0, 10);
    }

    private static void performLogout(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finishSignOut(activity);
            return;
        }

        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                finishSignOut(activity);
                return;
            }

            String idToken = "Bearer " + task.getResult().getToken();
            RetrofitClient.getApiService().logout(idToken).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    finishSignOut(activity);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // Sign out locally even if backend call fails
                    finishSignOut(activity);
                }
            });
        });
    }

    private static void finishSignOut(Activity activity) {
        DataCache.getInstance().clearAll();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(activity, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
