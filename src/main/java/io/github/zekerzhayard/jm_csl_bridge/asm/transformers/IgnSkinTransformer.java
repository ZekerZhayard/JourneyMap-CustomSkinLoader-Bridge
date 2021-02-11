package io.github.zekerzhayard.jm_csl_bridge.asm.transformers;

import java.util.Objects;

import customskinloader.forge.TransformerManager;
import io.github.zekerzhayard.jm_csl_bridge.asm.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

@TransformerManager.TransformTarget(className = "journeymap.client.render.texture.IgnSkin")
public class IgnSkinTransformer implements TransformerManager.IClassTransformer {
    @Override
    public ClassNode transform(ClassNode classNode) {

        //
        // ...
        // public static BufferedImage getFaceImage(UUID playerId, String username) {
        //     return getFaceImage(playerId, username, null);
        // }
        //
        // public static BufferedImage getFaceImage(UUID playerId, String username, net.minecraft.entity.EntityLivingBase entityLiving) {
        // ...
        //

        MethodNode oldGetFaceImage = null, newGetFaceImage = null;

        for (MethodNode mn : classNode.methods) {
            if (
                Objects.equals(mn.name, "getFaceImage")
                    && Objects.equals(mn.desc, "(Ljava/util/UUID;Ljava/lang/String;)Ljava/awt/image/BufferedImage;")
            ) {
                oldGetFaceImage = mn;
                newGetFaceImage = new MethodNode(mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]));
                LocalVariableNode playerId = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 0));
                LocalVariableNode username = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 1));
                LabelNode start = new LabelNode(), end = new LabelNode();
                newGetFaceImage.localVariables.add(new LocalVariableNode(playerId.name, playerId.desc, playerId.signature, start, end, playerId.index));
                newGetFaceImage.localVariables.add(new LocalVariableNode(username.name, username.desc, username.signature, start, end, username.index));

                mn.desc = "(Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljava/awt/image/BufferedImage;";
                ASMUtils.insertLocalVariable(mn, new LocalVariableNode("entityLiving", "Lnet/minecraft/entity/EntityLivingBase;", null, playerId.start, playerId.end, 2), mn.localVariables.size());

                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (
                            min.getOpcode() == Opcodes.INVOKESTATIC
                                && Objects.equals(min.owner, "net/minecraft/tileentity/TileEntitySkull")
                                && Objects.equals(min.name, ASMUtils.getMethodName("func_174884_b", "updateGameProfile"))
                                && Objects.equals(min.desc, "(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;")
                        ) {
                            mn.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, 2));
                            min.owner = "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper";
                            min.name = "getGameProfile";
                            min.desc = "(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/entity/EntityLivingBase;)Lcom/mojang/authlib/GameProfile;";
                        } else if (
                            min.getOpcode() == Opcodes.INVOKEINTERFACE
                                && Objects.equals(min.owner, "com/mojang/authlib/minecraft/MinecraftSessionService")
                                && Objects.equals(min.name, "getTextures")
                                && Objects.equals(min.desc, "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;")
                        ) {
                            mn.instructions.set(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                    "getTextures",
                                    "(Lcom/mojang/authlib/minecraft/MinecraftSessionService;Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;",
                                    false
                                )
                            );
                        } else if (
                            min.getOpcode() == Opcodes.INVOKEVIRTUAL
                                && Objects.equals(min.owner, "com/mojang/authlib/minecraft/MinecraftProfileTexture")
                                && Objects.equals(min.name, "getUrl")
                                && Objects.equals(min.desc, "()Ljava/lang/String;")
                        ) {
                            mn.instructions.set(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                    "convertUrl",
                                    "(Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;)Ljava/lang/String;",
                                    false
                                )
                            );
                        }
                    }
                }

                InsnList il = new InsnList();
                il.add(start);
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                il.add(new InsnNode(Opcodes.ACONST_NULL));
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, mn.name, mn.desc, false));
                il.add(new InsnNode(Opcodes.ARETURN));
                il.add(end);
                newGetFaceImage.instructions.add(il);
            }
        }

        classNode.methods.add(classNode.methods.indexOf(Objects.requireNonNull(oldGetFaceImage)), Objects.requireNonNull(newGetFaceImage));

        return classNode;
    }
}
