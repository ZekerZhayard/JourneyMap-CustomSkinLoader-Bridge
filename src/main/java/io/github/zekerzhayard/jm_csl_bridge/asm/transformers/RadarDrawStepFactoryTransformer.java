package io.github.zekerzhayard.jm_csl_bridge.asm.transformers;

import java.util.Objects;

import customskinloader.forge.TransformerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class RadarDrawStepFactoryTransformer {
    @TransformerManager.TransformTarget(
        className = "journeymap.client.render.draw.RadarDrawStepFactory",
        methodNames = "prepareSteps",
        desc = "(Ljava/util/List;Ljourneymap/client/render/map/GridRenderer;Ljourneymap/client/properties/InGameMapProperties;)Ljava/util/List;"
    )
    public static class PrepareStepsTransformer implements TransformerManager.IMethodTransformer {
        @Override
        public MethodNode transform(ClassNode classNode, MethodNode methodNode) {
            int entityLivingIndex = -1;

            for (LocalVariableNode lvn : methodNode.localVariables) {
                if (Objects.equals(lvn.name, "entityLiving")) {
                    entityLivingIndex = lvn.index;
                }
            }
            if (entityLivingIndex == -1) {
                throw new RuntimeException("Incompatible JourneyMap version!");
            }

            for (AbstractInsnNode ain : methodNode.instructions.toArray()) {
                if (ain.getOpcode() == Opcodes.INVOKESTATIC) {
                    MethodInsnNode min = (MethodInsnNode) ain;
                    if (
                        Objects.equals(min.owner, "journeymap/client/ui/minimap/EntityDisplay")
                            && Objects.equals(min.name, "getEntityTexture")
                            && Objects.equals(min.desc, "(Ljourneymap/client/ui/minimap/EntityDisplay;Ljava/util/UUID;Ljava/lang/String;)Ljourneymap/client/render/texture/TextureImpl;")
                    ) {
                        methodNode.instructions.insertBefore(min, new VarInsnNode(Opcodes.ALOAD, entityLivingIndex));
                        min.desc = "(Ljourneymap/client/ui/minimap/EntityDisplay;Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/entity/EntityLivingBase;)Ljourneymap/client/render/texture/TextureImpl;";
                        break;
                    }
                }
            }
            return methodNode;
        }
    }
}
