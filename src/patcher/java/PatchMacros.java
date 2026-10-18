import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Keeps the original mod and replaces only the removed input integration. */
public final class PatchMacros implements Opcodes {
    private static final String INPUT = "net/eq2online/macros/input/InputHandler";
    private static final String ADAPTER = "com/litemod/Svg7Input";

    public static void main(String[] args) throws Exception {
        int patched = 0;
        try (ZipFile source = new ZipFile(args[0]);
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(Paths.get(args[1])))) {
            java.util.Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] bytes;
                try (InputStream in = source.getInputStream(entry)) { bytes = read(in); }
                if (entry.getName().equals(INPUT + ".class") || entry.getName().equals("com/litemod/MacroBridge.class") ||
                    entry.getName().equals("net/eq2online/macros/core/MacroModCore.class") ||
                    entry.getName().equals("net/eq2online/macros/input/KeyInjectionProxy.class")) {
                    bytes = patch(bytes);
                    patched++;
                }
                output.putNextEntry(new ZipEntry(entry.getName()));
                output.write(bytes);
                output.closeEntry();
            }
            if (patched != 4) throw new IllegalStateException("Expected four integration classes");
            output.putNextEntry(new ZipEntry(ADAPTER + ".class"));
            output.write(Files.readAllBytes(Paths.get(args[2])));
            output.closeEntry();
        }
        System.out.println("Patched keyboard and mouse integration; original scripts and renderer retained");
    }

    private static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while ((size = in.read(buffer)) != -1) out.write(buffer, 0, size);
        return out.toByteArray();
    }

    private static byte[] patch(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassVisitor(ASM9, writer) {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals("getBuffers") && desc.equals("()V")) {
                    mv.visitCode();
                    for (String field : new String[]{"mouseReadBuffer", "mouseDownBuffer", "keyReadBuffer", "keyDownBuffer"}) {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETSTATIC, ADAPTER, field, "Ljava/nio/ByteBuffer;");
                        mv.visitFieldInsn(PUTFIELD, INPUT, field, "Ljava/nio/ByteBuffer;");
                    }
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(2, 1);
                    mv.visitEnd();
                    return null;
                }
                return new MethodVisitor(ASM9, mv) {
                    public void visitMethodInsn(int op, String owner, String method, String descriptor, boolean itf) {
                        if (owner.equals("dev/xavier/stein/bridge/core/LegacyInput") ||
                            (owner.equals("org/lwjgl/input/Keyboard") && method.equals("setLegacyExportStop"))) owner = ADAPTER;
                        super.visitMethodInsn(op, owner, method, descriptor, itf);
                    }
                };
            }
        }, 0);
        return writer.toByteArray();
    }
}
