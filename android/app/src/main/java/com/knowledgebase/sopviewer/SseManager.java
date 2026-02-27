package com.knowledgebase.sopviewer;

import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

/**
 * Polls the SSE /api/notifications/stream endpoint on a background thread.
 * The server sends one event (unread_count) then closes. We reconnect every 30
 * s.
 *
 * Usage:
 * SseManager.getInstance().start(firebaseUser);
 * SseManager.getInstance().addListener(count -> updateBadge(count));
 * SseManager.getInstance().stop(); // call in onDestroy of the owning Activity
 */
public class SseManager {

    public interface UnreadCountListener {
        void onUnreadCountChanged(int count);
    }

    private static volatile SseManager instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<UnreadCountListener> listeners = new ArrayList<>();
    private final OkHttpClient httpClient;

    private volatile int unreadCount = 0;
    private volatile boolean running = false;
    private volatile FirebaseUser firebaseUser;

    private static final long POLL_INTERVAL_MS = 30_000; // 30 seconds

    private SseManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public static SseManager getInstance() {
        if (instance == null) {
            synchronized (SseManager.class) {
                if (instance == null)
                    instance = new SseManager();
            }
        }
        return instance;
    }

    /** Start (or restart) polling with a fresh Firebase user reference. */
    public void start(FirebaseUser user) {
        this.firebaseUser = user;
        if (!running) {
            running = true;
            scheduleNextPoll(0);
        }
    }

    /**
     * Immediately clear the unread count (called when the user opens
     * Notifications).
     */
    public void clearUnreadCount() {
        unreadCount = 0;
        notifyListeners(0);
    }

    /** Stop polling (e.g. when user logs out). */
    public void stop() {
        running = false;
        firebaseUser = null;
        mainHandler.removeCallbacksAndMessages(null);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void addListener(UnreadCountListener l) {
        if (!listeners.contains(l))
            listeners.add(l);
    }

    public void removeListener(UnreadCountListener l) {
        listeners.remove(l);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void scheduleNextPoll(long delayMs) {
        mainHandler.postDelayed(this::fetchWithToken, delayMs);
    }

    private void fetchWithToken() {
        if (!running || firebaseUser == null)
            return;
        firebaseUser.getIdToken(false).addOnSuccessListener(result -> {
            String token = "Bearer " + result.getToken();
            new Thread(() -> doFetch(token)).start();
        }).addOnFailureListener(e -> {
            // Retry after interval even if token fetch fails
            if (running)
                scheduleNextPoll(POLL_INTERVAL_MS);
        });
    }

    private void doFetch(String token) {
        try {
            Request request = new Request.Builder()
                    .url(RetrofitClient.BASE_URL + "api/notifications/stream")
                    .header("Authorization", token)
                    .header("Accept", "text/event-stream")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String json = line.substring(5).trim();
                            try {
                                JSONObject obj = new JSONObject(json);
                                int count = obj.optInt("unread_count", 0);
                                unreadCount = count;
                                notifyListeners(count);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.w("SseManager", "SSE fetch failed: " + e.getMessage());
        } finally {
            if (running)
                scheduleNextPoll(POLL_INTERVAL_MS);
        }
    }

    private void notifyListeners(int count) {
        mainHandler.post(() -> {
            for (UnreadCountListener l : new ArrayList<>(listeners)) {
                l.onUnreadCountChanged(count);
            }
        });
    }
}
