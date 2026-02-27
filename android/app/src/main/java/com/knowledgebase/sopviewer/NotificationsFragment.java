package com.knowledgebase.sopviewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerNotifications, recyclerAuditLogs;
    private NotificationAdapter notificationAdapter;
    private AuditLogAdapter auditLogAdapter;
    private TextView tabNotifications, tabAuditLogs;
    private View layoutNotifications, layoutAuditLogs;
    private FirebaseAuth mAuth;
    private final List<Notification> notificationList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        recyclerNotifications = view.findViewById(R.id.recyclerNotifications);
        recyclerAuditLogs = view.findViewById(R.id.recyclerAuditLogs);
        layoutNotifications = view.findViewById(R.id.layoutNotifications);
        layoutAuditLogs = view.findViewById(R.id.layoutAuditLogs);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAuditLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        notificationAdapter = new NotificationAdapter(notificationList);
        recyclerNotifications.setAdapter(notificationAdapter);

        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                SettingsMenuHelper.showSettingsMenu(requireActivity(), v));

        tabNotifications = view.findViewById(R.id.tabNotifications);
        tabAuditLogs = view.findViewById(R.id.tabAuditLogs);
        tabNotifications.setOnClickListener(v -> setTabActive(true));
        tabAuditLogs.setOnClickListener(v -> setTabActive(false));

        setTabActive(true);
        loadMockAuditLogs();
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Badge is cleared by MainActivity's SSE listener when count resets after markAllRead
        SseManager.getInstance().clearUnreadCount();
    }

    private void loadNotifications() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;
            String token = "Bearer " + task.getResult().getToken();
            RetrofitClient.getApiService().getNotifications(token)
                    .enqueue(new Callback<List<Notification>>() {
                        @Override
                        public void onResponse(Call<List<Notification>> call,
                                               Response<List<Notification>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                notificationList.clear();
                                notificationList.addAll(response.body());
                                notificationAdapter.notifyDataSetChanged();
                                markAllRead(token);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Notification>> call, Throwable t) {
                            if (isAdded())
                                Toast.makeText(requireContext(), "Failed to load notifications",
                                        Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void markAllRead(String token) {
        RetrofitClient.getApiService().markAllNotificationsRead(token)
                .enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> r) {}
                    @Override public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
                });
    }

    private void setTabActive(boolean isNotifications) {
        if (isNotifications) {
            tabNotifications.setBackgroundResource(R.drawable.bg_active_tab);
            tabNotifications.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabNotifications.setTypeface(null, android.graphics.Typeface.BOLD);
            tabAuditLogs.setBackground(null);
            tabAuditLogs.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_text));
            tabAuditLogs.setTypeface(null, android.graphics.Typeface.NORMAL);
            layoutNotifications.setVisibility(View.VISIBLE);
            layoutAuditLogs.setVisibility(View.GONE);
        } else {
            tabAuditLogs.setBackgroundResource(R.drawable.bg_active_tab);
            tabAuditLogs.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabAuditLogs.setTypeface(null, android.graphics.Typeface.BOLD);
            tabNotifications.setBackground(null);
            tabNotifications.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_text));
            tabNotifications.setTypeface(null, android.graphics.Typeface.NORMAL);
            layoutNotifications.setVisibility(View.GONE);
            layoutAuditLogs.setVisibility(View.VISIBLE);
        }
    }

    private void loadMockAuditLogs() {
        List<AuditLog> logs = new ArrayList<>();
        logs.add(new AuditLog("Alice Johnson", "Project Manager", "2023-10-26 14:30",
                "Weekly Standup Summary", "Project Alpha Brief.docx", R.drawable.ic_profile));
        logs.add(new AuditLog("Bob Smith", "Software Engineer", "2023-10-25 10:00",
                "Debugging Session Notes", "Bug Report #123.pdf", R.drawable.ic_profile));
        logs.add(new AuditLog("Charlie Davis", "Compliance Officer", "2023-10-24 16:15",
                "Policy Update Review", "Security_SOP_v2.pdf", R.drawable.ic_profile));
        auditLogAdapter = new AuditLogAdapter(logs);
        recyclerAuditLogs.setAdapter(auditLogAdapter);
    }
}
