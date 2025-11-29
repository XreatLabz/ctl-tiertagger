package com.ctltierlist.tiertagger.cache;

import com.ctltierlist.tiertagger.CTLTierTagger;
import com.ctltierlist.tiertagger.api.TierListAPI;
import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

public class OverallCache {
    private static final String API_URL = "https://private-ctltierlist-api.vercel.app/rankings/overall";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000; // 1 hour
    private static final Map<String, TierListAPI.PlayerTierData> playerCache = new ConcurrentHashMap<>();
    private static Path cacheFilePath;
    private static ScheduledExecutorService scheduler;
    private static volatile boolean initialized = false;
    private static volatile long lastRefreshTime = 0;

    public static void init(Path configDir) {
        cacheFilePath = configDir.resolve("ctl-tiertagger").resolve("cache.json");
        
        // Load from disk cache first (instant startup)
        loadFromDisk();
        
        // Schedule hourly refresh with dedicated executor
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CTL-TierTagger-CacheRefresh");
            t.setDaemon(true);
            return t;
        });
        
        // Refresh from API immediately on startup (runs on our executor, not ForkJoinPool)
        scheduler.execute(OverallCache::refreshFromAPI);
        
        // Schedule hourly refresh after initial delay
        scheduler.scheduleAtFixedRate(
            OverallCache::refreshFromAPI,
            REFRESH_INTERVAL_MS,
            REFRESH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        initialized = true;
        CTLTierTagger.LOGGER.info("OverallCache initialized with {} players from disk cache, API refresh started", playerCache.size());
    }

    public static TierListAPI.PlayerTierData getPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return null;
        }
        return playerCache.get(playerName.toLowerCase());
    }

    public static boolean hasPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return playerCache.containsKey(playerName.toLowerCase());
    }

    public static int getCacheSize() {
        return playerCache.size();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static long getLastRefreshTime() {
        return lastRefreshTime;
    }

    public static void refreshFromAPI() {
        try {
            CTLTierTagger.LOGGER.info("Refreshing overall cache from API...");
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String json = response.body();
                parseAndCacheLeaderboard(json);
                saveToDisk(json);
                lastRefreshTime = System.currentTimeMillis();
                CTLTierTagger.LOGGER.info("Overall cache refreshed: {} players loaded", playerCache.size());
            } else {
                CTLTierTagger.LOGGER.warn("Failed to fetch overall rankings: HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            CTLTierTagger.LOGGER.error("Error refreshing overall cache: {}", e.getMessage());
        }
    }

    private static void parseAndCacheLeaderboard(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray leaderboard = root.getAsJsonArray("leaderboard");
            
            Map<String, TierListAPI.PlayerTierData> newCache = new ConcurrentHashMap<>();
            
            for (JsonElement element : leaderboard) {
                JsonObject player = element.getAsJsonObject();
                
                String ingameName = player.get("ingameName").getAsString();
                String region = player.has("region") ? player.get("region").getAsString() : "Unknown";
                int totalPoints = player.has("totalPoints") ? player.get("totalPoints").getAsInt() : 0;
                String title = player.has("title") ? player.get("title").getAsString() : "Rookie";
                int rank = player.has("rank") ? player.get("rank").getAsInt() : 0;
                
                TierListAPI.PlayerTierData tierData = new TierListAPI.PlayerTierData(
                    ingameName, region, "", "", totalPoints, title, rank
                );
                
                // Parse ranks for each gamemode
                if (player.has("ranks")) {
                    JsonObject ranks = player.getAsJsonObject("ranks");
                    for (String gamemode : ranks.keySet()) {
                        JsonObject rankData = ranks.getAsJsonObject(gamemode);
                        if (rankData.has("rank")) {
                            String tierRank = rankData.get("rank").getAsString();
                            boolean retired = rankData.has("retired") && rankData.get("retired").getAsBoolean();
                            tierData.setTierForGamemode(normalizeGamemode(gamemode), tierRank, retired);
                        }
                    }
                }
                
                newCache.put(ingameName.toLowerCase(), tierData);
            }
            
            // Atomic swap
            playerCache.clear();
            playerCache.putAll(newCache);
            
        } catch (Exception e) {
            CTLTierTagger.LOGGER.error("Error parsing leaderboard JSON: {}", e.getMessage());
        }
    }

    private static String normalizeGamemode(String gamemode) {
        return switch (gamemode.toLowerCase()) {
            case "sword", "swd" -> "Sword";
            case "crystal", "cpvp" -> "Crystal";
            case "netherite", "nethpot" -> "Netherite";
            case "pot", "potion" -> "Potion";
            case "mace", "macepvp" -> "Mace";
            case "uhc" -> "UHC";
            case "axe", "axepvp" -> "Axe";
            case "smp", "smpkit" -> "SMP";
            case "diasmp" -> "DiaSMP";
            default -> gamemode;
        };
    }

    private static void saveToDisk(String json) {
        try {
            Files.createDirectories(cacheFilePath.getParent());
            Files.writeString(cacheFilePath, json);
            CTLTierTagger.LOGGER.debug("Cache saved to disk: {}", cacheFilePath);
        } catch (Exception e) {
            CTLTierTagger.LOGGER.error("Failed to save cache to disk: {}", e.getMessage());
        }
    }

    private static void loadFromDisk() {
        try {
            if (Files.exists(cacheFilePath)) {
                String json = Files.readString(cacheFilePath);
                parseAndCacheLeaderboard(json);
                CTLTierTagger.LOGGER.info("Loaded {} players from disk cache", playerCache.size());
            }
        } catch (Exception e) {
            CTLTierTagger.LOGGER.warn("Failed to load cache from disk: {}", e.getMessage());
        }
    }

    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public static void forceRefresh() {
        CompletableFuture.runAsync(OverallCache::refreshFromAPI);
    }
}
