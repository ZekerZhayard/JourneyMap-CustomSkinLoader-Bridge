package io.github.zekerzhayard.jm_csl_bridge.asm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMUtils {
    public static String getMethodName(String srgName, String mcpName) {
        return (boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment") ? mcpName : srgName;
    }

    public static LocalVariableNode findLocalVariable(MethodNode mn, int ordinal, int index) {
        int count = 0;
        for (LocalVariableNode lvn : mn.localVariables) {
            if (lvn.index == index) {
                if (count == ordinal) {
                    return lvn;
                }
                count++;
            }
        }
        return null;
    }

    public static LocalVariableNode findLocalVariable(MethodNode mn, int ordinal, String desc) {
        int count = 0;
        for (LocalVariableNode lvn : mn.localVariables) {
            if (Objects.equals(lvn.desc, desc)) {
                if (count == ordinal) {
                    return lvn;
                }
                count++;
            }
        }
        return null;
    }

    public static void insertLocalVariable(MethodNode mn, LocalVariableNode lvn, int index) {
        insertLocalVariable(mn.localVariables, mn.instructions.toArray(), lvn, index);
    }

    public static void insertLocalVariable(List<LocalVariableNode> lvns, AbstractInsnNode[] ains, LocalVariableNode lvn, int index) {
        int shift = lvn.desc.equals("J") || lvn.desc.equals("D") ? 2 : 1;
        for (LocalVariableNode node : lvns) {
            if (node.index >= lvn.index) {
                node.index += shift;
            }
        }

        boolean inScope = false;
        for (AbstractInsnNode ain : ains) {
            if ((ain.getOpcode() >= Opcodes.ILOAD && ain.getOpcode() <= Opcodes.ALOAD) || (ain.getOpcode() >= Opcodes.ISTORE && ain.getOpcode() <= Opcodes.ASTORE)) {
                VarInsnNode vin = (VarInsnNode) ain;
                if (vin.var >= lvn.index) {
                    vin.var += shift;
                }
            } else if (ain.getOpcode() == Opcodes.IINC) {
                IincInsnNode iin = (IincInsnNode) ain;
                if (iin.var >= lvn.index) {
                    iin.var += shift;
                }
            } else if (Objects.equals(ain, lvn.start) && !inScope) {
                inScope = true;
            } else if (Objects.equals(ain, lvn.end) && inScope) {
                inScope = false;
            } else if (inScope && ain instanceof FrameNode) {
                FrameNode fn = (FrameNode) ain;
                if (fn.type == Opcodes.F_NEW || fn.type == Opcodes.F_FULL) {
                    Type type = Type.getType(lvn.desc);
                    fn.local = new ArrayList<>(fn.local);
                    fn.local.add(lvn.index, type.getSort() < 9 ? lvn.desc : type.getInternalName());
                }
            }
        }
        lvns.add(index, lvn);
    }
}
