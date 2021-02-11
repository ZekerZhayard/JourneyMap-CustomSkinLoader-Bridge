package io.github.zekerzhayard.jm_csl_bridge.asm.transformers;

import java.util.Objects;

import customskinloader.forge.TransformerManager;
import io.github.zekerzhayard.jm_csl_bridge.asm.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FakeSkinManagerTransformer {
    @TransformerManager.TransformTarget(
        className = "customskinloader.fake.FakeSkinManager",
        methodNames = "loadSkin",
        desc = "(Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcustomskinloader/utils/HttpTextureUtil$HttpTextureInfo;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;Lnet/minecraft/client/resources/SkinManager$SkinAvailableCallback;)Lnet/minecraft/util/ResourceLocation;"
    )
    public static class LoadSkinTransformer implements TransformerManager.IMethodTransformer {
        @Override
        public MethodNode transform(ClassNode classNode, MethodNode methodNode) {
            for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                if (ain.getOpcode() == Opcodes.CHECKCAST) {
                    TypeInsnNode tin = (TypeInsnNode) ain;
                    if (Objects.equals(tin.desc, "net/minecraft/client/renderer/texture/Texture")) {
                        methodNode.instructions.insertBefore(tin, new VarInsnNode(Opcodes.ALOAD, 1));
                        methodNode.instructions.insertBefore(
                            tin,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                "addTextureCallback",
                                "(Lnet/minecraft/client/renderer/texture/SimpleTexture;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;)Lnet/minecraft/client/renderer/texture/SimpleTexture;",
                                false
                            )
                        );
                    }
                }
            }

            return methodNode;
        }
    }

    @TransformerManager.TransformTarget(
        className = "customskinloader.fake.FakeSkinManager",
        methodNames = "lambda$loadProfileTextures$1",
        desc = "(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/client/resources/SkinManager$SkinAvailableCallback;)V"
    )
    public static class Lambda$loadProfileTextures$1Transformer implements TransformerManager.IMethodTransformer {
        @Override
        public MethodNode transform(ClassNode classNode, MethodNode methodNode) {
            for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                if (ain.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    if (
                        Objects.equals(min.owner, "com/mojang/authlib/minecraft/MinecraftProfileTexture")
                            && Objects.equals(min.name, "getUrl")
                            && Objects.equals(min.desc, "()Ljava/lang/String;")
                    ) {
                        methodNode.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, Objects.requireNonNull(ASMUtils.findLocalVariable(methodNode, 0, "Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;")).index));
                        methodNode.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, 1));
                        methodNode.instructions.insertBefore(
                            min,
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                "addMetadata",
                                "(Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;",
                                false
                            )
                        );
                    }
                }
            }

            return methodNode;
        }
    }
}
