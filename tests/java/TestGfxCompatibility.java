import java.io.File;
import java.lang.reflect.Method;

/** Uses the actual loader's compatibility check rather than duplicating it. */
public final class TestGfxCompatibility {
    public static void main(String[] args) throws Exception {
        Class<?> guard = Class.forName("dev.xavier.stein.loader.LegacyGl");
        Method check = guard.getDeclaredMethod("firstMissing", File.class);
        check.setAccessible(true);
        Object missing = check.invoke(null, new File(args[0]));
        if (missing != null) throw new AssertionError("Stein GFX rejects: " + missing);
        System.out.println("PASS: actual Stein Loader accepts all OpenGL calls with GFX enabled");
    }
}
