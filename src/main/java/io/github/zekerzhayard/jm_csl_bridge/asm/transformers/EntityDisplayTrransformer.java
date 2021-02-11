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

@TransformerManager.TransformTarget(className = "journeymap.client.ui.minimap.EntityDisplay")
public class EntityDisplayTrransformer implements TransformerManager.IClassTransformer {
    @Override
    public ClassNode transform(ClassNode classNode) {

        //
        // ...
        // public static TextureImpl getEntityTexture(EntityDisplay entityDisplay, UUID entityId, String playerName) {
        //     return getEntityTexture(entityDisplay, entityId, playerName, null);
        // }
        //
        // public static TextureImpl getEntityTexture(EntityDisplay entityDisplay, UUID entityId, String playerName, net.minecraft.entity.EntityLivingBase entityLiving) {
        // ...
        //

        MethodNode oldGetEntityTexture = null, newGetEntityTexture = null;

        for (MethodNode mn : classNode.methods) {
            if (
                Objects.equals(mn.name, "getEntityTexture")
                    && Objects.equals(mn.desc, "(Ljourneymap/client/ui/minimap/EntityDisplay;Ljava/util/UUID;Ljava/lang/String;)Ljourneymap/client/render/texture/TextureImpl;")
            ) {
                oldGetEntityTexture = mn;
                newGetEntityTexture = new MethodNode(mn.access, mn.name, mn.desc, mn.signature, mn.exceptions.toArray(new String[0]));
                LocalVariableNode entityDisplay = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 0));
                LocalVariableNode entityId = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 1));
                LocalVariableNode playerName = Objects.requireNonNull(ASMUtils.findLocalVariable(mn, 0, 2));
                LabelNode start = new LabelNode(), end = new LabelNode();
                newGetEntityTexture.localVariables.add(new LocalVariableNode(entityDisplay.name, entityDisplay.desc, entityDisplay.signature, start, end, entityDisplay.index));
                newGetEntityTexture.localVariables.add(new LocalVariableNode(entityId.name, entityId.desc, entityId.signature, start, end, entityId.index));
                newGetEntityTexture.localVariables.add(new LocalVariableNode(playerName.name, playerName.desc, playerName.signature, start, end, playerName.index));

                mn.desc = "(Ljourneymap/client/ui/minimap/EntityDisplay;Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljourneymap/client/render/texture/TextureImpl;";
                ASMUtils.insertLocalVariable(mn, new LocalVariableNode("entityLiving", "Lnet/minecraft/entity/EntityLivingBase;", null, entityDisplay.start, entityDisplay.end, 3), mn.localVariables.size());

                for (AbstractInsnNode ain : mn.instructions.toArray()) {
                    if (ain.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode min = (MethodInsnNode) ain;
                        if (
                            Objects.equals(min.owner, "journeymap/client/render/texture/TextureCache")
                                && Objects.equals(min.name, "getPlayerSkin")
                                && Objects.equals(min.desc, "(Ljava/util/UUID;Ljava/lang/String;)Ljourneymap/client/render/texture/TextureImpl;")
                        ) {
                            mn.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, 3));
                            min.desc = "(Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljourneymap/client/render/texture/TextureImpl;";
                        }
                    }
                }

                InsnList il = new InsnList();
                il.add(start);
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                il.add(new VarInsnNode(Opcodes.ALOAD, 1));
                il.add(new VarInsnNode(Opcodes.ALOAD, 2));
                il.add(new InsnNode(Opcodes.ACONST_NULL));
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.name, mn.name, mn.desc, false));
                il.add(new InsnNode(Opcodes.ARETURN));
                il.add(end);
                newGetEntityTexture.instructions.add(il);
            }
        }

        classNode.methods.add(classNode.methods.indexOf(Objects.requireNonNull(oldGetEntityTexture)), Objects.requireNonNull(newGetEntityTexture));
        return classNode;
    }
}
