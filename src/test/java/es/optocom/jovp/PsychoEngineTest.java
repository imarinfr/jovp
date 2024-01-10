package es.optocom.jovp;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.definitions.ViewMode;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Observer;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * Unitary tests for the psychophysics engine
 *
 * @since 0.0.1
 */
public class PsychoEngineTest {

    /**
     * 
     * Unitary tests for the psychophysics engine
     *
     * @since 0.0.1
     */
    public PsychoEngineTest() {
    }

    /**
     * 
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
     * 
     * Sets background for single-screen mode, and for split-screen mode
     *
     * @since 0.0.1
     */
    @Test
    public void runPsychoEngine() {
        Timer timer = new Timer();
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(timer));
        new Thread(() -> {
            while (timer.getElapsedTime() < 1000)
                try { Thread.sleep(200); } catch (InterruptedException e) { ; }
            psychoEngine.finish();
        }).start();
        psychoEngine.start("mouse", Paradigm.CLICKER);
        System.out.println("Engine was running for " + timer.getElapsedTime() / 1000 + " seconds");
        psychoEngine.cleanup();
    }

    /**
     * 
     * Render a triangle
     *
     * @since 0.0.1
     */
    @Test
    public void showTriangle() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicTriangle(), 300);
        psychoEngine.start("keypad", InputType.REPEAT, Paradigm.M9AFC);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Stereo View
     *
     * @since 0.0.1
     */
    @Test
    public void stereoMode() {
        PsychoEngine psychoEngine = new PsychoEngine(new StereoLogic());
        psychoEngine.setViewMode(ViewMode.STEREO);
        psychoEngine.start("mouse", Paradigm.CLICKER);
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
        PsychoEngine psychoEngine = new PsychoEngine(new WorldLogic(), 82.015f);
        float[] fov = psychoEngine.getFieldOfView();
        System.out.println("Field of view: " + fov[0] + " " + fov[1]);
        psychoEngine.start("keypad", InputType.REPEAT, Paradigm.M9AFC);
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

        public void init(PsychoEngine psychoEngine) {
            timer.start();
        }

        public void input(PsychoEngine psychoEngine, Command command) {
        }

        public void update(PsychoEngine psychoEngine) {
        }
    }

    // Psychophysics logic to show a simple triangle
    static class LogicTriangle implements PsychoLogic {

        public void init(PsychoEngine psychoEngine) {
            Text text = new Text(new double[] {0, 1, 0, 1});
            text.setText("Herro, prease");
            view.add(text);
            text.distance(Observer.ZFAR / 5);
            text.size(5);
            text.position(-25, 0);
            Item item = new Item(new Model(ModelType.TRIANGLE), new Texture(new double[] { 1, 1, 1, 1 }));
            view.add(item);
            item.distance(Observer.ZFAR / 2);
            item.size(5, 5);
            item.position(25, 10);
            item.rotation(0, 0, 45);
        }

        public void input(PsychoEngine psychoEngine, Command command) {
        }

        public void update(PsychoEngine psychoEngine) {
        }

    }
    // Psychophysics logic class
    static class StereoLogic implements PsychoLogic {

        double[] fixationColor = new double[] { 0, 1, 0, 1 };
        double[] backgroundColor = new double[] { 0, 0, 1, 1 };

        Item background, fixation, stimulus1, stimulus2, stimulus3;
        Timer timer = new Timer();
        Timer swapEyeTimer = new Timer();
        int swapEyeTime = 1500;
        Timer timerFps = new Timer();
        int fps = 0;
        Text title, text;
        int refreshTime = 1000;

        public void init(PsychoEngine psychoEngine) {
            background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor)); // background
            background.position(0, 0);
            background.distance(90);
            view.add(background);
            fixation = new Item(new Model(ModelType.MALTESE), new Texture(fixationColor)); // fixation
            fixation.size(2);
            fixation.distance(50);
            view.add(fixation);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus1.position(-3, -3);
            stimulus1.size(4.5, 4.5);
            stimulus1.distance(75);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            stimulus1.contrast(0.75);
            view.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus2.frequency(0, 2);
            stimulus2.position(3, 2);
            stimulus2.size(6, 3);
            stimulus2.distance(75);
            stimulus2.show(Eye.LEFT);
            stimulus2.contrast(0.25);
            view.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
            stimulus3.frequency(0, 2);
            stimulus3.position(3, -2);
            stimulus3.size(2, 2);
            stimulus3.distance(75);
            stimulus3.show(Eye.RIGHT);
            stimulus3.distance(75);
            stimulus3.contrast(0.5);
            view.add(stimulus3);
            // Add title
            title = new Text();
            title.setText("Stereoscopic view");
            title.show(Eye.LEFT);
            title.size(1.5);
            title.position(0, 0);
            title.distance(10);
            view.add(title);
            // Add text to show FPS
            text = new Text();
            text.setText("Refresh rate:");
            text.show(Eye.LEFT);
            text.size(1);
            text.position(-7.5, 6.5);
            text.distance(5);
            view.add(text);
            // Start timers
            timer.start();
            swapEyeTimer.start();
            timerFps.start();
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE) System.out.println(command);
            if (command == Command.YES){
                if(psychoEngine.getViewMode() == ViewMode.MONO) {
                    psychoEngine.setViewMode(ViewMode.STEREO);
                    title.setText("Stereoscopic view");
                } else {
                    psychoEngine.setViewMode(ViewMode.MONO);
                    title.setText("Monoscopic view");
                }
            }
        }

        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            double time = timer.getElapsedTime();
            if (swapEyeTimer.getElapsedTime() > swapEyeTime) {
                if (stimulus2.getEye() == Eye.LEFT)
                    stimulus2.show(Eye.RIGHT);
                else
                    stimulus2.show(Eye.LEFT);
                if (stimulus3.getEye() == Eye.RIGHT)
                    stimulus3.show(Eye.LEFT);
                else
                    stimulus3.show(Eye.RIGHT);
                swapEyeTimer.start();
            }
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
    static class WorldLogic implements PsychoLogic {

        private static final float STEP = 3;
        private boolean distortion = false;
        private boolean rotate = false;

        ArrayList<Item> eyePos = new ArrayList<Item>(2); 
        Item item;
        Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 0.5, 0.5, 0.5, 1 }));

        public void init(PsychoEngine psychoEngine) {
            view.add(background);
            background.distance(Observer.ZFAR / 2);
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 })));
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 })));
            view.add(eyePos.get(0));
            view.add(eyePos.get(1));
            eyePos.get(0).distance(-0.1);
            eyePos.get(0).size(170);
            eyePos.get(1).distance(-0.1);
            eyePos.get(1).size(170);
            eyePos.get(1).rotation(0, 90, 0);
            addItems();
        }

        private void addItems() {
            float angle = 90f;
            for (int i = 0; i < 2 * angle + 1; i++) {
                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }));
                item.distance(Observer.ZFAR / 4);
                item.position(0, i - angle);
                item.size(1);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }));
                item.distance(Observer.ZFAR / 4);
                item.position(i - angle, 0);
                item.size(1);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }));
                item.distance(Observer.ZFAR / 4);
                item.position(i - angle, i - angle);
                item.size(1);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }));
                item.distance(Observer.ZFAR / 4);
                item.position(i - angle, angle - i);
                item.size(1);
                view.add(item);
            }
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command == Command.NONE) return;
            switch (command) {
                case ITEM1 -> back(psychoEngine);
                case ITEM2 -> up(psychoEngine);
                case ITEM3 -> toggleDistortion(psychoEngine);
                case ITEM4 -> left(psychoEngine);
                case ITEM5 -> toggleRotation();
                case ITEM6 -> right(psychoEngine);
                case ITEM7 -> forward(psychoEngine);
                case ITEM8 -> down(psychoEngine);
                case ITEM9 -> toggleViewMode(psychoEngine);
                default -> {}
            };
        }

        private void toggleRotation() {
			rotate = !rotate;
		}

        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
        }

        private void forward(PsychoEngine psychoEngine) {
            psychoEngine.translate(new Vector3f(0, 0, -STEP));
        }

        private void back(PsychoEngine psychoEngine) {
            psychoEngine.translate(new Vector3f(0, 0, STEP));
        }

        private void left(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotate(new Vector3f(0, -STEP, 0));
            } else {
                psychoEngine.translate(new Vector3f(-STEP, 0, 0));
            }
        }

        private void right(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotate(new Vector3f(0, STEP, 0));
            } else {
                psychoEngine.translate(new Vector3f(STEP, 0, 0));
            }
        }

        private void up(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotate(new Vector3f(STEP, 0, 0));
            } else {
                psychoEngine.translate(new Vector3f(0, STEP, 0));
            }
        }

        private void down(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotate(new Vector3f(-STEP, 0, 0));
            } else {
                psychoEngine.translate(new Vector3f(0, -STEP, 0));
            }
        }

        private void toggleViewMode(PsychoEngine psychoEngine) {
            if (psychoEngine.getViewMode() == ViewMode.MONO) {
                psychoEngine.setViewMode(ViewMode.STEREO);
                psychoEngine.setPupilDistance(0);
            } else
                psychoEngine.setViewMode(ViewMode.MONO);
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
        }

        private void toggleDistortion(PsychoEngine psychoEngine) {
            if (distortion) psychoEngine.setNoDistortion();
            else psychoEngine.setDistortion(-0.0001, -0.003);
            distortion = !distortion;
        };
    }
}
