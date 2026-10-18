import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import dev.xavier.stein.bridge.engine.GLMatrix;
import dev.xavier.stein.gfx.SteinEntityDeferred;
import dev.xavier.stein.gfx.SteinLazyGl;
import dev.xavier.stein.gfx.gl.SteinCoreGl;
import dev.xavier.stein.gfx.gl.SteinGlFrame;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

/** Core-profile draw for the original XOR cursor and inverted stop icon. */
final class Svg7Logic {
    private static int program, vao, vbo, model, projection, textured;
    private Svg7Logic() {}

    static void draw(bfx tessellator, int opcode) {
        bfd vertices = tessellator.c();
        bmu format = vertices.g();
        boolean uv = format.equals(bms.g) || format.equals(bms.i);
        boolean color = format.equals(bms.f) || format.equals(bms.i);
        if (!format.equals(bms.e) && !format.equals(bms.g) && !format.equals(bms.f) && !format.equals(bms.i))
            throw new IllegalArgumentException("Unexpected logic vertex format: " + format);
        int count = vertices.h(), mode = vertices.i();
        if (mode != 7 || count % 4 != 0) throw new IllegalArgumentException("Expected logic quads");
        float[] rgba = Svg7Graphics.color();
        int stride = 12 + (uv ? 8 : 0) + (color ? 4 : 0);
        ByteBuffer source = vertices.f().duplicate().order(ByteOrder.nativeOrder());
        FloatBuffer data = ByteBuffer.allocateDirect(count / 4 * 6 * 9 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        int[] corners = {0, 1, 2, 0, 2, 3};
        for (int quad = 0; quad < count; quad += 4) for (int corner : corners) {
            int offset = (quad + corner) * stride;
            data.put(source.getFloat(offset)).put(source.getFloat(offset + 4)).put(source.getFloat(offset + 8));
            data.put(uv ? source.getFloat(offset + 12) : 0f).put(uv ? source.getFloat(offset + 16) : 0f);
            for (int channel = 0; channel < 4; channel++)
                data.put(color ? (source.get(offset + stride - 4 + channel) & 255) / 255f : rgba[channel]);
        }
        data.flip();
        vertices.e(); vertices.b();
        SteinEntityDeferred.barrier();
        SteinLazyGl.sync();
        SteinGlFrame.sync("Macros color logic");
        int oldProgram = GL11C.glGetInteger(35725), oldVao = GL11C.glGetInteger(34229);
        int oldBuffer = GL11C.glGetInteger(34964), oldActive = GL11C.glGetInteger(34016);
        boolean oldLogic = GL11C.glIsEnabled(3058), oldBlend = GL11C.glIsEnabled(3042);
        int oldOpcode = GL11C.glGetInteger(3056);
        GL13C.glActiveTexture(33984);
        int oldTexture = GL11C.glGetInteger(32873);
        try {
            if (program == 0) init();
            GL20C.glUseProgram(program);
            GL20C.glUniformMatrix4fv(model, false, GLMatrix.current());
            GL20C.glUniformMatrix4fv(projection, false, SteinCoreGl.projection());
            GL20C.glUniform1i(textured, uv ? 1 : 0);
            if (uv) GL11C.glBindTexture(3553, Svg7Graphics.texture());
            GL30C.glBindVertexArray(vao);
            GL15C.glBindBuffer(34962, vbo);
            GL15C.glBufferData(34962, data, 35040);
            GL11C.glDisable(3042);
            GL11C.glEnable(3058);
            GL11C.glLogicOp(opcode);
            GL11C.glDrawArrays(4, 0, count / 4 * 6);
        } finally {
            GL11C.glLogicOp(oldOpcode);
            if (oldLogic) GL11C.glEnable(3058); else GL11C.glDisable(3058);
            if (oldBlend) GL11C.glEnable(3042); else GL11C.glDisable(3042);
            GL11C.glBindTexture(3553, oldTexture);
            GL13C.glActiveTexture(oldActive);
            GL30C.glBindVertexArray(oldVao);
            GL15C.glBindBuffer(34962, oldBuffer);
            GL20C.glUseProgram(oldProgram);
        }
    }

    private static void init() {
        int vertex = shader(35633, "#version 330 core\nlayout(location=0) in vec3 pos; layout(location=1) in vec2 uv; layout(location=2) in vec4 color; uniform mat4 model; uniform mat4 projection; out vec2 tex; out vec4 tint; void main(){gl_Position=projection*model*vec4(pos,1);tex=uv;tint=color;}");
        int fragment = shader(35632, "#version 330 core\nin vec2 tex; in vec4 tint; uniform sampler2D image; uniform int textured; out vec4 result; void main(){result=tint;if(textured!=0){result*=texture(image,tex);if(result.a<0.1)discard;}}");
        int linked = GL20C.glCreateProgram();
        GL20C.glAttachShader(linked, vertex); GL20C.glAttachShader(linked, fragment); GL20C.glLinkProgram(linked);
        GL20C.glDeleteShader(vertex); GL20C.glDeleteShader(fragment);
        if (GL20C.glGetProgrami(linked, 35714) == 0) throw new IllegalStateException(GL20C.glGetProgramInfoLog(linked));
        program = linked;
        model = GL20C.glGetUniformLocation(program, "model"); projection = GL20C.glGetUniformLocation(program, "projection");
        textured = GL20C.glGetUniformLocation(program, "textured");
        GL20C.glUseProgram(program); GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "image"), 0);
        vao = GL30C.glGenVertexArrays(); vbo = GL15C.glGenBuffers();
        GL30C.glBindVertexArray(vao); GL15C.glBindBuffer(34962, vbo);
        GL20C.glEnableVertexAttribArray(0); GL20C.glVertexAttribPointer(0, 3, 5126, false, 36, 0L);
        GL20C.glEnableVertexAttribArray(1); GL20C.glVertexAttribPointer(1, 2, 5126, false, 36, 12L);
        GL20C.glEnableVertexAttribArray(2); GL20C.glVertexAttribPointer(2, 4, 5126, false, 36, 20L);
    }

    private static int shader(int type, String source) {
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, source); GL20C.glCompileShader(shader);
        if (GL20C.glGetShaderi(shader, 35713) == 0) throw new IllegalStateException(GL20C.glGetShaderInfoLog(shader));
        return shader;
    }
}
