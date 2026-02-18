package com.ctltierlist.tiertagger.version.compat;

import com.ctltierlist.tiertagger.version.MinecraftVersion;
import com.ctltierlist.tiertagger.version.VersionSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ClientCompatBridge121 implements ClientCompatBridge {
    // 1.21.11+ uses new KeyBinding.Category API
    private static final MinecraftVersion KEYBINDING_CHANGE_VERSION = MinecraftVersion.parse("1.21.11");
    private static final boolean USE_NEW_KEYBINDING_API = VersionSupport.current().compareTo(KEYBINDING_CHANGE_VERSION) >= 0;

    @Override
    public PlayerSkinWidget createPlayerSkinWidget(MinecraftClient client, Identifier textureId, String skinUrl, int width, int height) {
        Object skinSupplier = createSkinSupplier(textureId, skinUrl);
        Object modelLoader = resolveEntityModelLoader(client);
        if (modelLoader == null) {
            throw new RuntimeException("Could not resolve entity model loader from MinecraftClient");
        }

        return createPlayerSkinWidgetReflect(width, height, modelLoader, skinSupplier);
    }

    @Override
    public void drawSeeThroughText(TextRenderer textRenderer, Text text, float x, Matrix4f matrix4f,
                                   VertexConsumerProvider vertexConsumers, int backgroundColor, int light) {
        textRenderer.draw(
            text,
            x,
            0,
            0xFFFFFF,
            false,
            matrix4f,
            vertexConsumers,
            TextRenderer.TextLayerType.SEE_THROUGH,
            backgroundColor,
            light
        );
    }

    @Override
    public KeyBinding createKeyBinding(String translationKey, int keyCode, String category) {
        if (USE_NEW_KEYBINDING_API) {
            return createKeyBindingReflect(translationKey, keyCode, category, true);
        }
        return createKeyBindingReflect(translationKey, keyCode, category, false);
    }

    @Override
    public String resolvePlayerName(Object renderLabelContext) {
        try {
            Field nameField = renderLabelContext.getClass().getField("name");
            Object nameValue = nameField.get(renderLabelContext);
            if (nameValue instanceof String name && !name.isBlank()) {
                return name;
            }
        } catch (Exception ignored) {
        }

        try {
            Field playerNameField = renderLabelContext.getClass().getField("playerName");
            Object playerNameValue = playerNameField.get(renderLabelContext);
            if (playerNameValue instanceof Text playerNameText) {
                String extracted = playerNameText.getString();
                if (!extracted.isBlank()) {
                    return extracted;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    // Cached category object for reuse across multiple keybindings
    private Object cachedCategory = null;
    private Class<?> cachedCategoryClass = null;

    private Object resolveEntityModelLoader(MinecraftClient client) {
        String[] candidates = {"getEntityModelLoader", "getLoadedEntityModels", "getEntityModels"};
        for (String methodName : candidates) {
            try {
                Method method = client.getClass().getMethod(methodName);
                return method.invoke(client);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Object createSkinSupplier(Identifier textureId, String skinUrl) {
        try {
            Class<?> skinTexturesClass;
            try {
                skinTexturesClass = Class.forName("net.minecraft.client.util.SkinTextures");
            } catch (ClassNotFoundException ignored) {
                skinTexturesClass = Class.forName("net.minecraft.entity.player.SkinTextures");
            }

            Class<?> skinModelClass;
            Object defaultModel;
            try {
                skinModelClass = Class.forName("net.minecraft.client.util.SkinTextures$Model");
                defaultModel = Enum.valueOf((Class<Enum>) skinModelClass.asSubclass(Enum.class), "WIDE");
            } catch (ClassNotFoundException ignored) {
                skinModelClass = Class.forName("net.minecraft.entity.player.PlayerSkinType");
                defaultModel = Enum.valueOf((Class<Enum>) skinModelClass.asSubclass(Enum.class), "WIDE");
            }

            Constructor<?>[] constructors = skinTexturesClass.getConstructors();
            for (Constructor<?> ctor : constructors) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 6
                        && params[0] == Identifier.class
                        && params[1] == String.class
                        && params[4] == skinModelClass
                        && params[5] == boolean.class) {
                    Object skinTextures = ctor.newInstance(textureId, skinUrl, null, null, defaultModel, false);
                    return (Supplier<?>) () -> skinTextures;
                }

                if (params.length == 5
                        && params[0].getName().equals("net.minecraft.util.AssetInfo$TextureAsset")
                        && params[3] == skinModelClass
                        && params[4] == boolean.class) {
                    Class<?> assetInfoClass = Class.forName("net.minecraft.util.AssetInfo$SkinAssetInfo");
                    Constructor<?> assetCtor = assetInfoClass.getConstructor(Identifier.class, String.class);
                    Object body = assetCtor.newInstance(textureId, skinUrl);
                    Object skinTextures = ctor.newInstance(body, null, null, defaultModel, false);
                    return (Supplier<?>) () -> skinTextures;
                }
            }

            throw new RuntimeException("No compatible SkinTextures constructor found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SkinTextures supplier: " + e.getMessage(), e);
        }
    }

    private PlayerSkinWidget createPlayerSkinWidgetReflect(int width, int height, Object modelLoader, Object skinSupplier) {
        for (Constructor<?> constructor : PlayerSkinWidget.class.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length != 4) {
                continue;
            }

            if (params[0] != int.class || params[1] != int.class) {
                continue;
            }

            if (!Supplier.class.isAssignableFrom(params[3])) {
                continue;
            }

            if (!params[2].isInstance(modelLoader)) {
                continue;
            }

            try {
                return (PlayerSkinWidget) constructor.newInstance(width, height, modelLoader, skinSupplier);
            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException("No compatible PlayerSkinWidget constructor found");
    }

    private KeyBinding createKeyBindingReflect(String translationKey, int keyCode, String category, boolean useNewApi) {
        try {
            if (useNewApi) {
                if (cachedCategory == null) {
                    for (Class<?> inner : KeyBinding.class.getDeclaredClasses()) {
                        for (Method m : inner.getMethods()) {
                            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())
                                    && m.getParameterCount() == 1
                                    && m.getParameterTypes()[0] == Identifier.class
                                    && m.getReturnType() == inner) {
                                cachedCategoryClass = inner;
                                cachedCategory = m.invoke(null, Identifier.of("ctl-tiertagger", "controls"));
                                break;
                            }
                        }
                        if (cachedCategory != null) break;
                    }
                    if (cachedCategory == null) {
                        throw new RuntimeException("Could not find KeyBinding.Category class or register method");
                    }
                }

                Constructor<?> constructor = KeyBinding.class.getConstructor(
                    String.class, InputUtil.Type.class, int.class, cachedCategoryClass
                );
                return (KeyBinding) constructor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, cachedCategory);
            } else {
                Constructor<?> constructor = KeyBinding.class.getConstructor(
                    String.class, InputUtil.Type.class, int.class, String.class
                );
                return (KeyBinding) constructor.newInstance(translationKey, InputUtil.Type.KEYSYM, keyCode, category);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create KeyBinding via reflection: " + e.getMessage(), e);
        }
    }
}
