import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import com.mumfrey.liteloader.gl.GL;
import dev.xavier.stein.bridge.core.CoreOptions;
import dev.xavier.stein.gfx.SteinTessCapture;

/** CPU font commands rendered through Minecraft's tessellator and Stein GFX. */
public final class Svg7Graphics {
    private static final TreeMap<Integer, Font> fonts = new TreeMap<Integer, Font>();
    private static final ArrayDeque<State> states = new ArrayDeque<State>();
    private static int next = 1;
    private static int editing;
    private static boolean logicEnabled;
    private static int logicOpcode = 5379;

    private Svg7Graphics() {}

    public static int legacy(int[] widths) {
        Font font = new Font(widths.clone(), false);
        for (int code = 0; code < 32; code++) {
            int extra = (code >> 3 & 1) * 85;
            int r = (code >> 2 & 1) * 170 + extra;
            int g = (code >> 1 & 1) * 170 + extra;
            int b = (code & 1) * 170 + extra;
            if (code == 6) r += 85;
            if (code >= 16) { r /= 4; g /= 4; b /= 4; }
            font.colors[code] = new float[]{r / 255f, g / 255f, b / 255f};
        }
        return register(font, 288);
    }

    public static int editor(int width) {
        int[] widths = new int[256];
        Arrays.fill(widths, width);
        Font font = new Font(widths, true);
        font.colors[1] = new float[]{.5f, .5f, 1f};
        font.colors[2] = new float[]{.4f, .2f, 0f};
        font.colors[3] = new float[]{1f, 1f, 1f};
        font.colors[4] = new float[]{0f, .5f, 0f};
        font.colors[5] = new float[]{.5f, 0f, 0f};
        font.colors[6] = new float[]{0f, 0f, .5f};
        font.colors[7] = new float[]{.5f, 0f, .5f};
        font.colors[8] = new float[]{0f, .3f, .3f};
        return register(font, 265);
    }

    private static int register(Font font, int count) {
        int base = next;
        next += count;
        font.base = base;
        font.count = count;
        fonts.put(base, font);
        return base;
    }

    private static Font font(int id) {
        Map.Entry<Integer, Font> entry = fonts.floorEntry(id);
        if (entry == null || id >= entry.getKey() + entry.getValue().count)
            throw new IllegalArgumentException("Unknown Macros font command: " + id);
        return entry.getValue();
    }

    // The text editor updates its normal text color with this old list API.
    public static void begin(int id, int mode) {
        if (editing != 0 || mode != 4864 || id - font(id).base != 259)
            throw new IllegalArgumentException("Unexpected Macros color command");
        editing = id;
    }

    public static void end() { editing = 0; }

    public static void color3f(float r, float g, float b) {
        if (editing != 0) font(editing).colors[3] = new float[]{r, g, b};
        else bfl.c(r, g, b);
    }

    public static void draw(IntBuffer commands) {
        for (int i = commands.position(); i < commands.limit(); i++) {
            int id = commands.get(i);
            Font font = font(id);
            int offset = id - font.base;
            if (offset < 256) {
                if (!font.editor) { bfl.l(); bfl.b(770, 771); }
                glyph(offset);
                GL.glTranslatef(font.widths[offset], 0f, 0f);
                if (!font.editor) bfl.k();
            } else if (font.editor && offset == 256) {
                bfl.c(.35f, .35f, .35f, 1f);
                bfl.x();
                bfd vertices = bfx.a().c();
                vertices.a(7, bms.e);
                vertices.b(-1.0, 9.0, 0.0).d();
                vertices.b((double) font.widths[0], 9.0, 0.0).d();
                vertices.b((double) font.widths[0], -1.0, 0.0).d();
                vertices.b(-1.0, -1.0, 0.0).d();
                submit();
                bfl.w();
                bfl.c(.6f, .6f, .6f);
            } else {
                float[] rgb = font.colors[offset - 256];
                bfl.c(rgb[0], rgb[1], rgb[2]);
            }
        }
    }

    private static void glyph(int index) {
        float u = index % 16 * 8 / 128f;
        float v = index / 16 * 8 / 128f;
        float edge = 7.99f;
        float step = edge / 128f;
        bfd vertices = bfx.a().c();
        vertices.a(7, bms.g);
        vertices.b(0.0, (double) edge, 0.0).a((double) u, (double) (v + step)).d();
        vertices.b((double) edge, (double) edge, 0.0).a((double) (u + step), (double) (v + step)).d();
        vertices.b((double) edge, 0.0, 0.0).a((double) (u + step), (double) v).d();
        vertices.b(0.0, 0.0, 0.0).a((double) u, (double) v).d();
        submit();
    }

    private static void submit() {
        submit(bfx.a());
    }

    public static void submit(Object tessellator) {
        if (CoreOptions.GFX && logicEnabled) Svg7Logic.draw((bfx) tessellator, logicOpcode);
        else if (CoreOptions.GFX) SteinTessCapture.draw(tessellator);
        else ((bfx) tessellator).b();
    }

    public static void logicEnable() {
        logicEnabled = true;
        if (!CoreOptions.GFX) bfl.u();
    }
    public static void logicDisable() {
        logicEnabled = false;
        if (!CoreOptions.GFX) bfl.v();
    }
    public static void logicOp(int opcode) {
        logicOpcode = opcode;
        if (!CoreOptions.GFX) bfl.f(opcode);
    }

    public static void rect(int left, int top, int right, int bottom, int color) {
        if (!CoreOptions.GFX || !logicEnabled) { avp.a(left, top, right, bottom, color); return; }
        bfl.c((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
        bfd vertices = bfx.a().c();
        vertices.a(7, bms.e);
        vertices.b((double) left, (double) bottom, 0.0).d();
        vertices.b((double) right, (double) bottom, 0.0).d();
        vertices.b((double) right, (double) top, 0.0).d();
        vertices.b((double) left, (double) top, 0.0).d();
        submit();
    }

    static float[] color() {
        Object color = field(null, "t");
        return new float[]{((Number) field(color, "a")).floatValue(), ((Number) field(color, "b")).floatValue(),
            ((Number) field(color, "c")).floatValue(), ((Number) field(color, "d")).floatValue()};
    }
    static int texture() { return integer(((Object[]) field(null, "p"))[integer(null, "o")], "b"); }

    public static void push(int mask) {
        if (mask != 1048575) throw new IllegalArgumentException("Unexpected attribute mask: " + mask);
        states.push(new State());
    }

    public static void pop() { states.pop().restore(); }

    private static Object field(Object object, String name) {
        try {
            Field f = (object == null ? bfl.class : object.getClass()).getDeclaredField(name);
            f.setAccessible(true);
            return f.get(object);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Macros render state changed", e); }
    }

    private static int integer(Object object, String name) { return ((Number) field(object, name)).intValue(); }
    private static boolean enabled(Object state) { return (Boolean) field(field(state, "a"), "b"); }

    private static final class Font {
        final int[] widths;
        final boolean editor;
        final float[][] colors = new float[32][];
        int base, count;
        Font(int[] widths, boolean editor) { this.widths = widths; this.editor = editor; }
    }

    /** Restores the state touched by the overlay's stop button, including caches. */
    private static final class State {
        final boolean blend, logic, textureEnabled;
        final int src, dst, srcAlpha, dstAlpha, opcode, unit, texture;
        final float r, g, b, a;
        final boolean emulatedLogic = logicEnabled;
        final int emulatedOpcode = logicOpcode;
        State() {
            Object blendState = field(null, "e");
            blend = enabled(blendState);
            src = integer(blendState, "b"); dst = integer(blendState, "c");
            srcAlpha = integer(blendState, "d"); dstAlpha = integer(blendState, "e");
            Object logicState = field(null, "j");
            logic = enabled(logicState); opcode = integer(logicState, "b");
            unit = integer(null, "o");
            Object textureState = ((Object[]) field(null, "p"))[unit];
            textureEnabled = enabled(textureState); texture = integer(textureState, "b");
            Object color = field(null, "t");
            r = ((Number) field(color, "a")).floatValue(); g = ((Number) field(color, "b")).floatValue();
            b = ((Number) field(color, "c")).floatValue(); a = ((Number) field(color, "d")).floatValue();
        }
        void restore() {
            logicEnabled = emulatedLogic; logicOpcode = emulatedOpcode;
            bfl.g(33984 + unit); bfl.i(texture);
            if (textureEnabled) bfl.w(); else bfl.x();
            bfl.a(src, dst, srcAlpha, dstAlpha);
            if (blend) bfl.l(); else bfl.k();
            bfl.f(opcode);
            if (logic) bfl.u(); else bfl.v();
            bfl.c(r, g, b, a);
        }
    }
}
