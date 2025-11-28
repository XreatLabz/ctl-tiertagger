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
     * Get tier data for a player. Checks OverallCache first, then local cache,
     * then fetches from API asynchronously as fallback.
     */
    public static TierListAPI.PlayerTierData getTierData(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }

        // Check OverallCache first (bulk cache from /rankings/overall)
        TierListAPI.PlayerTierData overallCached = OverallCache.getPlayer(playerName);
        if (overallCached != null) {
            return overallCached;
        }

        // Check local cache
        TierListAPI.PlayerTierData cached = cache.get(playerName);
        if (cached != null && !cached.isExpired(ModConfig.getCacheTime())) {
            return cached;
        }

        // Fallback: fetch from individual API (for players not in overall rankings)
        if (!pendingRequests.getOrDefault(playerName, false)) {
            pendingRequests.put(playerName, true);
            
            TierListAPI.fetchPlayerTier(playerName).thenAccept(data -> {
                if (data != null) {
                    CTLTierTagger.LOGGER.debug("Fetched tier data for {} via API fallback", playerName);
                    cache.put(playerName, data);
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
