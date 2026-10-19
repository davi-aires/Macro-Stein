package com.litemod.test;

import dev.xavier.stein.loader.api.SteinMod;
import dev.xavier.stein.bridge.core.Win;
import dev.xavier.stein.gfx.gl.SteinGlFrame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;

/** Test-only mod: opens Macros screens without touching an existing world. */
public final class MigrationProbe implements SteinMod {
    private int ticks;
    private boolean bindingShot, editorShot, overlayShot, controlsShot, itemPromptShot;
    public void onTickEnd() {
        ticks++;
        if (ticks != 10 && ticks != 30 && ticks != 40 && ticks != 100 && ticks != 120 && ticks != 160 && ticks != 180 && ticks != 220) return;
        try {
            Class<?> mcClass = Class.forName("ave");
            Object mc = mcClass.getMethod("A").invoke(null);
            if (ticks == 10) {
                Object core = Class.forName("net.eq2online.macros.core.MacroModCore").getMethod("getInstance").invoke(null);
                core.getClass().getMethod("enableScrollToButtons").invoke(core);
                Object screen = Class.forName("ayj").getConstructor(Class.forName("axu"), Class.forName("avh"))
                    .newInstance(null, mcClass.getField("t").get(mc));
                mcClass.getMethod("a", Class.forName("axu")).invoke(mc, screen);
            } else if (ticks == 30) {
                Object screen = mcClass.getField("m").get(mc);
                Object list = privateField("keyBindingList", screen);
                Object[] entries = (Object[]) privateField("keyBindingEntries", list);
                Object buttonEntry = null;
                boolean combo = false, override = false;
                for (Object entry : entries) {
                    String name = entry.getClass().getSimpleName();
                    if (name.equals("BindingButtonEntry")) buttonEntry = entry;
                    if (name.equals("BindScreenOptionEntry")) combo = true;
                    if (name.equals("OverrideKeyEntry")) override = true;
                }
                if (buttonEntry == null || !combo || !override) throw new AssertionError("Macros settings entries missing from Controls");
                Field field = buttonEntry.getClass().getDeclaredField("bindingButton"); field.setAccessible(true);
                Object button = field.get(buttonEntry);
                int x = button.getClass().getField("h").getInt(button) + 3;
                int y = button.getClass().getField("i").getInt(button) + 3;
                Object clicked = buttonEntry.getClass().getMethod("a", int.class, int.class, int.class, int.class, int.class, int.class)
                    .invoke(buttonEntry, 0, x, y, 0, 3, 3);
                if (!Boolean.TRUE.equals(clicked)) throw new AssertionError("Macro Settings button did not accept click");
            } else if (ticks == 40) {
                Object screen = mcClass.getField("m").get(mc);
                if (!screen.getClass().getSimpleName().equals("GuiMacroBind")) throw new AssertionError("Button did not open Macro Settings");
                System.out.println("[migration-test] Controls button opened Macros binding screen");
            } else if (ticks == 100) {
                File script = new File("migration-render-test.txt");
                Files.write(script.toPath(), ("// SVG7 syntax, colors and selection\nSET(#svg7_migration,42);\nIF(#svg7_migration = 42);\n  SET(&message,\"Macros + Stein GFX\");\nENDIF;\n").getBytes(StandardCharsets.UTF_8));
                Class<?> context = Class.forName("net.eq2online.macros.scripting.parser.ScriptContext");
                Object main = context.getField("MAIN").get(null);
                Object parent = mcClass.getField("m").get(mc);
                Object screen = Class.forName("net.eq2online.macros.gui.screens.GuiEditTextFile")
                    .getConstructor(Class.forName("net.eq2online.macros.gui.shared.GuiScreenEx"), File.class, context)
                    .newInstance(parent, script, main);
                mcClass.getMethod("a", Class.forName("axu")).invoke(mc, screen);
                System.out.println("[migration-test] Script editor opened");
                Object manager = Class.forName("net.eq2online.macros.core.MacroModCore").getMethod("getMacroManager").invoke(null);
                Object template = manager.getClass().getMethod("createLooseTemplate", String.class, String.class)
                    .invoke(manager, "$${SET(#svg7_migration,42);}$$", "svg7-migration-test");
                manager.getClass().getMethod("playMacro", template.getClass(), boolean.class, context,
                    Class.forName("net.eq2online.macros.scripting.api.IVariableProvider"), boolean.class)
                    .invoke(manager, template, false, main, null, true);
                Object value = template.getClass().getMethod("getCounter", String.class).invoke(template, "svg7_migration");
                if (!Integer.valueOf(42).equals(value)) throw new AssertionError("Script did not execute SET: " + value);
                System.out.println("[migration-test] Original macro interpreter executed SET successfully");
            } else if (ticks == 120) {
                Object screen = mcClass.getField("m").get(mc);
                Field field = Class.forName("net.eq2online.macros.gui.screens.GuiEditText").getDeclaredField("textEditor");
                field.setAccessible(true);
                Object editor = field.get(screen);
                editor.getClass().getMethod("keyTyped", char.class, int.class).invoke(editor, '\u0001', 30);
            } else if (ticks == 160) {
                Class<?> targetType = Class.forName("net.eq2online.macros.interfaces.IMacroParamTarget");
                final Object[] params = new Object[1];
                Object target = java.lang.reflect.Proxy.newProxyInstance(targetType.getClassLoader(), new Class<?>[]{targetType},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getTargetString")) return "$$i:d";
                        if (method.getName().equals("getIteration")) return 1;
                        if (method.getName().equals("getDisplayName")) return "Item prompt test";
                        if (method.getName().equals("getPromptMessage")) return "Select an item";
                        if (method.getName().equals("getNextParam") || method.getName().equals("hasRemainingParams"))
                            return params[0].getClass().getMethod(method.getName()).invoke(params[0]);
                        return null;
                    });
                params[0] = Class.forName("net.eq2online.macros.core.MacroParams").getConstructor(targetType).newInstance(target);
                params[0].getClass().getMethod("evaluateParams").invoke(params[0]);
                Object screen = Class.forName("net.eq2online.macros.gui.screens.GuiMacroParam").getConstructor(targetType).newInstance(target);
                mcClass.getMethod("a", Class.forName("axu")).invoke(mc, screen);
            } else if (ticks == 180) {
                Object screen = mcClass.getField("m").get(mc);
                if (screen == null || !screen.getClass().getSimpleName().equals("GuiMacroParam"))
                    throw new AssertionError("Item prompt did not open: " + screen);
                System.out.println("[migration-test] Item selection prompt opened using the Venda prompt syntax");
            } else {
                Object screen = mcClass.getField("m").get(mc);
                if (!screen.getClass().getName().equals("net.eq2online.macros.gui.screens.GuiMacroParam"))
                    throw new AssertionError("Unexpected screen: " + screen);
                if (!bindingShot || !editorShot || !overlayShot || !controlsShot || !itemPromptShot) throw new AssertionError("Missing rendered screenshots");
                System.out.println("[migration-test] GFX migration checks complete");
            }
        } catch (Throwable failure) {
            System.out.println("[migration-test] FAILED");
            failure.printStackTrace();
        }
    }

    public void afterDrawScreen(Object screen, int mouseX, int mouseY, float partialTick) {
        try {
            if (ticks >= 25 && ticks < 30 && !controlsShot) {
                screenshot("macros-controls.png"); controlsShot = true;
            }
            if (ticks >= 70 && ticks < 100 && !bindingShot) {
                screenshot("macros-bindings.png"); bindingShot = true;
            }
            if (ticks >= 85 && ticks < 100 && !overlayShot) {
                Class<?> mcClass = Class.forName("ave");
                Object mc = mcClass.getMethod("A").invoke(null);
                Object font = mcClass.getField("k").get(mc);
                Class.forName("net.eq2online.macros.gui.screens.GuiMacroModOverlay")
                    .getMethod("drawStatusLine", Class.forName("avn"), String.class, int.class, int.class,
                        int.class, int.class, boolean.class)
                    .invoke(null, font, "Macro stop button", 20, 30, 22, 31, true);
                screenshot("macros-overlay.png"); overlayShot = true;
                System.out.println("[migration-test] Overlay stop button with color inversion rendered");
            }
            if (ticks >= 190 && !itemPromptShot && screen.getClass().getSimpleName().equals("GuiMacroParam")) {
                Field listField = screen.getClass().getDeclaredField("itemListBox"); listField.setAccessible(true);
                Object list = listField.get(screen);
                int count = ((Number) list.getClass().getMethod("getItemCount").invoke(list)).intValue();
                if (count <= 0) throw new AssertionError("Item selector list is empty");
                screenshot("macros-item-prompt.png"); itemPromptShot = true;
                System.out.println("[migration-test] Item selector icons rendered without a cached itemRenderer");
            }
            if (ticks >= 140 && !editorShot) {
                screenshot("macros-editor.png"); editorShot = true;
            }
        } catch (Throwable failure) {
            System.out.println("[migration-test] FAILED"); failure.printStackTrace();
        }
    }

    private static Object privateField(String name, Object instance) throws Exception {
        Object accessor = Class.forName("net.eq2online.macros.compatibility.PrivateFields").getField(name).get(null);
        return accessor.getClass().getMethod("get", Object.class).invoke(accessor, instance);
    }

    private static void screenshot(String name) throws Exception {
        SteinGlFrame.sync("macros migration screenshot");
        int width = Win.width, height = Win.height;
        ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, 6408, 5121, pixels);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int offset = (y * width + x) * 4;
            int color = 0xff000000 | ((pixels.get(offset) & 255) << 16) |
                ((pixels.get(offset + 1) & 255) << 8) | (pixels.get(offset + 2) & 255);
            image.setRGB(x, height - y - 1, color);
        }
        ImageIO.write(image, "png", new File(name));
        System.out.println("[migration-test] Saved " + name);
    }
}
