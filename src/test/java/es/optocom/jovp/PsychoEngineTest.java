package es.optocom.jovp;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.ViewEye;
import es.optocom.jovp.definitions.InputType;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.Units;
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
        PsychoEngine psychoEngine = new PsychoEngine(new LogicTriangle(), 750);
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Add Barrel and pincushion distortions using Brown-Conrady model
     *
     * @since 0.0.1
     */
    @Test
    public void coordinateSystems() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicCoordinates(), 572.943f);
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

        /**
     * 
     * Add Barrel and pincushion distortions using Brown-Conrady model
     *
     * @since 0.0.1
     */
    @Test
    public void sphericalCoordinates() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicSpherical(), 750);
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
     * View virtual world
     *
     * @since 0.0.1
     */
    @Test
    public void viewVirtualWorld() {
        PsychoEngine psychoEngine = new PsychoEngine(new WorldLogic(), 100);
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

        Item item;
        int count = 0;
        @Override
        public void init(PsychoEngine psychoEngine) {
            item = new Item(new Model(ModelType.TRIANGLE), new Texture(TextureType.CHECKERBOARD), Units.ANGLES);
            item.setColors(new double[] { 1, 1, 1, 1 }, new double[] { 0, 0, 1, 1 });
            item.frequency(90, 0.25);
            item.distance(0);
            item.position(0, 0);
            item.size(10);
            Text title = new Text();
            title.setText("Hello Triangle");
            title.setPosition(0.3, 0.05);
            title.setSize(0.4);
            view.add(item);
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
    static class LogicCoordinates implements PsychoLogic {

        Timer timer = new Timer();
        double distance;
        Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.5, 0.5, 0.5, 1}), Units.ANGLES);
        Item zMovingCartesian, zMovingAngular, zMovingSpherical;

        @Override
        public void init(PsychoEngine psychoEngine) {

            distance = psychoEngine.getDistanceM();
            double posCartesian = distance * Math.atan(Math.toRadians(-6));
            double posAngular = -5;
            double posSpherical = -4;

            background.distance(1000);
            background.position(0, 0);
            view.add(background);

            Item center = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 0, 1}), Units.METERS);
            center.distance(distance);
            center.position(0, 0);
            center.size(0.001);
            view.add(center);
            Item centerSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 0, 0.25}), Units.METERS);
            centerSurround.distance(distance);
            centerSurround.position(0, 0);
            centerSurround.size(0.01);
            view.add(centerSurround);

            Item cart = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
            cart.distance(distance);
            cart.position(0.05, 0);
            cart.size(0.001);
            view.add(cart);
            Item cartSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.METERS);
            cartSurround.distance(distance);
            cartSurround.position(0.05, 0);
            cartSurround.size(0.01);
            view.add(cartSurround);

            Item ang = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.ANGLES);
            ang.distance(distance);
            ang.position(0, 5);
            ang.size(0.1);
            view.add(ang);
            Item angSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.ANGLES);
            angSurround.distance(distance);
            angSurround.position(0, 5);
            angSurround.size(1);
            view.add(angSurround);

            float pixw = psychoEngine.getMonitor().getPixelWidthM();
            float pixh = psychoEngine.getMonitor().getPixelHeightM();

            Item pix = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.PIXELS);
            pix.distance(distance);
            pix.position(0, -0.05 / pixh);
            pix.size(0.001 / pixh, 0.001 / pixw);
            view.add(pix);
            Item pixSurround = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 0.25}), Units.PIXELS);
            pixSurround.distance(distance);
            pixSurround.position(0, -0.05 / pixh);
            pixSurround.size(0.01 / pixh, 0.01 / pixw);
            view.add(pixSurround);

            zMovingCartesian = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.METERS);
            zMovingCartesian.distance(distance);
            zMovingCartesian.position(posCartesian, 0);
            zMovingCartesian.size(0.01);
            view.add(zMovingCartesian);

            zMovingAngular = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.9, 0.6, 0.9, 1}), Units.ANGLES);
            zMovingAngular.distance(distance);
            zMovingAngular.position(posAngular, 0);
            zMovingAngular.size(1);
            view.add(zMovingAngular);

            zMovingSpherical = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.9, 0.9, 0.6, 1}), Units.SPHERICAL);
            zMovingSpherical.distance(distance);
            zMovingSpherical.position(posSpherical, 0);
            zMovingSpherical.size(1);
            view.add(zMovingSpherical);

            Text title = new Text();
            title.setText("Coordinate systems");
            title.setColor(new double[] {0, 1, 0, 1});
            title.setPosition(0.3, 0.05);
            title.setSize(0.4);
            view.add(title);
            
            timer.start();
        }
    
        @Override
        public void input(PsychoEngine psychoEngine, Command command) {
        }
    
        @Override
        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);

            double d = distance + 1.5 - 1.5 * Math.cos(timer.getElapsedTime() / 500.0f);
            zMovingCartesian.distance(d);
            zMovingAngular.distance(d);
            zMovingSpherical.distance(d);
        }
    
    }

    /** Psychophysics logic to play around different coordinate systems */
    class LogicSpherical implements PsychoLogic {

            Timer timer = new Timer();
            Item background = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0.5, 0.5, 0.5, 1}), Units.ANGLES);
            Item item1, item2, item3, item4, item5, item6, item7, item8, item9, item10;
    
            @Override
            public void init(PsychoEngine psychoEngine) {
    
                psychoEngine.translateView(new Vector3f(0, 0, -1000));
                background.distance(1000);
                background.position(0, 0);
                view.add(background);

                Item center = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 0, 0, 1}), Units.METERS);
                center.distance(-psychoEngine.getDistance());
                center.position(0, 0);
                center.size(1);
                view.add(center);
    
                item1 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item1.distance(0.75);
                item1.position(0, 0);
                item1.size(90, 90);
                view.add(item1);
    
                item2 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item2.distance(50);
                item2.position(0, 0);
                item2.size(1, 1);
                view.add(item2);
    
                item3 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item3.distance(50);
                item3.position(0, 0);
                item3.size(1, 1);
                view.add(item3);
    
                item4 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item4.distance(50);
                item4.position(0, 0);
                item4.size(1, 1);
                view.add(item4);
    
                item5 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item5.distance(50);
                item5.position(0, 0);
                item5.size(1, 1);
                view.add(item5);
    
                item6 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item6.distance(50);
                item6.position(0, 0);
                item6.size(1, 1);
                view.add(item6);
    
                item7 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item7.distance(50);
                item7.position(0, 0);
                item7.size(1, 1);
                view.add(item7);
    
                item8 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {1, 1, 1, 1}), Units.SPHERICAL);
                item8.distance(50);
                item8.position(0, 0);
                item8.size(1, 1);
                view.add(item8);
    
                item9 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 1, 1}), Units.SPHERICAL);
                item9.distance(50);
                item9.position(0, 0);
                item9.size(1, 1);
                view.add(item9);
    
                item10 = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] {0, 1, 1, 1}), Units.SPHERICAL);
                item10.distance(50);
                item10.position(0, 0);
                item10.size(1, 1);
                view.add(item10);
    
                Text title = new Text();
                title.setText("Spherical coordinates");
                title.setColor(new double[] {0, 1, 0, 1});
                title.setPosition(0.3, 0.05);
                title.setSize(0.4);
                view.add(title);
                timer.start();
            }
        
            @Override
            public void input(PsychoEngine psychoEngine, Command command) {
            }
        
            @Override
            public void update(PsychoEngine psychoEngine) {
                float[] fov = psychoEngine.getFieldOfView();
                background.size(fov[0], fov[1]);
                double angle = ((timer.getElapsedTime() / 20 + 180) % 360 + 360) % 360 - 180;
                item1.position(0, angle);
                item2.position(0, angle - 180);
                item3.position(angle, 0);
                item4.position(angle - 180, 0);
                item5.position(angle, angle);
                item6.position(angle - 180, angle - 180);
                item7.position(angle, -angle);
                item8.position(angle - 180, -angle - 180);
                item9.position(30, angle);
                item10.position(angle, 30);
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
            background.distance(900);
            view.add(background);
            fixation = new Item(new Model(ModelType.MALTESE), new Texture(fixationColor)); // fixation
            fixation.size(2);
            fixation.distance(50);
            view.add(fixation);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus1.position(-3, -3);
            stimulus1.size(4.5, 4.5);
            stimulus1.distance(500);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            stimulus1.contrast(0.75);
            view.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.CHECKERBOARD));
            stimulus2.frequency(0, 2);
            stimulus2.position(6, 2);
            stimulus2.size(4, 2);
            stimulus2.distance(250);
            stimulus2.show(ViewEye.LEFT);
            stimulus2.contrast(0.25);
            view.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
            stimulus3.frequency(0, 2);
            stimulus3.position(3, -2);
            stimulus3.size(2, 2);
            stimulus3.distance(100);
            stimulus3.show(ViewEye.RIGHT);
            stimulus3.distance(75);
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

    static class WorldLogic implements PsychoLogic {

        private static final float STEP = 5.0f;
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
            background.distance(Observer.ZFAR / 2);
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 })));
            eyePos.add(new Item(new Model(ModelType.CROSS), new Texture(new double[] { 1, 0, 0, 1 })));
            view.add(eyePos.get(0));
            view.add(eyePos.get(1));
            eyePos.get(0).distance(-psychoEngine.getDistance() / 1000.0f);
            eyePos.get(0).size(1);
            eyePos.get(1).distance(-psychoEngine.getDistance() / 1000.0f);
            eyePos.get(1).size(1);
            eyePos.get(1).rotation(0, 90, 0);
            addItems();
        }

        private void addItems() {
            Units projection = Units.ANGLES;
            float angle = 180.0f;
            if (projection == Units.ANGLES) angle = 80.0f;
            for (int i = 0; i < 2 * angle + 1; i++) {
                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), projection);
                item.distance(50);
                item.position(0, i - angle);
                item.size(1);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), projection);
                item.distance(50);
                item.position(i - angle, 0);
                item.size(1);
                view.add(item);
            }

            for (int i = 0; i < 2 * Math.sqrt(2) * angle - 1; i++) {
                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), projection);
                item.distance(50);
                item.position((i - angle) / Math.sqrt(2), (i - angle) / Math.sqrt(2));
                item.size(1);
                view.add(item);

                item = new Item(new Model(ModelType.CIRCLE), new Texture(new double[] { 1, 1, 1, 1 }), projection);
                item.distance(50);
                item.position((i - angle) / Math.sqrt(2), (angle - i) / Math.sqrt(2));
                item.size(1);
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

        private void toggleRotation() {
			rotate = !rotate;
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
                psychoEngine.rotateView(new Vector3f(0, -STEP, 0));
            } else {
                psychoEngine.translateView(new Vector3f(STEP, 0, 0));
            }
        }

        private void right(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(0, STEP, 0));
            } else {
                psychoEngine.translateView(new Vector3f(-STEP, 0, 0));
            }
        }

        private void up(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(-STEP, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, -STEP, 0));
            }
        }

        private void down(PsychoEngine psychoEngine) {
            if (rotate) {
                psychoEngine.rotateView(new Vector3f(STEP, 0, 0));
            } else {
                psychoEngine.translateView(new Vector3f(0, STEP, 0));
            }
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

    /** Psychophysics logic to show a simple triangle */
    class LogicDistortion implements PsychoLogic {

        private boolean distortion = false;

        @Override
        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD));
            item.setColors(new double[] { 1, 1, 1, 1 }, new double[] { 0, 0, 1, 1 });
            item.frequency(0, 0.2);
            item.distance(100);
            item.position(0, 0);
            item.size(25, 15);
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

}
