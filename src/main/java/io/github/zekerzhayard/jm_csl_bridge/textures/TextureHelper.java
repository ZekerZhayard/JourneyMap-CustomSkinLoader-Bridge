package io.github.zekerzhayard.jm_csl_bridge.textures;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import customskinloader.CustomSkinLoader;
import customskinloader.utils.HttpTextureUtil;
import journeymap.client.render.texture.IgnSkin;
import journeymap.client.render.texture.TextureCache;
import journeymap.client.render.texture.TextureImpl;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntitySkull;

public class TextureHelper {
    private final static Field propertiesField; // Hashtable<String, Object>
    private final static Field metadataField; // Map<String, String>

    private final static Method cropToFaceMethod; // BufferedImage (BufferedImage)

    static {
        try {
            propertiesField = BufferedImage.class.getDeclaredField("properties");
            propertiesField.setAccessible(true);

            metadataField = MinecraftProfileTexture.class.getDeclaredField("metadata");
            metadataField.setAccessible(true);

            cropToFaceMethod = IgnSkin.class.getDeclaredMethod("cropToFace", BufferedImage.class);
            cropToFaceMethod.setAccessible(true);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // Called from journeymap.client.render.texture.TextureCache#getPlayerSkin
    @SuppressWarnings("unchecked")
    public static BufferedImage setBlankImageProperty(BufferedImage image) {
        try {
            Hashtable<String, Object> properties = (Hashtable<String, Object>) propertiesField.get(image);
            if (properties == null) {
                properties = new Hashtable<>();
            }
            propertiesField.set(image, properties = new Hashtable<>(properties));
            properties.put("jm-csl-isBlank", true);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return image;
    }

    // Called from journeymap.client.render.texture.TextureCache#getPlayerSkin
    public static Runnable convertCallable(Callable<Void> callable) {
        return () -> {
            try {
                callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    // Called from journeymap.client.render.texture.TextureCache#lambda$getPlayerSkin$0
    public static void setImage(TextureImpl texture, BufferedImage image, boolean retainImage, EntityLivingBase entityLiving) {
        GameProfile profile = getGameProfile(null, entityLiving);
        if (profile != null && profile.getName() != null) {
            TextureImpl textureImpl;
            synchronized (TextureCache.playerSkins) {
                textureImpl = TextureCache.playerSkins.get(profile.getName());
            }
            // Make sure the face image has not been loaded by this mod. (after requesting the url)
            if (!Objects.equals(textureImpl.getImage().getProperty("jm-csl-isBlank"), true)) {
                return;
            }
        }
        texture.setImage(image, retainImage);
    }

    // Called from journeymap.client.render.texture.IgnSkin#getFaceImage
    public static GameProfile getGameProfile(GameProfile profile, EntityLivingBase entityLiving) {
        return entityLiving instanceof EntityPlayer ? ((EntityPlayer) entityLiving).getGameProfile() : TileEntitySkull.updateGameProfile(profile);
    }

    // Called from journeymap.client.render.texture.IgnSkin#getFaceImage
    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(MinecraftSessionService mss, GameProfile profile, boolean requireSecure) {
        TextureImpl textureImpl;
        synchronized (TextureCache.playerSkins) {
            textureImpl = TextureCache.playerSkins.get(profile.getName());
        }
        // Make sure the face image has not been loaded by this mod. (before requesting the url)
        if (textureImpl != null && !Objects.equals(textureImpl.getImage().getProperty("jm-csl-isBlank"), true)) {
            HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = new HashMap<>();
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("jm-csl-isBlank", "false");
            map.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(null, metadata));
            return map;
        } else {
            CustomSkinLoader.logger.info("[JourneyMap-CustomSkinLoader-Bridge] Start loading " + profile.getName() + "'s skin.");
            return CustomSkinLoader.loadProfile(profile);
        }
    }

    // Called from journeymap.client.render.texture.IgnSkin#getFaceImage
    public static String convertUrl(MinecraftProfileTexture profileTexture) {
        if (Objects.equals(profileTexture.getMetadata("jm-csl-isBlank"), "false")) {
            return "Ignore this exception because this face image has been loaded by JourneyMap-CustomSkinLoader-Bridge mod.";
        } else {
            return HttpTextureUtil.toHttpTextureInfo(profileTexture.getUrl()).url;
        }
    }

    // Called from customskinloader.fake.FakeSkinManager#loadSkin
    public static SimpleTexture addTextureCallback(SimpleTexture texture, MinecraftProfileTexture profileTexture) {
        if (texture instanceof IThreadDownloadImageData) {
            ((IThreadDownloadImageData) texture).setCallback(image -> {
                String name = profileTexture.getMetadata("jm-csl-palyerName");
                if (name != null) {
                    BufferedImage face;
                    try {
                        face = (BufferedImage) cropToFaceMethod.invoke(null, image);
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    
                    TextureImpl textureImpl;
                    synchronized (TextureCache.playerSkins) {
                        textureImpl = TextureCache.playerSkins.get(name);
                    }
                    if (textureImpl == null) {
                        synchronized (TextureCache.playerSkins) {
                            TextureCache.playerSkins.put(name, new TextureImpl(null, face, true, false));
                        }
                    } else {
                        textureImpl.setImage(face, true);
                    }
                }
            });
        }

        return texture;
    }

    // Called from customskinloader.fake.FakeSkinManager#lambda$loadProfileTextures$1
    @SuppressWarnings("unchecked")
    public static MinecraftProfileTexture addMetadata(MinecraftProfileTexture profileTexture, MinecraftProfileTexture.Type type, GameProfile profile) {
        if (profile.getName() != null && Objects.equals(type, MinecraftProfileTexture.Type.SKIN)) {
            try {
                Map<String, String> metadata = (Map<String, String>) metadataField.get(profileTexture);
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
                metadataField.set(profileTexture, metadata = new HashMap<>(metadata));
                metadata.put("jm-csl-palyerName", profile.getName());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        return profileTexture;
    }
}
