import java.lang.reflect.Field;

/** Replaces the field setter removed from the new loader's compatibility API. */
public final class Svg7Reflect {
    private Svg7Reflect() {}

    public static void setField(Object type, Object instance, Object name, Object value) throws NoSuchFieldException {
        Field field = ((Class<?>) type).getDeclaredField((String) name);
        field.setAccessible(true);
        try {
            field.set(instance, value);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot update Macros field: " + name, failure);
        }
    }
}
