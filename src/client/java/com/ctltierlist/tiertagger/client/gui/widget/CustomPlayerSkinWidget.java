package com.ctltierlist.tiertagger.client.gui.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * Custom player skin widget that works on both 1.20 and 1.21
 * Renders a 2D player skin texture for compatibility across versions
 */
@Getter
public class CustomPlayerSkinWidget implements Drawable, Element, Selectable {
    @Setter private int x;
    @Setter private int y;
    private final int width;
    private final int height;
    private final Supplier<Identifier> skinTextureSupplier;
    @Setter private boolean focused;
    
    private static final int SKIN_TEX_WIDTH = 64;
    private static final int SKIN_TEX_HEIGHT = 64;
    private static final int HEAD_U = 8;
    private static final int HEAD_V = 8;
    private static final int HEAD_SIZE = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int BODY_U = 20;
    private static final int BODY_V = 20;
    private static final int BODY_WIDTH = 8;
    private static final int BODY_HEIGHT = 12;
    
    public CustomPlayerSkinWidget(int x, int y, int width, int height, Supplier<Identifier> skinTextureSupplier) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.skinTextureSupplier = skinTextureSupplier;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Identifier skinTexture = skinTextureSupplier.get();
        if (skinTexture == null) return;
        
        int headDisplaySize = Math.min(width, height / 2);
        int headX = x + (width - headDisplaySize) / 2;
        int headY = y;
        
        // Head + hat overlay
        drawTexturePart(context, skinTexture, headX, headY, headDisplaySize, headDisplaySize, HEAD_U, HEAD_V, HEAD_SIZE, HEAD_SIZE);
        drawTexturePart(context, skinTexture, headX, headY, headDisplaySize, headDisplaySize, HAT_U, HAT_V, HEAD_SIZE, HEAD_SIZE);
        
        // Body
        int bodyWidth = (int)(headDisplaySize * 0.5);
        int bodyHeight = (int)(headDisplaySize * 0.75);
        int bodyX = x + (width - bodyWidth) / 2;
        int bodyY = headY + headDisplaySize;
        drawTexturePart(context, skinTexture, bodyX, bodyY, bodyWidth, bodyHeight, BODY_U, BODY_V, BODY_WIDTH, BODY_HEIGHT);
        
        // Arms
        int armWidth = (int)(headDisplaySize * 0.25);
        drawTexturePart(context, skinTexture, bodyX + bodyWidth, bodyY, armWidth, bodyHeight, 44, 20, 4, 12);
        drawTexturePart(context, skinTexture, bodyX - armWidth, bodyY, armWidth, bodyHeight, 36, 52, 4, 12);
        
        // Legs
        int legWidth = bodyWidth / 2;
        int legHeight = (int)(headDisplaySize * 0.75);
        int legY = bodyY + bodyHeight;
        drawTexturePart(context, skinTexture, bodyX, legY, legWidth, legHeight, 4, 20, 4, 12);
        drawTexturePart(context, skinTexture, bodyX + legWidth, legY, legWidth, legHeight, 20, 52, 4, 12);
    }
    
    private void drawTexturePart(DrawContext context, Identifier texture, int x, int y, int width, int height, int u, int v, int regionWidth, int regionHeight) {
        context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, SKIN_TEX_WIDTH, SKIN_TEX_HEIGHT);
    }
    
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    @Override
    public SelectionType getType() {
        return SelectionType.NONE;
    }
    
    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {}
}
