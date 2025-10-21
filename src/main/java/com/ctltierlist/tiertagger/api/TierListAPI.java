package com.ctltierlist.tiertagger.api;

import com.ctltierlist.tiertagger.CTLTierTagger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TierListAPI {
    private static final String API_BASE_URL = "https://ctltierlist-api-b2s8.vercel.app";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    /**
     * Fetch player profile data from the API
     * @param playerName The player's in-game name
     * @return CompletableFuture with PlayerTierData
     */
    public static CompletableFuture<PlayerTierData> fetchPlayerTier(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = API_BASE_URL + "/api/search_profile/" + playerName;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    return parsePlayerData(json, playerName);
                } else {
                    CTLTierTagger.LOGGER.warn("Failed to fetch tier for {}: HTTP {}", playerName, response.statusCode());
                    return null;
                }
            } catch (Exception e) {
                CTLTierTagger.LOGGER.error("Error fetching tier for {}: {}", playerName, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Parse player data from JSON response
     */
    private static PlayerTierData parsePlayerData(JsonObject json, String playerName) {
        try {
            JsonObject profile = json.has("profile") ? json.getAsJsonObject("profile") : null;
            JsonObject ranks = json.has("ranks") ? json.getAsJsonObject("ranks") : null;

            String region = profile != null && profile.has("region") ? profile.get("region").getAsString() : "Unknown";
            
            PlayerTierData tierData = new PlayerTierData(playerName, region);

            // Store tier for each gamemode
            if (ranks != null) {
                for (String gamemode : ranks.keySet()) {
                    JsonObject rankData = ranks.getAsJsonObject(gamemode);
                    if (rankData.has("rank")) {
                        String rank = rankData.get("rank").getAsString();
                        tierData.setTierForGamemode(normalizeGamemode(gamemode), rank);
                    }
                }
            }

            return tierData;
        } catch (Exception e) {
            CTLTierTagger.LOGGER.error("Error parsing player data: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Normalize gamemode names to match config names
     */
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
            default -> gamemode;
        };
    }

    /**
     * Convert tier string to numeric value for comparison (lower is better)
     */
    private static int getTierValue(String tier) {
        if (tier == null || tier.equals("Unranked")) return 999;
        
        // Extract tier number and type (HT/LT)
        if (tier.startsWith("HT")) {
            int num = Integer.parseInt(tier.substring(2));
            return (num * 2) - 1; // HT1=1, HT2=3, HT3=5, etc.
        } else if (tier.startsWith("LT")) {
            int num = Integer.parseInt(tier.substring(2));
            return num * 2; // LT1=2, LT2=4, LT3=6, etc.
        }
        return 999;
    }

    /**
     * Data class to hold player tier information
     */
    public static class PlayerTierData {
        public final String playerName;
        public final String region;
        public final long fetchTime;
        private final Map<String, String> gamemodeTiers;

        public PlayerTierData(String playerName, String region) {
            this.playerName = playerName;
            this.region = region;
            this.fetchTime = System.currentTimeMillis();
            this.gamemodeTiers = new ConcurrentHashMap<>();
        }

        public void setTierForGamemode(String gamemode, String tier) {
            gamemodeTiers.put(gamemode, tier);
        }

        public String getTierForGamemode(String gamemode) {
            return gamemodeTiers.getOrDefault(gamemode, "Unranked");
        }

        public boolean hasTierForGamemode(String gamemode) {
            String tier = gamemodeTiers.get(gamemode);
            return tier != null && !tier.equals("Unranked");
        }

        public String getHighestTier() {
            String highestTier = "Unranked";
            int highestTierValue = 999;
            
            for (Map.Entry<String, String> entry : gamemodeTiers.entrySet()) {
                String tier = entry.getValue();
                int tierValue = TierListAPI.getTierValue(tier);
                if (tierValue < highestTierValue) {
                    highestTierValue = tierValue;
                    highestTier = tier;
                }
            }
            
            return highestTier;
        }

        public String getHighestTierGamemode() {
            String highestGamemode = null;
            int highestTierValue = 999;
            
            for (Map.Entry<String, String> entry : gamemodeTiers.entrySet()) {
                String tier = entry.getValue();
                int tierValue = TierListAPI.getTierValue(tier);
                if (tierValue < highestTierValue) {
                    highestTierValue = tierValue;
                    highestGamemode = entry.getKey();
                }
            }
            
            return highestGamemode;
        }

        public boolean isExpired(long cacheTimeMs) {
            return System.currentTimeMillis() - fetchTime > cacheTimeMs;
        }

        @Override
        public String toString() {
            return String.format("Player: %s, Region: %s, Tiers: %s", playerName, region, gamemodeTiers);
        }
    }
}
