package es.optocom.jovp;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.definitions.ViewMode;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Observer;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unitary tests for the psychophysics engine
 *
 * @since 0.0.1
 */
public class PsychoEngineTest {

    /**
     * Unitary tests for the psychophysics engine
     *
     * @since 0.0.1
     */
    public PsychoEngineTest() {
    }

    /**
     * Gets information about the system and attached devices
     *
     * @since 0.0.1
     */
    @Test
    public void initializeEngine() {
        // Init psychoEngine and show some general info
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
        Window window = psychoEngine.getWindow();
        Monitor monitor = window.getMonitor();
        // Check field of view
        int monitorWidthMM = 500;
        int monitorHeightMM = (int) (monitorWidthMM / monitor.getAspect());
        double theta = 90; // field of view
        monitor.setPhysicalSize(monitorWidthMM, monitorHeightMM);
        psychoEngine.setSize(monitor.getWidth(), monitor.getHeight());
        psychoEngine.setDistance(monitorWidthMM / (2 * Math.tan(Math.toRadians(theta) / 2.0)));
        float[] fov = psychoEngine.getFieldOfView();
        assertEquals(90, Math.round(1e3 * fov[0]) / 1e3);
        assertEquals(ViewMode.MONO, psychoEngine.getViewMode());
        psychoEngine.setViewMode(ViewMode.STEREO);
        assertEquals(ViewMode.STEREO, psychoEngine.getViewMode());
        psychoEngine.cleanup();
    }

    /**
     * Get physical device
     *
     * @since 0.0.1
     */
    @Test
    public void getPhysicalDevices() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
        List<VkPhysicalDevice> physicalDevices = psychoEngine.getPhysicalDevices();
        for (VkPhysicalDevice vkPhysicalDevice : physicalDevices)
            System.out.println(vkPhysicalDevice.toString());
        psychoEngine.cleanup();
    }

    /**
     * Get window, check window position, and set window monitor
     *
     * @since 0.0.1
     */
    @Test
    public void getWindow() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(new Timer()));
        Window window = psychoEngine.getWindow();
        // window position
        int[] position = psychoEngine.getPosition();
        assertArrayEquals(position, window.getPosition());
        System.out.println("Window position is: " + Arrays.toString(position));
        // window monitor
        psychoEngine.setMonitor(0);
        psychoEngine.cleanup();
    }

    /**
     * Sets background for single-screen mode, and for split-screen mode
     *
     * @since 0.0.1
     */
    @Test
    public void runPsychoEngine() {
        Timer timer = new Timer();
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(timer));
        new Thread(() -> {
            while (timer.getElapsedTime() == -1)
                Thread.onSpinWait(); // wait for the beginning of the psychophysics experience
            while (timer.getElapsedTime() < 1000)
                Thread.onSpinWait(); // close window after 1 second
            psychoEngine.finish();
        }).start();
        psychoEngine.start("mouse", Paradigm.CLICKER);
        System.out.println("Engine was running for " + timer.getElapsedTime() / 1000 + " seconds");
        psychoEngine.cleanup();
    }

    /**
     * 
     * View virtual world
     *
     * @since 0.0.1
     */
    @Test
    public void viewVirtualWorld() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicWorld());
        psychoEngine.setPhysicalSize(621, 341);
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Stereo View
     *
     * @since 0.0.1
     */
    @Test
    public void stereoTest() {
        PsychoEngine psychoEngine = new PsychoEngine(new StereoLogic());
        psychoEngine.setViewMode(ViewMode.STEREO);
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /** Psychophysics logic class */
    static class Logic implements PsychoLogic {

        /** Logic timer */
        Timer timer;

        /** Init with timer */
        Logic(Timer timer) {
            this.timer = timer;
        }

        @Override
        public void init(PsychoEngine psychoEngine) {
            timer.start();
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }
    }

    // Psychophysics logic class
    static class StereoLogic implements PsychoLogic {

        double[] fixationColor = new double[] { 0, 1, 0, 1 };
        double[] backgroundColor = new double[] { 0, 0, 1, 1 };

        Item background, fixation, stimulus1, stimulus2, stimulus3;
        Timer timer = new Timer();
        Timer timerFps = new Timer();
        int fps = 0;
        Text text;
        int refreshTime = 1000;

        @Override
        public void init(PsychoEngine psychoEngine) {
            background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor)); // background
            background.position(0, 0);
            background.distance(90);
            view.add(background);
            fixation = new Item(new Model(ModelType.MALTESE), new Texture(fixationColor)); // fixation
            fixation.size(2);
            view.add(fixation);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus1.position(-3, -3);
            stimulus1.size(4.5, 4.5);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            stimulus1.contrast(0.75);
            view.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus2.frequency(0, 2);
            stimulus2.position(3, 2);
            stimulus2.size(3, 1.5);
            stimulus2.eye(Eye.LEFT);
            stimulus2.contrast(0.25);
            view.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
            stimulus3.eye(Eye.RIGHT);
            stimulus3.frequency(0, 2);
            stimulus3.position(3, -2);
            stimulus3.size(2, 2);
            stimulus3.contrast(0.5);
            view.add(stimulus3);
            // Add title
            Text title = new Text();
            title.setText("Stereoscopic view");
            title.eye(Eye.LEFT);
            title.size(0.75);
            title.position(-5, 5);
            view.add(title);
            // Add text to show FPS
            text = new Text();
            text.setText("Refresh rate:");
            text.eye(Eye.LEFT);
            text.size(0.6);
            text.position(-5, 4);
            view.add(text);
            // Start timer
            timer.start();
            timerFps.start();
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE)
                System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            double time = timer.getElapsedTime();
            stimulus1.contrast(Math.sin(time / 1000.0) / 2 + 0.5);
            stimulus3.contrast(Math.sin(time / 200.0) / 2 + 0.5);
            stimulus1.frequency(Math.sin(time / 250.0), 0.5);
            stimulus1.rotation(time / 10.0);
            stimulus2.rotation(-time / 20.0);
            stimulus2.texRotation(time / 5.0);
            if (timerFps.getElapsedTime() <= refreshTime)
                fps++;
            else { // restart the timer every second
                timerFps.start();
                text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }
    }

    // Psychophysics logic to show a simple triangle
    static class LogicWorld implements PsychoLogic {

        Vector3f eye = new Vector3f(0, 0, 0);
        Vector3f center = new Vector3f(0, 0, 1);
        Vector3f up = new Vector3f(0, 1, 0);

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item leftEye = new Item(new Model(
                    "/Users/imarinfr/06.optocom/03.software/jovp/src/test/resources/es/optocom/jovp/models/eyeball.obj"),
                    new Texture(new double[] { 1, 1, 1, 1 }));
            Item rightEye = new Item(new Model(
                    "/Users/imarinfr/06.optocom/03.software/jovp/src/test/resources/es/optocom/jovp/models/eyeball.obj"),
                    new Texture(new double[] { 1, 1, 1, 1 }));
            Item item = new Item(new Model(ModelType.TRIANGLE), new Texture(new double[] { 1, 1, 1, 1 }));
            view.add(leftEye);
            view.add(rightEye);
            view.add(item);
            leftEye.distance(0.2);
            leftEye.position(-4, 0);
            leftEye.size(0.5);
            rightEye.distance(0.2);
            rightEye.position(10, 10);
            rightEye.size(0.5);
            item.distance(Observer.FAR);
            item.position(15, 15);
            item.size(2);
            item.rotation(0);
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE)
                System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
            psychoEngine.setView(eye, center, up);
        }
    }

}
