package com.litemod;

import dev.xavier.stein.bridge.core.Win;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/** Adapts the Macros LWJGL2 input buffers to the SVG 7 event queues. */
public final class Svg7Input {
    public static final ByteBuffer keyReadBuffer = ByteBuffer.allocate(50 * 18);
    public static final ByteBuffer keyDownBuffer = ByteBuffer.allocate(256);
    public static final ByteBuffer mouseReadBuffer = ByteBuffer.allocate(50 * 22);
    public static final ByteBuffer mouseDownBuffer = ByteBuffer.allocate(16);
    private static int[] exportStop = new int[0];

    private Svg7Input() {}

    public static void setLegacyExportStop(int[] keys) {
        exportStop = keys == null ? new int[0] : keys.clone();
    }

    public static void exportBuffers() {
        keyReadBuffer.clear();
        for (int i = 0; i < 50 && !Win.keyEvents.isEmpty(); i++) {
            Win.KeyEvent event = Win.keyEvents.pollFirst();
            keyReadBuffer.putInt(event.key).put((byte) (event.state ? 1 : 0))
                .putInt(event.character).putLong(0L).put((byte) (event.repeat ? 1 : 0));
            if (event.state && stops(event.key)) break;
        }
        keyReadBuffer.flip();
        for (int i = 0; i < 256; i++) keyDownBuffer.put(i, (byte) (Win.keysDown[i] ? 1 : 0));
        mouseReadBuffer.clear();
        for (int i = 0; i < 50 && !Win.mouseEvents.isEmpty(); i++) {
            Win.MouseEvent event = Win.mouseEvents.pollFirst();
            mouseReadBuffer.put((byte) event.button).put((byte) (event.state ? 1 : 0))
                .putInt(event.x).putInt(event.y).putInt(event.dwheel).putLong(0L);
        }
        mouseReadBuffer.flip();
        for (int i = 0; i < 16; i++) mouseDownBuffer.put(i, (byte) (Win.mouseButtons[i] ? 1 : 0));
    }

    private static boolean stops(int key) {
        for (int stop : exportStop) if (stop != 0 && stop == key) return true;
        return false;
    }

    public static void importBuffers() {
        ArrayDeque<Win.KeyEvent> keys = new ArrayDeque<Win.KeyEvent>();
        while (keyReadBuffer.remaining() >= 18) {
            Win.KeyEvent event = new Win.KeyEvent();
            event.key = keyReadBuffer.getInt();
            event.state = keyReadBuffer.get() != 0;
            event.character = (char) keyReadBuffer.getInt();
            keyReadBuffer.getLong();
            event.repeat = keyReadBuffer.get() != 0;
            keys.addLast(event);
        }
        while (!keys.isEmpty()) Win.keyEvents.addFirst(keys.pollLast());
        for (int i = 0; i < 256; i++) Win.keysDown[i] = keyDownBuffer.get(i) != 0;
        ArrayDeque<Win.MouseEvent> mouse = new ArrayDeque<Win.MouseEvent>();
        while (mouseReadBuffer.remaining() >= 22) {
            Win.MouseEvent event = new Win.MouseEvent();
            event.button = mouseReadBuffer.get();
            event.state = mouseReadBuffer.get() != 0;
            event.x = mouseReadBuffer.getInt();
            event.y = mouseReadBuffer.getInt();
            event.dwheel = mouseReadBuffer.getInt();
            mouseReadBuffer.getLong();
            mouse.addLast(event);
        }
        while (!mouse.isEmpty()) Win.mouseEvents.addFirst(mouse.pollLast());
        for (int i = 0; i < 16; i++) Win.mouseButtons[i] = mouseDownBuffer.get(i) != 0;
    }
}
