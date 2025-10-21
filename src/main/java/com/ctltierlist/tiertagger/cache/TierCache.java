package com.ctltierlist.tiertagger.cache;

import com.ctltierlist.tiertagger.CTLTierTagger;
import com.ctltierlist.tiertagger.api.TierListAPI;
import com.ctltierlist.tiertagger.config.ModConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TierCache {
    private static final Map<String, TierListAPI.PlayerTierData> cache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> pendingRequests = new ConcurrentHashMap<>();

    /**
     * Get tier data for a player. Returns cached data if available and not expired.
     * Otherwise, fetches from API asynchronously.
     */
    public static TierListAPI.PlayerTierData getTierData(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        // Check cache first
        TierListAPI.PlayerTierData cached = cache.get(playerName);
        if (cached != null && !cached.isExpired(ModConfig.getCacheTime())) {
            return cached;
        }

        // If not pending, fetch from API
        if (!pendingRequests.getOrDefault(playerName, false)) {
            pendingRequests.put(playerName, true);
            
            TierListAPI.fetchPlayerTier(playerName).thenAccept(data -> {
                if (data != null) {
                    CTLTierTagger.LOGGER.info("Cached tier data for {}: {}", playerName, data.toString());
                    cache.put(playerName, data);
                } else {
                    CTLTierTagger.LOGGER.warn("Failed to fetch tier for {}", playerName);
                }
                pendingRequests.remove(playerName);
            });
        }

        // Return cached data even if expired while we fetch new data
        return cached;
    }

    /**
     * Clear all cached data
     */
    public static void clearCache() {
        cache.clear();
        pendingRequests.clear();
    }

    /**
     * Remove a specific player from cache
     */
    public static void removeCached(String playerName) {
        cache.remove(playerName);
    }

    /**
     * Get cache size
     */
    public static int getCacheSize() {
        return cache.size();
    }
}
