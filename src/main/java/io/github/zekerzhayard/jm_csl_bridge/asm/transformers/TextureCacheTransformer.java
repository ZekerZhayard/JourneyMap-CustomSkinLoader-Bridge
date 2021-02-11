package io.github.zekerzhayard.jm_csl_bridge.asm.transformers;

import java.util.Objects;

import customskinloader.forge.TransformerManager;
import io.github.zekerzhayard.jm_csl_bridge.asm.utils.ASMUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

@TransformerManager.TransformTarget(className = "journeymap.client.render.texture.TextureCache")
public class TextureCacheTransformer implements TransformerManager.IClassTransformer {
    @Override
    public ClassNode transform(ClassNode classNode) {

        //
        // ...
        // public static TextureImpl getPlayerSkin(UUID playerId, String username) {
        //     return getPlayerSkin(playerId, username, null);
        // }
        //
        // public static TextureImpl getPlayerSkin(UUID playerId, String username, net.minecraft.entity.EntityLivingBase entityLiving) {
        // ...

        MethodNode oldGetPlayerSkin = null, newGetPlayerSkin = null;

        for (MethodNode mn : classNode.methods) {
            if (
                Objects.equals(mn.name, "getPlayerSkin")
                    && Objects.equals(mn.desc, "(Ljava/util/UUID;Ljava/lang/String;)Ljourneymap/client/render/texture/TextureImpl;")
            ) {
                oldGetPlayerSkin = mn;
                newGetPlayerSkin = new MethodNode(mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]));
                LocalVariableNode playerId = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 0));
                LocalVariableNode username = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 1));
                LabelNode start = new LabelNode(), end = new LabelNode();
                newGetPlayerSkin.localVariables.add(new LocalVariableNode(playerId.name, playerId.desc, playerId.signature, start, end, playerId.index));
                newGetPlayerSkin.localVariables.add(new LocalVariableNode(username.name, username.desc, username.signature, start, end, username.index));

                mn.desc = "(Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljourneymap/client/render/texture/TextureImpl;";
                ASMUtils.insertLocalVariable(mn, new LocalVariableNode("entityLiving", "Lnet/minecraft/entity/EntityLivingBase;", null, playerId.start, playerId.end, 2), mn.localVariables.size());

                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                        InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
                        if (
                            Objects.equals(idin.name, "call")
                                && Objects.equals(idin.desc, "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;)Ljava/util/concurrent/Callable;")
                                && idin.bsmArgs.length == 3
                                && idin.bsmArgs[1] instanceof Handle
                        ) {
                            Handle handle = (Handle) idin.bsmArgs[1];
                            if (
                                handle.getTag() == Opcodes.H_INVOKESTATIC
                                    && Objects.equals(handle.getOwner(), "journeymap/client/render/texture/TextureCache")
                                    && Objects.equals(handle.getName(), "lambda$getPlayerSkin$0")
                                    && Objects.equals(handle.getDesc(), "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;)Ljava/lang/Void;")
                            ) {
                                mn.instructions.insertBefore(idin, new VarInsnNode(Opcodes.ALOAD, 2));
                                idin.desc = "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;Lnet/minecraft/entity/EntityLivingBase;)Ljava/util/concurrent/Callable;";
                                idin.bsmArgs[1] = new Handle(handle.getTag(), handle.getOwner(), handle.getName(), "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;Lnet/minecraft/entity/EntityLivingBase;)Ljava/lang/Void;", handle.isInterface());
                            }
                        }
                    } else if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (
                            min.getOpcode() == Opcodes.INVOKESPECIAL
                                && Objects.equals(min.owner, "java/awt/image/BufferedImage")
                                && Objects.equals(min.name, "<init>")
                                && Objects.equals(min.desc, "(III)V")
                        ) {
                            mn.instructions.insert(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                    "setBlankImageProperty",
                                    "(Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage;",
                                    false
                                )
                            );
                        } else if (
                            min.getOpcode() == Opcodes.INVOKEVIRTUAL
                                && Objects.equals(min.owner, "java/util/concurrent/ThreadPoolExecutor")
                                && Objects.equals(min.name, "submit")
                                && Objects.equals(min.desc, "(Ljava/util/concurrent/Callable;)Ljava/util/concurrent/Future;")
                        ) {
                            // texExec.submit(() -> {...}); -> texExec.execute(TextureHelper.convertCallable(() -> {...}));
                            // Ensure that the error can be printed in the log.
                            mn.instructions.insertBefore(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                    "convertCallable",
                                    "(Ljava/util/concurrent/Callable;)Ljava/lang/Runnable;",
                                    false
                                )
                            );
                            min.name = "execute";
                            min.desc = "(Ljava/lang/Runnable;)V";
                            mn.instructions.remove(min.getNext());
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
                newGetPlayerSkin.instructions.add(il);
            } else if (
                Objects.equals(mn.name, "lambda$getPlayerSkin$0")
                    && Objects.equals(mn.desc, "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;)Ljava/lang/Void;")
            ) {
                LocalVariableNode playerId = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 0));

                mn.desc = "(Ljava/util/UUID;Ljava/lang/String;Ljourneymap/client/render/texture/TextureImpl;Lnet/minecraft/entity/EntityLivingBase;)Ljava/lang/Void;";
                ASMUtils.insertLocalVariable(mn, new LocalVariableNode("entityLiving", "Lnet/minecraft/entity/EntityLivingBase;", null, playerId.start, playerId.end, 3), mn.localVariables.size());

                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (
                            ain.getOpcode() == Opcodes.INVOKESTATIC
                                && Objects.equals(min.owner, "journeymap/client/render/texture/IgnSkin")
                                && Objects.equals(min.name, "getFaceImage")
                                && Objects.equals(min.desc, "(Ljava/util/UUID;Ljava/lang/String;)Ljava/awt/image/BufferedImage;")
                        ) {
                            mn.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, 3));
                            min.desc = "(Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljava/awt/image/BufferedImage;";
                        } else if (
                            min.getOpcode() == Opcodes.INVOKEVIRTUAL
                                && Objects.equals(min.owner, "journeymap/client/render/texture/TextureImpl")
                                && Objects.equals(min.name, "setImage")
                                && Objects.equals(min.desc, "(Ljava/awt/image/BufferedImage;Z)V")
                        ) {
                            mn.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, 3));
                            mn.instructions.set(
                                min,
                                new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "io/github/zekerzhayard/jm_csl_bridge/textures/TextureHelper",
                                    "setImage",
                                    "(Ljourneymap/client/render/texture/TextureImpl;Ljava/awt/image/BufferedImage;ZLnet/minecraft/entity/EntityLivingBase;)V",
                                    false
                                )
                            );
                        }
                    }
                }
            } else if (
                Objects.equals(mn.name, "<clinit>")
                    && Objects.equals(mn.desc, "()V")
            ) {
                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain.getOpcode() == Opcodes.NEW) {
                        TypeInsnNode tin = (TypeInsnNode) ain;
                        if (Objects.equals(tin.desc, "java/util/concurrent/ArrayBlockingQueue")) {
                            tin.desc = "java/util/concurrent/LinkedBlockingQueue";
                        }
                    } else if (ain.getOpcode() == Opcodes.INVOKESPECIAL) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (
                            Objects.equals(min.owner, "java/util/concurrent/ArrayBlockingQueue")
                                && Objects.equals(min.name, "<init>")
                                && Objects.equals(min.desc, "(I)V")
                        ) {
                            mn.instructions.remove(min.getPrevious());
                            min.owner = "java/util/concurrent/LinkedBlockingQueue";
                            min.desc = "()V";
                        }
                    }
                }
            }
        }

        classNode.methods.add(classNode.methods.indexOf(Objects.requireNonNull(oldGetPlayerSkin)), Objects.requireNonNull(newGetPlayerSkin));
        return classNode;
    }
}
