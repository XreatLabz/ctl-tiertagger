package com.ctltierlist.tiertagger.client.mixin;

import com.ctltierlist.tiertagger.CTLTierTagger;
import com.ctltierlist.tiertagger.client.render.TierHudRenderer;
import com.ctltierlist.tiertagger.version.compat.CompatBridgeFactory;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("TAIL"),
        require = 0
    )
    private void onRenderLabel(@Coerce Object renderLabelContext, Text text, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        try {
            if (renderLabelContext instanceof AbstractClientPlayerEntity player) {
                TierHudRenderer.renderTierAboveNametag(player, matrices, vertexConsumers, light);
                return;
            }

            String playerName = CompatBridgeFactory.client().resolvePlayerName(renderLabelContext);
            if (playerName != null) {
                TierHudRenderer.renderTierAboveNametag(playerName, matrices, vertexConsumers, light);
            }
        } catch (Exception e) {
            CTLTierTagger.LOGGER.error("Error rendering tier label: {}", e.getMessage(), e);
        }
    }
}
