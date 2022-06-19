package es.optocom.jovp.unit;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.Window;
import es.optocom.jovp.engine.structures.Command;
import es.optocom.jovp.engine.structures.Eye;
import es.optocom.jovp.engine.structures.Paradigm;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PsychoEngineTest
 *
 * <ul>
 * <li>PsychoEngineTest test</li>
 * Unitary tests for the psychophysics engine
 * </ul>
 *
 * @since 0.0.1
 */
public class PsychoEngineTest {

    /**
     * Gets information about the system and attached devices
     *
     * @since 0.0.1
     */
    @Test
    public void initializeEngine() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500);
        System.out.println(psychoEngine);
        System.out.println(psychoEngine.getWindow().getMonitorManager());
        // Set eye
        assertEquals(Eye.BOTH, psychoEngine.getEye());
        psychoEngine.setEye(Eye.LEFT);
        assertEquals(Eye.LEFT, psychoEngine.getEye());
        psychoEngine.setEye(Eye.RIGHT);
        assertEquals(Eye.RIGHT, psychoEngine.getEye());
        psychoEngine.setEye(Eye.BOTH);
        assertEquals(Eye.BOTH, psychoEngine.getEye());
        psychoEngine.setEye(Eye.STEREO);
        assertEquals(Eye.STEREO, psychoEngine.getEye());
        // Set paradigm
        psychoEngine.setParadigm(Paradigm.CLICKER);
        assertEquals(Paradigm.CLICKER, psychoEngine.getParadigm());
        psychoEngine.cleanup();
    }

    /**
     * Tests for updating distance and the fixation point, and for retrieving the field of view of the window
     * @since 0.0.1
     */
    @Test
    public void fieldOfView() {
        // At a meter distance, check for full-screen window on a monitor with the same 1 mm of pixel size
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 1);
        int width = psychoEngine.getWindow().getWidth();
        int height = psychoEngine.getWindow().getHeight();
        int distance = width / 2;
        psychoEngine.setDistance(distance);
        float[] expectedFieldOfView = new float[] {
                (float) (2 * Math.toDegrees(Math.atan((double) width / 2 / distance))),
                (float) (2 * Math.toDegrees(Math.atan((double) height / 2 / distance)))
        };
        psychoEngine.getWindow().getMonitor().setPhysicalSize(width, height);
        psychoEngine.getWindow().getMonitor().setSize(width, height);
        psychoEngine.setWindowSize(width, height);
        assertEquals(distance, psychoEngine.getDistance());
        assertArrayEquals(expectedFieldOfView, psychoEngine.getFieldOfView());
        psychoEngine.cleanup();
    }

    /**
     * Get physical device
     * @since 0.0.1
     */
    @Test
    public void getPhysicalDevices() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500);
        List<VkPhysicalDevice> physicalDevices = psychoEngine.getPhysicalDevices();
        for (VkPhysicalDevice vkPhysicalDevice: physicalDevices)
            System.out.println(vkPhysicalDevice.toString());
        psychoEngine.cleanup();
    }

    /**
     * Get window, check window position, and set window monitor
     * @since 0.0.1
     */
    @Test
    public void getWindow() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500);
        Window window = psychoEngine.getWindow();
        // window position
        int[] position = psychoEngine.getWindowPosition();
        assertArrayEquals(position, window.getPosition());
        System.out.println("Window position is: " + Arrays.toString(position));
        // window monitor
        psychoEngine.setWindowMonitor(0);
        psychoEngine.cleanup();
    }

    /**
     * Sets background for single-screen mode, and for split-screen mode
     * @since 0.0.1
     */
    @Test
    public void runPsychoEngine() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500);
        new Thread(() -> {
            double time = 0;
            while (time == 0) { // wait for the beginning of the psychophysics experience
                time = psychoEngine.getStartTime();
                Thread.onSpinWait();
            }
            time = 0;
            while (time < 2000) { // close window after 1 second
                time = psychoEngine.getElapsedTime();
                Thread.onSpinWait();
            }
            psychoEngine.finish();
        }).start();
        psychoEngine.start();
        System.out.println("Engine was running for " + psychoEngine.getElapsedTime() / 1000 + " seconds");
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic{

        @Override
        public void init() {}

        @Override
        public void input(Command command, double time) {}

        @Override
        public void update() {}

    }

}
