package com.knowledgebase.sopviewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory cache for API responses
 * Caches data for 2 minutes to reduce API calls during tab switching
 */
public class DataCache {
    private static DataCache instance;
    private static final long CACHE_DURATION = 2 * 60 * 1000; // 2 minutes

    private Map<String, CacheEntry<?>> cache = new HashMap<>();

    private DataCache() {
    }

    public static DataCache getInstance() {
        if (instance == null) {
            instance = new DataCache();
        }
        return instance;
    }

    public <T> void put(String key, T data) {
        cache.put(key, new CacheEntry<>(data, System.currentTimeMillis()));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // Check if cache is expired
        if (System.currentTimeMillis() - entry.timestamp > CACHE_DURATION) {
            cache.remove(key);
            return null;
        }

        return (T) entry.data;
    }

    public void clear(String key) {
        cache.remove(key);
    }

    public void clearAll() {
        cache.clear();
    }

    public boolean has(String key) {
        Object data = get(key);
        return data != null;
    }

    private static class CacheEntry<T> {
        T data;
        long timestamp;

        CacheEntry(T data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }
    }

    // Cache keys constants
    public static final String KEY_MAIN_RECENT_DOCS = "main_recent_docs";
    public static final String KEY_MAIN_CATEGORIES = "main_categories";
    public static final String KEY_BOOKMARKS = "bookmarks";
    public static final String KEY_SEARCH_RESULTS = "search_results_";
    public static final String KEY_USER_TOKEN = "user_token";
}
