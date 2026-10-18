import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Converts font display lists to tessellator commands; the loader stays intact. */
public final class PatchMacrosGfx implements Opcodes {
    public static void main(String[] args) throws Exception {
        int patched = 0;
        try (ZipFile source = new ZipFile(args[0]);
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Paths.get(args[1])))) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes;
                try (InputStream in = source.getInputStream(entry)) { bytes = read(in); }
                String name = entry.getName();
                if (name.equals("com/mumfrey/liteloader/gl/GL.class")) { bytes = patchGl(bytes); patched++; }
                if (name.equals("net/eq2online/macros/rendering/FontRendererLegacy.class") ||
                    name.equals("net/eq2online/macros/rendering/FontRendererTextEditor.class")) {
                    bytes = patchFont(bytes); patched++;
                }
                if (name.endsWith(".class")) bytes = routeTessellator(bytes);
                output.putNextEntry(new ZipEntry(name)); output.write(bytes); output.closeEntry();
            }
            if (patched != 3) throw new IllegalStateException("Expected exactly three graphics classes");
            try (DirectoryStream<Path> helpers = Files.newDirectoryStream(Paths.get(args[2]), "Svg7*.class")) {
                for (Path helper : helpers) {
                    output.putNextEntry(new ZipEntry(helper.getFileName().toString()));
                    output.write(Files.readAllBytes(helper)); output.closeEntry();
                }
            }
        }
        System.out.println("Patched two fonts and overlay state; Stein GFX remains enabled");
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int size;
        while ((size = in.read(buffer)) != -1) out.write(buffer, 0, size);
        return out.toByteArray();
    }

    private static byte[] patchFont(byte[] bytes) {
        ClassNode cls = new ClassNode(); new ClassReader(bytes).accept(cls, 0);
        boolean legacy = cls.name.endsWith("FontRendererLegacy");
        boolean replaced = false;
        for (MethodNode method : cls.methods) {
            if (!method.name.equals("<init>")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode)) continue;
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (field.getOpcode() != PUTFIELD || !field.name.equals("fontDisplayLists")) continue;
                AbstractInsnNode allocation = field.getPrevious();
                if (!(allocation instanceof MethodInsnNode) || !((MethodInsnNode) allocation).owner.equals("avd"))
                    throw new IllegalStateException("Font allocation changed");
                InsnList registration = new InsnList();
                registration.add(new InsnNode(POP)); // original list count
                registration.add(new VarInsnNode(ALOAD, 0));
                registration.add(new FieldInsnNode(GETFIELD, cls.name, legacy ? "d" : "fixedWidth", legacy ? "[I" : "I"));
                registration.add(new MethodInsnNode(INVOKESTATIC, "Svg7Graphics", legacy ? "legacy" : "editor", legacy ? "([I)I" : "(I)I", false));
                method.instructions.insertBefore(allocation, registration);
                method.instructions.remove(allocation);
                while (field.getNext() != null) method.instructions.remove(field.getNext());
                method.instructions.add(new InsnNode(RETURN));
                method.localVariables = null;
                replaced = true;
                break;
            }
        }
        if (!replaced) throw new IllegalStateException("Font constructor not found");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cls.accept(writer); return writer.toByteArray();
    }

    private static byte[] patchGl(byte[] bytes) {
        ClassNode cls = new ClassNode(); new ClassReader(bytes).accept(cls, 0);
        Map<String, String> helpers = new HashMap<String, String>();
        helpers.put("glCallLists", "draw"); helpers.put("glNewList", "begin"); helpers.put("glEndList", "end");
        helpers.put("glColor3f", "color3f"); helpers.put("glPushAttrib", "push"); helpers.put("glPopAttrib", "pop");
        helpers.put("glEnableColorLogic", "logicEnable"); helpers.put("glDisableColorLogic", "logicDisable"); helpers.put("glLogicOp", "logicOp");
        Map<String, String> state = new HashMap<String, String>();
        int replaced = 0;
        for (MethodNode method : cls.methods) {
            if (!helpers.containsKey(method.name) && !state.containsKey(method.name)) continue;
            method.instructions.clear(); method.tryCatchBlocks.clear(); method.localVariables = null;
            int slot = 0;
            for (Type type : Type.getArgumentTypes(method.desc)) {
                method.instructions.add(new VarInsnNode(type.getOpcode(ILOAD), slot)); slot += type.getSize();
            }
            boolean helper = helpers.containsKey(method.name);
            method.instructions.add(new MethodInsnNode(INVOKESTATIC, helper ? "Svg7Graphics" : "bfl",
                helper ? helpers.get(method.name) : state.get(method.name), method.desc, false));
            method.instructions.add(new InsnNode(RETURN));
            replaced++;
        }
        if (replaced != 9) throw new IllegalStateException("Expected nine compatibility wrappers, got " + replaced);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cls.accept(writer); return writer.toByteArray();
    }

    private static byte[] routeTessellator(byte[] bytes) {
        final boolean[] changed = {false};
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassVisitor(ASM9, writer) {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
                    public void visitMethodInsn(int op, String owner, String method, String descriptor, boolean itf) {
                        if (op == INVOKESTATIC && owner.equals("dev/xavier/stein/loader/compat/Reflect") && method.equals("setField")) {
                            changed[0] = true;
                            super.visitMethodInsn(op, "Svg7Reflect", method, descriptor, false);
                        } else if (op == INVOKEVIRTUAL && owner.equals("bfx") && method.equals("b") && descriptor.equals("()V")) {
                            changed[0] = true;
                            super.visitMethodInsn(INVOKESTATIC, "Svg7Graphics", "submit", "(Ljava/lang/Object;)V", false);
                        } else if (op == INVOKESTATIC && owner.equals("net/eq2online/macros/gui/controls/GuiTextEditor") &&
                            method.equals("a") && descriptor.equals("(IIIII)V")) {
                            changed[0] = true;
                            super.visitMethodInsn(INVOKESTATIC, "Svg7Graphics", "rect", descriptor, false);
                        } else super.visitMethodInsn(op, owner, method, descriptor, itf);
                    }
                };
            }
        }, 0);
        return changed[0] ? writer.toByteArray() : bytes;
    }
}
