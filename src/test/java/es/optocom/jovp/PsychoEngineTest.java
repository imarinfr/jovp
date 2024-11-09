package es.optocom.jovp;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.Projection;
import es.optocom.jovp.definitions.Units;
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.definitions.ViewMode;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
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
        monitor.setPhysicalSize(monitorWidthMM, monitorHeightMM);
        psychoEngine.setSize(monitor.getWidth(), monitor.getHeight());
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
     * Render a triangle
     *
     * @since 0.0.1
     */
    @Test
    public void showTriangle() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicTriangle());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Add Barrel and pincushion distortions using Brown-Conrady model
     *
     * @since 0.0.1
     */
    //@Test
    public void coordinateSystems() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicCoordinateSystems(), Projection.ORTHOGRAPHIC);
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
    public void stereoMode() {
        PsychoEngine psychoEngine = new PsychoEngine(new StereoLogic(), 500, ViewMode.STEREO);
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Spherical coordinates
     *
     * @since 0.0.1
     */
    @Test
    public void sphericalCoordinates() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicSpherical(), 50);
        psychoEngine.start("keypad", InputType.REPEAT, Paradigm.M9AFC);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Add Barrel and pincushion distortions using Brown-Conrady model
     *
     * @since 0.0.1
     */
    @Test
    public void barrelDistortion() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicDistortion());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * View virtual world
     *
     * @since 0.0.1
     */
    //@Test
    public void viewVirtualWorld() {
        PsychoEngine psychoEngine = new PsychoEngine(new WorldLogic(), 100, Projection.PERSPECTIVE);
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

    /** Psychophysics logic to show a simple triangle */
    static class LogicTriangle implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.TRIANGLE), new Texture(TextureType.CHECKERBOARD), Units.ANGLES);
            item.setColors(new double[] { 1, 1, 1, 1 }, new double[] { 0, 0, 1, 1 });
            item.frequency(90, 0.2);
            item.depth(90);
            item.position(0, 0);
            item.size(10);
            view.add(item);

            Text title = new Text();
            title.setText("Hello Triangle");
            title.setPosition(0.3, 0.05);
            title.setSize(0.4);
            view.add(title);
        }

        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }

    }

    /** Psychophysics logic to play around different coordinate systems */
    static class LogicCoordinateSystems implements PsychoLogic {

        Text title;
        Timer timer = new Timer();
        double depth;
        Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.5, 0.5, 0.5, 1}), Units.ANGLES);
        Item zMovingPixels, zMovingMeters, zMovingAngular, zMovingSpherical;

        @Override
        public void init(PsychoEngine psychoEngine) {

            depth = 0.0f;
            double scale = psychoEngine.getProjection() == Projection.ORTHOGRAPHIC ? 1 : 2 * depth;
            double posPixels = psychoEngine.getDistanceM() * Math.atan(Math.toRadians(-3)) / psychoEngine.getMonitor().getPixelWidthM();
            double posMeters = psychoEngine.getDistanceM() * Math.atan(Math.toRadians(-4));
            double posAngular = -5;
            double posSpherical = -6;

            background.depth(100);
            background.position(0, 0);
            view.add(background);

            double x = 0.05 * (psychoEngine.getProjection() == Projection.ORTHOGRAPHIC ? 1 : psychoEngine.getDistanceM() + depth * Math.tan(Math.toRadians(5)));
            Item center = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 0, 1}), Units.METERS);
            center.depth(depth);
            center.position(0, 0);
            center.size(scale * 0.001);
            view.add(center);
            Item centerSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 0, 0.25}), Units.METERS);
            centerSurround.depth(depth);
            centerSurround.position(0, 0);
            centerSurround.size(scale * 0.01);
            view.add(centerSurround);
            Item cart = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
            cart.depth(depth);
            cart.position(scale * 0.05, 0);
            cart.size(scale * 0.001);
            view.add(cart);
            Item cartSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.METERS);
            cartSurround.depth(depth);
            cartSurround.position(x, 0);
            cartSurround.size(scale * 0.01);
            view.add(cartSurround);
            Item ang = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.ANGLES);
            ang.depth(depth);
            ang.position(0, 5);
            ang.size(0.1);
            view.add(ang);
            Item angSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.ANGLES);
            angSurround.depth(depth);
            angSurround.position(0, 5);
            angSurround.size(1);
            view.add(angSurround);
            float pixw = psychoEngine.getMonitor().getPixelWidthM();
            float pixh = psychoEngine.getMonitor().getPixelHeightM();
            Item pix = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.PIXELS);
            pix.depth(depth);
            pix.position(0, scale * -0.05 / pixh);
            pix.size(scale * 0.001 / pixh, scale * 0.001 / pixw);
            view.add(pix);
            Item pixSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.PIXELS);
            pixSurround.depth(depth);
            pixSurround.position(0, scale * -0.05 / pixh);
            pixSurround.size(scale * 0.01 / pixh, scale * 0.01 / pixw);
            view.add(pixSurround);

            zMovingPixels = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.1, 0.1, 0.1, 1}), Units.PIXELS);
            zMovingPixels.depth(0);
            zMovingPixels.position(scale * posPixels, 0);
            zMovingPixels.size(scale * 0.01 / psychoEngine.getMonitor().getPixelWidthM());
            view.add(zMovingPixels);
            zMovingMeters = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.METERS);
            zMovingMeters.depth(depth);
            zMovingMeters.position(scale * posMeters, 0);
            zMovingMeters.size(scale * 0.01);
            view.add(zMovingMeters);
            zMovingAngular = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.9, 0.6, 0.9, 1}), Units.ANGLES);
            zMovingAngular.depth(depth);
            zMovingAngular.position(posAngular, 0);
            zMovingAngular.size(1);
            view.add(zMovingAngular);
            zMovingSpherical = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.9, 0.9, 0.6, 1}), Units.SPHERICAL);
            zMovingSpherical.depth(depth);
            zMovingSpherical.position(posSpherical, 0);
            zMovingSpherical.size(1);
            view.add(zMovingSpherical);

            title = new Text();
            title.setText("Coordinate systems: " + psychoEngine.getProjection() + " projection");
            title.setColor(new double[] {0, 1, 0, 1});
            title.setPosition(0.1, 0.05);
            title.setSize(0.8);
            view.add(title);
            timer.start();
        }
    
        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command == Command.NONE) return;
            switch (psychoEngine.getProjection()) {
                case ORTHOGRAPHIC -> psychoEngine.setProjection(Projection.PERSPECTIVE);
                case PERSPECTIVE -> psychoEngine.setProjection(Projection.ORTHOGRAPHIC);
            }
        }
    
        @Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            title.setText("Coordinate systems: " + psychoEngine.getProjection() + " projection");

            double d = (1 + depth - Math.cos(timer.getElapsedTime() / 500.0)) / 2.5;
            zMovingPixels.depth(d);
            zMovingMeters.depth(d);
            zMovingAngular.depth(d);
            zMovingSpherical.depth(d);
        }
    
    }
        
    /** Psychophysics logic class */
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

        @Override
        public void init(PsychoEngine psychoEngine) {
            background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor)); // background
            background.position(0, 0);
            background.depth(10);
            view.add(background);
            fixation = new Item(new Model(ModelType.MALTESE), new Texture(fixationColor)); // fixation
            fixation.size(2);
            fixation.depth(5);
            view.add(fixation);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus1.position(-3, -3);
            stimulus1.size(4.5, 4.5);
            stimulus1.depth(6);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            stimulus1.contrast(0.75);
            view.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.CHECKERBOARD));
            stimulus2.frequency(0, 2);
            stimulus2.position(6, 2);
            stimulus2.size(4, 2);
            stimulus2.depth(6);
            stimulus2.show(ViewEye.LEFT);
            stimulus2.contrast(0.25);
            view.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
            stimulus3.frequency(0, 2);
            stimulus3.position(3, -2);
            stimulus3.size(2, 2);
            stimulus3.depth(6);
            stimulus3.show(ViewEye.RIGHT);
            stimulus3.contrast(0.5);
            view.add(stimulus3);
            // Add title
            title = new Text();
            title.setText("Stereoscopic view");
            title.setPosition(0.25, 0.05);
            title.setSize(0.5);
            view.add(title);
            // Add text to show FPS
            text = new Text();
            text.setText("Refresh rate:");
            text.setPosition(0, 0.1);
            text.setSize(0.35);
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

        @Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            double time = timer.getElapsedTime();
            if (swapEyeTimer.getElapsedTime() > swapEyeTime) {
                if (stimulus2.getEye() == ViewEye.LEFT)
                    stimulus2.show(ViewEye.RIGHT);
                else
                    stimulus2.show(ViewEye.LEFT);
                if (stimulus3.getEye() == ViewEye.RIGHT)
                    stimulus3.show(ViewEye.LEFT);
                else
                    stimulus3.show(ViewEye.RIGHT);
                swapEyeTimer.start();
            }
            stimulus1.contrast(Math.sin(time / 1000.0) / 2 + 0.5);
            stimulus1.rotation(time / 10.0);
            stimulus2.rotation(-time / 20.0);
            stimulus2.texRotation(time / 20.0);
            if (timerFps.getElapsedTime() <= refreshTime)
                fps++;
            else { // restart the timer every second
                timerFps.start();
                text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }
    }

    /** Psychophysics logic to play around different coordinate systems */
    class LogicSpherical implements PsychoLogic {

        private static final float STEP = 0.01f;
        private static final float ANGLE = 3.0f;
        private boolean rotate = false;

        Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.5, 0.5, 0.5, 1}), Units.ANGLES);
        Item item1, item2, item3, item4, item5, item6, item7, item8;
        double counter = 0;
        @Override
        public void init(PsychoEngine psychoEngine) {
            psychoEngine.translateView(new Vector3f(0, 0, -0.25f));
            background.depth(90);
            background.position(0, 0);
            view.add(background);

            Item center = new Item(new Model(ModelType.CROSS), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
            center.depth(-psychoEngine.getDistanceM());
            center.position(0, 0);
            center.size(0.05);
            view.add(center);
            center = new Item(new Model(ModelType.CROSS), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
            center.depth(-psychoEngine.getDistanceM());
            center.position(0, 0);
            center.size(0.05);
            center.rotation(0, 90, 0);
            view.add(center);
            center = new Item(new Model(ModelType.CROSS), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
            center.depth(-psychoEngine.getDistanceM());
            center.position(0, 0);
            center.size(0.05);
            center.rotation(90, 0, 0);
            view.add(center);

            double depth = 0.05f;
            double size = 10;
            item1 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item1.depth(depth);
            item1.position(0, 0);
            item1.size(size);
            view.add(item1);
    
            item2 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item2.depth(depth);
            item2.position(0, 0);
            item2.size(size);
            view.add(item2);
    
            item3 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item3.depth(depth);
            item3.position(0, 0);
            item3.size(size);
            view.add(item3);
    
            item4 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item4.depth(depth);
            item4.position(0, 0);
            item4.size(size);
            view.add(item4);
    
            item5 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item5.depth(depth);
            item5.position(0, 0);
            item5.size(size);
            view.add(item5);
    
            item6 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item6.depth(depth);
            item6.position(0, 0);
            item6.size(size);
            view.add(item6);
    
            item7 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item7.depth(depth);
            item7.position(0, 0);
            item7.size(size);
            view.add(item7);
    
            item8 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
            item8.depth(depth);
            item8.position(0, 0);
            item8.size(size);
            view.add(item8);
    
            Text title = new Text();
            title.setText("Spherical coordinates");
            title.setColor(new double[] {0, 1, 0, 1});
            title.setPosition(0.3, 0.05);
            title.setSize(0.4);
            view.add(title);
        }
        
        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command == Command.NONE) return;
            switch (command) {
                case ITEM1 -> back(psychoEngine);
                case ITEM2 -> up(psychoEngine);
                case ITEM3 -> toggleProjection(psychoEngine);
                case ITEM4 -> left(psychoEngine);
                case ITEM5 -> toggleRotation();
                case ITEM6 -> right(psychoEngine);
                case ITEM7 -> forward(psychoEngine);
                case ITEM8 -> down(psychoEngine);
                case ITEM9 -> toggleViewMode(psychoEngine);
                default -> {}
            };
        }
        
        @Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            double angle = (counter++ % 360 + 360) % 360 - 180;
            item1.position(0, angle);
            item2.position(0, angle - 180);
            item3.position(angle, 0);
            item4.position(angle - 180, 0);
            item5.position(angle, angle);
            item6.position(angle - 180, angle - 180);
            item7.position(angle, -angle);
            item8.position(angle - 180, -angle - 180);
        }

        private void forward(PsychoEngine psychoEngine) {
            psychoEngine.translateView(new Vector3f(0, 0, -STEP));
        }
    
        private void back(PsychoEngine psychoEngine) {
            psychoEngine.translateView(new Vector3f(0, 0, STEP));
        }
    
        private void left(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(0, ANGLE, 0));
            } else {
                psychoEngine.translateView(new Vector3f(STEP, 0, 0));
            }
        }
    
        private void right(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(0, -ANGLE, 0));
            } else {
                 psychoEngine.translateView(new Vector3f(-STEP, 0, 0));
            }
        }
    
        private void up(PsychoEngine psychoEngine) {
            if (rotate) {
                    psychoEngine.rotateView(new Vector3f(-ANGLE, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, -STEP, 0));
            }
        }
    
        private void down(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(ANGLE, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, STEP, 0));
            }
        }

        private void toggleRotation() {
            rotate = !rotate;
        }
 
        private void toggleViewMode(PsychoEngine psychoEngine) {
            if (psychoEngine.getViewMode() == ViewMode.MONO) {
                psychoEngine.setViewMode(ViewMode.STEREO);
                psychoEngine.setPupilDistance(0);
            } else {
                psychoEngine.setViewMode(ViewMode.MONO);
            }
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
        }

        private void toggleProjection(PsychoEngine psychoEngine) {
            switch (psychoEngine.getProjection()) {
                case ORTHOGRAPHIC -> psychoEngine.setProjection(Projection.PERSPECTIVE);
                case PERSPECTIVE -> psychoEngine.setProjection(Projection.ORTHOGRAPHIC);
            }
        };

    }

    /** Psychophysics logic to show a simple triangle */
    static class LogicDistortion implements PsychoLogic {

        private boolean distortion = false;

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD));
            item.setColors(new double[] { 1, 1, 1, 1 }, new double[] { 0, 0, 1, 1 });
            item.frequency(0, 0.25);
            item.depth(1);
            item.position(0, 0);
            item.size(24, 12);
            item.rotation(0);
            Text title = new Text();
            title.setText("Click to Toggle Distortion");
            title.setPosition(0.3, 0.05);
            title.setSize(0.4);
            view.add(item);
            view.add(title);
        }

    @Override
        public void input(PsychoEngine psychoEngine, Command command) {
            if (command == Command.NONE) return;
            if (distortion) {
                psychoEngine.setDistortion(0.0001);
                System.out.println("Distortion ON");
            } else {
                psychoEngine.setDistortion(0);
                System.out.println("Distortion OFF");
            }
            distortion = !distortion;
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }

    }


    static class WorldLogic implements PsychoLogic {

        private static final float STEP = 0.1f;
        private static final float ANGLE = 3.0f;
        private boolean distortion = false;
        private boolean rotate = false;

        Item item;
        ArrayList<Item> eyePos = new ArrayList<Item>(2); 
        Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 0.5, 0.5, 0.5, 1 }), Units.ANGLES);
        ArrayList<Item> items = new ArrayList<Item>();

        @Override
        public void init(PsychoEngine psychoEngine) {
            Text title = new Text(new double[] {0, 1, 0, 1});
            title.setText("Virtual World");
            title.setPosition(0.35, 0.05);
            title.setSize(0.3);
            view.add(title);
            view.add(background);
            background.depth(10);
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 }), Units.METERS));
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 }), Units.METERS));eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 }), Units.METERS));
            eyePos.get(0).depth(-psychoEngine.getDistanceM());
            eyePos.get(0).size(0.2);
            eyePos.get(1).depth(-psychoEngine.getDistanceM());
            eyePos.get(1).size(0.2);
            eyePos.get(1).rotation(0, 90, 0);
            eyePos.get(2).depth(-psychoEngine.getDistanceM());
            eyePos.get(2).size(0.2);
            eyePos.get(2).rotation(90, 0, 0);
            view.add(eyePos.get(0));
            view.add(eyePos.get(1));
            view.add(eyePos.get(2));
            addItems();
        }

        private void addItems() {
            Units units = Units.SPHERICAL;
            float angle = 180.0f;
            double size = 3;
            if (units == Units.ANGLES) angle = 80.0f;
            for (int i = 0; i < 2 / size * angle; i++) {
                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), units);
                item.depth(5);
                item.position(0, size * (i - angle));
                item.size(size);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), units);
                item.depth(5);
                item.position(size * (i - angle), 0);
                item.size(size);
                view.add(item);
            }

            for (int i = 0; i < 2 / size * angle; i++) {
                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), units);
                item.depth(5);
                item.position(size * (i - angle), size * (i - angle));
                item.size(size);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), units);
                item.depth(5);
                item.position(size * (i - angle), size * (angle - i));
                item.size(size);
                view.add(item);
            }
        }

        @Override
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

		@Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
        }

        private void forward(PsychoEngine psychoEngine) {
            psychoEngine.translateView(new Vector3f(0, 0, -STEP));
        }

        private void back(PsychoEngine psychoEngine) {
            psychoEngine.translateView(new Vector3f(0, 0, STEP));
        }

        private void left(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(0, ANGLE, 0));
            } else {
                psychoEngine.translateView(new Vector3f(STEP, 0, 0));
            }
        }

        private void right(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(0, -ANGLE, 0));
            } else {
                psychoEngine.translateView(new Vector3f(-STEP, 0, 0));
            }
        }

        private void up(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(-ANGLE, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, -STEP, 0));
            }
        }

        private void down(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(ANGLE, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, STEP, 0));
            }
        }

        private void toggleRotation() {
			rotate = !rotate;
		}

        private void toggleViewMode(PsychoEngine psychoEngine) {
            if (psychoEngine.getViewMode() == ViewMode.MONO) {
                psychoEngine.setViewMode(ViewMode.STEREO);
                psychoEngine.setPupilDistance(0);
            } else {
                psychoEngine.setViewMode(ViewMode.MONO);
            }
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
        }

        private void toggleDistortion(PsychoEngine psychoEngine) {
            if (distortion) {
                psychoEngine.setNoDistortion();
            } else {
                psychoEngine.setDistortion(0.01, -0.001);
            }
            distortion = !distortion;
        };
    }

}
