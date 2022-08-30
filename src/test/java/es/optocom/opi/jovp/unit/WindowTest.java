package es.optocom.opi.jovp.unit;

import es.optocom.opi.jovp.engine.PsychoEngine;
import es.optocom.opi.jovp.engine.PsychoLogic;
import es.optocom.opi.jovp.engine.Window;
import es.optocom.opi.jovp.engine.structures.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * Unitary tests for window management
 *
 * @since 0.0.1
 */
public class WindowTest {

    /**
     *
     * Unitary tests for window management
     *
     * @since 0.0.1
     */
    public WindowTest() {}

    /**
     *
     * Tests for creating, showing, hiding, resizing, and hiding windows
     *
     * @since 0.0.1
     */
    @Test
    public void showWindowedAndFullScreen() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
        Window window = psychoEngine.getWindow();
        window.show();
        window.hide();
        psychoEngine.start();
        psychoEngine.setFullScreen();
        psychoEngine.setWindowed();
        psychoEngine.cleanup();
    }

    /**
     *
     * Tests for updating distance and the fixation point, and for retrieving the field of view of the window
     *
     * @since 0.0.1
     */
    @Test
    public void changeWindowPositionAndSize() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
        Window window = psychoEngine.getWindow();
        // position is relative to monitor workspace in the virtual desktop
        psychoEngine.setWindowPosition(10, 10);
        assertArrayEquals(new int[] {10, 10}, window.getPosition());
        psychoEngine.setWindowSize(500, 200);
        assertEquals(500, window.getWidth());
        assertEquals(200, window.getHeight());
        window.show();
        psychoEngine.setWindowSize(1000, 800);
        psychoEngine.setWindowPosition(10, 10);
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {}

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {}

    }

}
