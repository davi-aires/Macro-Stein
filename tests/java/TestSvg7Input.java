import com.litemod.Svg7Input;
import dev.xavier.stein.bridge.core.Win;

public final class TestSvg7Input {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Win.KeyEvent key(int code, char character) {
        Win.KeyEvent e = new Win.KeyEvent(); e.key = code; e.character = character; e.state = true; return e;
    }
    public static void main(String[] args) {
        Win.resetInput();
        Win.keyEvents.add(key(30, 'a'));
        Win.keyEvents.add(key(20, 't'));
        Win.keyEvents.add(key(48, 'b'));
        Win.keysDown[30] = true;
        Svg7Input.setLegacyExportStop(new int[]{20});
        Svg7Input.exportBuffers();
        check(Svg7Input.keyReadBuffer.remaining() == 36, "Stop after the chat-opening key");
        check(Win.keyEvents.peek().key == 48, "Preserve events after chat opens");
        Svg7Input.importBuffers();
        check(Win.keyEvents.poll().key == 30 && Win.keyEvents.poll().key == 20 && Win.keyEvents.poll().key == 48, "Preserve key event order");
        Win.keyEvents.add(key(30, 'a'));
        Win.keyEvents.add(key(48, 'b'));
        Win.MouseEvent mouse = new Win.MouseEvent();
        mouse.button = 2; mouse.state = true; mouse.x = 123; mouse.y = 456; mouse.dwheel = -120;
        Win.mouseEvents.add(mouse);
        Win.mouseButtons[2] = true;
        Svg7Input.exportBuffers();
        Svg7Input.keyReadBuffer.position(18);
        Svg7Input.keyDownBuffer.put(30, (byte) 0);
        Svg7Input.importBuffers();
        check(Win.keyEvents.size() == 1 && Win.keyEvents.poll().character == 'b', "Consume only the macro key");
        check(!Win.keysDown[30], "Apply key state changes");
        mouse = Win.mouseEvents.poll();
        check(mouse.button == 2 && mouse.state && mouse.x == 123 && mouse.y == 456 && mouse.dwheel == -120, "Round-trip mouse buttons, coordinates and scrolling");
        check(Win.mouseButtons[2], "Preserve held mouse buttons");
        for (int i = 0; i < 60; i++) Win.keyEvents.add(key(30, 'a'));
        Svg7Input.exportBuffers();
        check(Svg7Input.keyReadBuffer.remaining() == 900 && Win.keyEvents.size() == 10, "Bound event batches without dropping overflow");
        Svg7Input.importBuffers();
        check(Win.keyEvents.size() == 60, "Restore complete event queue");
        System.out.println("PASS: chat boundaries, event order, consumed keys, held states, mouse and overflow");
    }
}
