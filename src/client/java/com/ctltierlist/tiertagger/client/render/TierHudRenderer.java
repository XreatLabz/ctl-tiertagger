package com.ctltierlist.tiertagger.client.render;

import com.ctltierlist.tiertagger.api.TierListAPI;
import com.ctltierlist.tiertagger.cache.TierCache;
import com.ctltierlist.tiertagger.config.ModConfig;
import com.ctltierlist.tiertagger.version.compat.CompatBridgeFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class TierHudRenderer {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * Render tier label above player's nametag
     * Called from renderLabelIfPresent, so positioning is already done
     */
    public static void renderTierAboveNametag(PlayerEntity player, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        renderTierAboveNametag(player, player != null ? player.getName().getString() : null, matrices, vertexConsumers, light);
    }

    public static void renderTierAboveNametag(String playerName, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        renderTierAboveNametag(null, playerName, matrices, vertexConsumers, light);
    }

    private static void renderTierAboveNametag(PlayerEntity player, String playerName, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }

        if (!ModConfig.isEnabled()) {
            return;
        }

        // Don't render for local player when possible
        if (player != null && player == client.player) {
            return;
        }
        if (player == null && client.player != null && playerName.equals(client.player.getName().getString())) {
            return;
        }

        // Fetch tier data if not cached
        TierListAPI.PlayerTierData tierData = TierCache.getTierData(playerName);

        if (tierData == null) {
            return;
        }

        // Determine which tier to show based on config
        String displayTier;
        String displayGamemode;
        boolean isRetired;

        if (ModConfig.shouldShowHighestTier()) {
            // Show highest tier across all gamemodes
            displayTier = tierData.getHighestTier();
            displayGamemode = tierData.getHighestTierGamemode();

            if (displayTier.equals("Unranked") || displayGamemode == null) {
                return;
            }

            isRetired = tierData.isRetired(displayGamemode);
        } else {
            // Filter by selected gamemode
            String selectedGamemode = ModConfig.getSelectedGamemode();

            if (!tierData.hasTierForGamemode(selectedGamemode)) {
                return;
            }

            displayTier = tierData.getTierForGamemode(selectedGamemode);
            displayGamemode = selectedGamemode;
            isRetired = tierData.isRetired(selectedGamemode);
        }

        // Add R prefix for retired tiers (e.g., "RHT3")
        if (isRetired) {
            displayTier = "R" + displayTier;
        }

        // Build tier text in TierTagger format: [ICON] TIER
        Text text = Text.empty();

        // Add gamemode icon if enabled
        if (ModConfig.shouldShowGamemode()) {
            String icon = getGamemodeIcon(displayGamemode);
            if (!icon.isEmpty()) {
                text = Text.literal(icon + " ");
            }
        }

        // Add tier with color from config
        int tierColor = ModConfig.getTierColor(displayTier);
        text = text.copy().append(Text.literal(displayTier).styled(s -> s.withColor(tierColor)));

        TextRenderer textRenderer = client.textRenderer;

        if (textRenderer == null) {
            return;
        }

        matrices.push();
        try {
            // Move up above the nametag (nametag is at y=0 in this context)
            matrices.translate(0.0, -10.0, 0.0);

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();

            // Center the text
            float x = -textRenderer.getWidth(text) / 2.0f;

            // Render with background
            int backgroundColor = (int)(0.25F * 255.0F) << 24;

            CompatBridgeFactory.client().drawSeeThroughText(
                textRenderer,
                text,
                x,
                matrix4f,
                vertexConsumers,
                backgroundColor,
                light
            );
        } finally {
            matrices.pop();
        }
    }


    /**
     * Get gamemode icon (using custom font characters)
     * These map to textures defined in assets/minecraft/font/default.json
     */
    private static String getGamemodeIcon(String gamemode) {
        return switch (gamemode.toLowerCase()) {
            case "sword" -> "\uE801"; // sword.png
            case "cpvp", "crystal" -> "\uE800"; // vanilla.png (crystal)
            case "netherite", "nethpot" -> "\uE803"; // neth_op.png
            case "pot", "potion" -> "\uE802"; // pot.png
            case "mace", "macepvp" -> "\uE807"; // mace.png
            case "uhc" -> "\uE804"; // uhc.png
            case "axe", "axepvp" -> "\uE805"; // axe.png
            case "smp", "smpkit" -> "\uE806"; // smp.png
            default -> "";
        };
    }


}
