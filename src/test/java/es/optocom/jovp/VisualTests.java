package es.optocom.jovp;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Optotype;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

/**
 *
 * Unitary tests for different contrasts and changes
 *
 * @since 0.0.1
 */
public class VisualTests {

    /**
     *
     * Unitary tests for different contrasts and changes
     *
     * @since 0.0.1
     */
    public VisualTests() {
    }

    /**
     *
     * Patterns and their spatial properties
     *
     * @since 0.0.1
     */
    @Test
    public void changingContrast() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicContrast());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     *
     * Performs a visual acuity test
     *
     * @since 0.0.1
     */
    @Test
    public void visualAcuityTest() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicVA());
        psychoEngine.start("keypad", Paradigm.M4AFC);
        psychoEngine.cleanup();
    }

    /**
     *
     * Shows optotypes
     *
     * @since 0.0.1
     */
    @Test
    public void showAllOptotypes() {
        final String classPath = System.getProperty("java.class.path", ".");
        System.out.println(classPath);
        PsychoEngine psychoEngine = new PsychoEngine(new LogicOptotypes());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Shows all optotypes moving awkwardly
     *
     * @since 0.0.1
     */
    @Test
    public void stressTest() {
        PsychoEngine psychoEngine = new PsychoEngine(new StressLogic());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    /**
     * 
     * Blinking stuff
     *
     * @since 0.0.1
     */
    @Test
    public void blinkingAndChangingShape() {
        PsychoEngine psychoEngine = new PsychoEngine(new LogicBlinkingAndChangingShape());
        psychoEngine.start("mouse", Paradigm.CLICKER);
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class LogicContrast implements PsychoLogic {

        double[] fixation = new double[] { 0, 1, 0, 1 };
        double[] black = new double[] { 0, 0, 0, 1 };
        double[] white = new double[] { 1, 1, 1, 1 };
        double[] red = new double[] { 1, 0, 0, 1 };
        double[] green = new double[] { 0, 1, 0, 1 };

        Item stimulus1, stimulus2, stimulus3;

        Timer timer = new Timer();
        Timer timerFps = new Timer();
        int fps = 0;
        Text text;
        int refreshTime = 500;

        public void init(PsychoEngine psychoEngine) {
            Item item = new Item(new Model(ModelType.MALTESE), new Texture(fixation));
            item.size(1, 1);
            item.distance(20);
            view.add(item);
            item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE, black, white));
            item.position(14, 4);
            item.distance(90);
            item.frequency(0, 3);
            item.rotation(45);
            item.contrast(0.5);
            view.add(item);
            item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.SQUARESINE, black, white));
            item.position(6, -3);
            item.distance(100);
            item.frequency(0.25, 2);
            item.size(6, 3);
            item.contrast(0.1);
            view.add(item);
            item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD, black, white));
            item.position(5, 3);
            item.distance(90);
            item.frequency(0.5, 3, 0.25, 2);
            item.size(4, 2);
            item.rotation(0);
            view.add(item);
            item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD, black, white));
            item.position(9, 0);
            item.distance(100);
            item.frequency(0.25, 0.5);
            item.size(15, 12);
            view.add(item);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE, black, white));
            stimulus1.position(-8, -4);
            stimulus1.distance(90);
            stimulus1.size(6, 6);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            view.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus2.frequency(0, 2);
            stimulus2.position(-12, 2);
            stimulus2.distance(90);
            stimulus2.size(6, 3);
            view.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE, red, green));
            stimulus3.frequency(0, 2);
            stimulus3.position(-3, 0);
            stimulus3.distance(90);
            stimulus3.size(2, 2);
            view.add(stimulus3);
            // Add title
            Text title = new Text();
            title.set("Fun with contrasts");
            title.size(10);
            title.position(0, 0);
            view.add(title);
            // Add text to show FPS
            text = new Text();
            text.set("Refresh rate:");
            text.size(10);
            text.position(0, 0);
            view.add(text);
            // Start timers
            timer.start();
            timerFps.start();
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE) System.out.println(command);
        }

        public void update(PsychoEngine psychoEngine) {
            double time = timer.getElapsedTime();
            stimulus1.contrast(0.5 * Math.sin(time / 1000.0) + 0.5);
            stimulus3.contrast(0.4 * Math.sin(time / 500.0) + 0.6);
            stimulus1.frequency(Math.sin(time / 250.0), 0.5);
            stimulus1.rotation(time / 10.0);
            stimulus2.rotation(-time / 20.0);
            stimulus2.texRotation(time / 5.0);
            if (timerFps.getElapsedTime() <= refreshTime)
                fps++;
            else { // restart the timer every second
                timerFps.start();
                text.set("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }

    }

    // Psychophysics logic class
    static class LogicVA implements PsychoLogic {

        double[] white = new double[] { 1, 1, 1, 1 };
        double[] black = new double[] { 0, 0, 0, 1 };

        Random random = new Random();
        Item optotype;
        double minSize = 0.1 / 12.0;
        double maxSize = 5 / 12.0;
        double size = maxSize;
        double deltaSize = 2 / 12.0;
        double minDeltaSize = 0.1 / 12.0;
        double theta = 0;
        boolean lastSeen = true;
        int reversals = 0;
        Text info;

        public void init(PsychoEngine psychoEngine) {
            // Background
            Item bg = new Item(new Model(ModelType.SQUARE), new Texture(white)); // background
            bg.size(8, 8);
            bg.position(0, 0);
            bg.distance(100);
            view.add(bg);
            // Optotype
            optotype = new Item(new Model(Optotype.E), new Texture(black));
            optotype.position(0, 0);
            optotype.distance(90);
            optotype.size(size, size);
            theta = 180 * random.nextInt(2);
            optotype.rotation(theta);
            view.add(optotype);
            // Title
            Text title = new Text();
            title.set("Visual Acuity test");
            title.size(10);
            title.position(0, 0);
            view.add(title);
            // Info text
            info = new Text();
            info.set("VA: " + String.format("%.2f", 12 * size) + " arc min; " +
                    "LogMAR: " + String.format("%.2f", Math.log10(12 * size)) + "; " +
                    "Reversals: " + reversals);
            info.size(10);
            info.position(0, 0);
            view.add(info);
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command == Command.NONE) return;
            switch (command) {
                case ITEM1 -> nextDeltaSize(90);
                case ITEM2 -> nextDeltaSize(180);
                case ITEM3 -> nextDeltaSize(0);
                case ITEM4 -> nextDeltaSize(270);
                default -> {}
            };
        }

        public void update(PsychoEngine psychoEngine) {
            optotype.size(size);
            optotype.rotation(theta);
            info.set("VA: " + String.format("%.2f", 12 * size) + " arc min; " +
                    "LogMAR: " + String.format("%.2f", Math.log10(12 * size)) + "; " +
                    "Reversals: " + reversals);
            System.out.println("VA: " + String.format("%.2f", 12 * size) + " arc min; " +
                    "LogMAR: " + String.format("%.2f", Math.log10(12 * size)) + "; " +
                    "Reversals: " + reversals);
        }

        private void nextDeltaSize(double correctTheta) {
            if (theta == correctTheta) {
                if (!lastSeen) {
                    reversals++;
                    deltaSize = deltaSize / 2;
                }
                size -= deltaSize;
                lastSeen = true;
            } else {
                if (lastSeen) {
                    reversals++;
                    deltaSize = deltaSize / 2;
                }
                size += deltaSize;
                lastSeen = false;
            }
            if (size < minSize)
                size = minSize;
            if (size > maxSize)
                size = maxSize;
            if (deltaSize < minDeltaSize)
                size = minDeltaSize;
            theta = 90 * random.nextInt(4);
        }

    }

    // Psychophysics logic class
    static class LogicOptotypes implements PsychoLogic {

        Text text;
        double theta;
        int fps = 0;
        Timer timer = new Timer();
        float initialPos = -15.5f;
        float spacing = 1.25f;
        float size = 1;
        int refreshTime = 500;

        boolean inverted = false;
        double[] textColor1 = new double[] { 0.75, 0.75, 1, 1 };
        double[] textColor2 = new double[] { 1, 0.75, 0.75, 1 };
        double[] colorRed = new double[] { 1, 0, 0, 1 };
        double[] colorGreen = new double[] { 0, 1, 0, 1 };
        double[] colorYellow = new double[] { 1, 1, 0, 1 };
        double[] colorBlue = new double[] { 0, 0, 1, 1 };

        public void init(PsychoEngine psychoEngine) {
            view.add(new Item(new Model(Optotype.A), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.B), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.C), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.D), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.E), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.F), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.G), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.H), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.I), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.J), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.K), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.L), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.M), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.N), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.O), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.P), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.Q), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.R), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.S), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.T), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.U), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.V), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.W), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.X), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            view.add(new Item(new Model(Optotype.Y), new Texture(TextureType.SINE, colorBlue, colorYellow)));
            view.add(new Item(new Model(Optotype.Z), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
            float xpos = initialPos;
            for (Item item : view.items()) {
                item.position(xpos, 0.0f);
                item.size(size, size);
                item.distance(100);
                xpos += spacing;
            }
            // Add title
            Text title = new Text();
            title.set("Optotype test");
            title.size(10);
            title.position(0, 0);
            view.add(title);
            // Add text to show FPS
            text = new Text(textColor1);
            text.set("Refresh rate:");
            text.size(10);
            text.position(0, 0);
            view.add(text);
            timer.start();
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE)
                System.out.println(command);
        }

        public void update(PsychoEngine psychoEngine) {
            theta -= 5;
            float xpos = initialPos;
            float ypos = 0.0f;
            for (Item item : view.items()) {
                if (item.getModel().getType() == ModelType.OPTOTYPE) {
                    item.position(xpos, ypos);
                    item.rotation(theta);
                    xpos += spacing;
                }
            }
            if (timer.getElapsedTime() <= refreshTime)
                fps++;
            else { // restart the timer every second
                if (inverted) {
                    for (Item item : view.items()) {
                        if (item.getTexture().getType() == TextureType.CHECKERBOARD)
                            item.setColors(colorGreen, colorRed);
                        if (item.getTexture().getType() == TextureType.SINE)
                            item.setColors(colorYellow, colorBlue);
                    }
                    text.setColor(textColor2);
                } else {
                    for (Item item : view.items()) {
                        if (item.getTexture().getType() == TextureType.CHECKERBOARD)
                            item.setColors(colorRed, colorGreen);
                        if (item.getTexture().getType() == TextureType.SINE)
                            item.setColors(colorBlue, colorYellow);
                    }
                    text.setColor(textColor1);
                }
                inverted = !inverted;
                timer.start();
                text.set("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }

    }

    // Psychophysics logic class
    static class StressLogic implements PsychoLogic {

        double theta;
        double amplitude = 5;
        double frequency = 1;
        double rx = 18;
        double ry = 12;
        int iteration = 0;
        int fps = 0;
        Timer timer = new Timer();
        Timer timerFps = new Timer();
        Text text;
        int refreshTime = 1000;
        double leadPosition = -25;
        double[] triangleColor1 = new double[] { 1, 1, 1, 1 };
        double[] triangleColor2 = new double[] { 0, 0, 1, 1 };
        Item circle;
        double size = 1;
        double ypos = 3;
        double zpos = 25;

        public void init(PsychoEngine psychoEngine) {
            addOptotypes();
            for (int i = 0; i < 15; i++)
                addPolygon(i);
            for (int i = 0; i < 20; i++)
                addCircle(i);
            addTriangle();
            addSquares();
            addText();
            circle = new Item(new Model(ModelType.CIRCLE), new Texture("ecceIvanito.jpeg"));
            circle.position(0, ypos);
            circle.distance(zpos);
            circle.size(size, size);
            circle.show(Eye.BOTH);
            view.add(circle);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            timer.start();
            timerFps.start();
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE)
                System.out.println(command);
        }

        public void update(PsychoEngine psychoEngine) {
            double time = timer.getElapsedTime();
            if (time > 13425) {
                // throw myself at observer
                if (time > 14425) {
                    circle.show(Eye.BOTH);
                    circle.position(0, ypos + Math.sin(time / 100));
                    circle.distance(zpos);
                }
                if (time > 16425) {
                    size += 2;
                    ypos += 0.6;
                    zpos -= 0.5;
                    circle.position(0, ypos);
                    circle.distance(zpos);
                    circle.size(size, size);
                }
                if (zpos < 2)
                    for (Item item : view.items())
                        item.show(Eye.BOTH);
                return;
            }
            iteration++;
            theta -= 5;
            float xpos = -16;
            int number = 0;
            // process optotypes
            for (Item item : view.items()) {
                if (item.getModel().getType() != ModelType.OPTOTYPE)
                    continue;
                double ypos = amplitude * Math.sin((2 * Math.PI * frequency * number / (view.size() - 1)
                        * timer.getElapsedTime()) / 500);
                item.position(xpos, ypos);
                item.rotation(theta);
                xpos += 1.25;
                number++;
            }
            // process polygons
            int imageNumber = 0;
            for (Item item : view.items()) {
                if (item.getModel().getType() != ModelType.POLYGON)
                    continue;
                int pos = iteration - 38 * imageNumber;
                if (pos < 0)
                    pos = 0;
                item.position(rx * Math.cos(pos / 90.0), ry * Math.sin(pos / 90.0));
                item.distance(20);
                item.rotation(-theta / 5);
                imageNumber++;
            }
            // process circles
            imageNumber = 0;
            for (Item item : view.items()) {
                if (item.getModel().getType() != ModelType.CIRCLE)
                    continue;
                if (time < 8000)
                    continue;
                item.show(Eye.BOTH);
                leadPosition += 1 / 60.0;
                if (imageNumber < 10)
                    item.position(leadPosition - 5 * imageNumber, 8);
                item.distance(10);
                if (imageNumber >= 10 & imageNumber < 20)
                    item.position(-leadPosition + 5 * (imageNumber - 10), -8);
                item.distance(10);
                imageNumber++;
            }
            // process triangle
            for (Item item : view.items()) {
                if (item.getModel().getType() != ModelType.TRIANGLE)
                    continue;
                item.size(4 * Math.sin(iteration / 40.0) + 10, 4 * Math.sin(iteration / 30.0) + 10);
                item.contrast(0.4 * Math.sin(iteration / 50.0) + 0.6);
            }
            // process squares
            int square = 0;
            for (Item item : view.items()) {
                if (item.getModel().getType() != ModelType.SQUARE)
                    continue;
                if (square == 0)
                    item.position(-10.025 + Math.sin(iteration / 20.0) / 2, -8);
                if (square == 1)
                    item.position(-8.975 - Math.sin(iteration / 20.0) / 2, -8);
                square++;
            }
            // check FPS
            if (timerFps.getElapsedTime() <= refreshTime) // restart the timer every second
                fps++;
            else {
                text.set("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                timerFps.start();
                fps = 0;
            }
        }

        private void addOptotypes() {
            double[] colorWhite = new double[] { 1, 1, 1, 1 };
            double[] colorRed = new double[] { 1, 0, 0, 1 };
            double[] colorGreen = new double[] { 0, 1, 0, 1 };
            double[] colorBlue = new double[] { 0, 0, 1, 1 };
            view.add(new Item(new Model(Optotype.A), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.B), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.C), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.D), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.E), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.F), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.G), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.H), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.I), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.J), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.K), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.L), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.M), new Texture(colorBlue)));
            view.add(new Item(new Model(Optotype.N), new Texture(colorBlue)));
            view.add(new Item(new Model(Optotype.O), new Texture(colorBlue)));
            view.add(new Item(new Model(Optotype.P), new Texture(colorBlue)));
            view.add(new Item(new Model(Optotype.Q), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.R), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.S), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.T), new Texture(colorWhite)));
            view.add(new Item(new Model(Optotype.U), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.V), new Texture(colorRed)));
            view.add(new Item(new Model(Optotype.W), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.X), new Texture(colorGreen)));
            view.add(new Item(new Model(Optotype.Y), new Texture(colorBlue)));
            view.add(new Item(new Model(Optotype.Z), new Texture(colorBlue)));
            for (Item optotype : view.items())
                optotype.size(1, 1);
        }

        private void addPolygon(int i) {
            String fileName = null;
            if (i % 3 == 0)
                fileName = "ecceIvanito.jpeg";
            if (i % 3 == 1)
                fileName = "ecceHomo.jpeg";
            if (i % 3 == 2)
                fileName = "ivanito.jpeg";
            Item polygon = new Item(new Model(12), new Texture(fileName));
            polygon.size(2, 2);
            view.add(polygon);
        }

        private void addCircle(int i) {
            String fileName = null;
            if (i % 2 == 0)
                fileName = "ecceIvanito.jpeg";
            if (i % 2 == 1)
                fileName = "ivanito.jpeg";
            Item circle = new Item(new Model(ModelType.CIRCLE), new Texture(fileName));
            circle.size(2.25, 2.25);
            circle.show(Eye.NONE);
            view.add(circle);
        }

        private void addTriangle() {
            Item triangle = new Item(new Model(ModelType.TRIANGLE),
                    new Texture(TextureType.SINE, triangleColor1, triangleColor2));
            triangle.position(5, 5);
            triangle.size(2, 2);
            triangle.frequency(0, 1);
            view.add(triangle);
        }

        private void addSquares() {
            double[] color1 = new double[] { 1, 0, 0, 0.5 };
            double[] color2 = new double[] { 0, 0, 1, 0.5 };
            Item square1 = new Item(new Model(ModelType.SQUARE), new Texture(color1));
            Item square2 = new Item(new Model(ModelType.SQUARE), new Texture(color2));
            square1.position(-10, -12);
            square2.position(-9, -12);
            square1.size(0.05, 10);
            square2.size(0.05, 10);
            view.add(square1);
            view.add(square2);
        }

        private void addText() {
            double[] textColor = new double[] { 1, 0, 1, 1 };
            text = new Text(textColor);
            text.set("Optotypes and basic shapes moving");
            text.size(100);
            text.position(0, 0);
            view.add(text);
            text = new Text();
            text.set("Refresh rate: ");
            text.size(100);
            text.position(0, 0);
            view.add(text);
        }

    }

    // Psychophysics logic to show stimuli blinking and changing shape
    static class LogicBlinkingAndChangingShape implements PsychoLogic {
        Timer timer = new Timer();
        Timer timerFps = new Timer();
        int refreshTime = 1000;
        int fps = 0;
        Timer blinkTimer = new Timer();
        int blinkItemTime = 500;
        Timer modelTimer = new Timer();
        int updateModelTime = 2000;
        Timer textureTimer = new Timer();
        int updateTextureTime = 5000;
        Text text;
        Item background, item1, item2;
        ModelType[] models = { ModelType.CIRCLE, ModelType.SQUARE, ModelType.TRIANGLE, ModelType.ANNULUS,
                ModelType.OPTOTYPE };
        TextureType[] textures = { TextureType.CHECKERBOARD, TextureType.SINE, TextureType.G1, TextureType.G2,
                TextureType.G3 };
        double[] backgroundColor = new double[] { 0.5, 0.5, 0.5, 1 };
        double[] color0 = new double[] { 1, 1, 1, 1 };
        double[] color1 = new double[] { 0, 0, 0.5, 1 };

        public void init(PsychoEngine psychoEngine) {
            // Background
            background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor));
            background.position(0, 0);
            background.distance(100);
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            view.add(background);
            // Title
            Text title = new Text();
            title.set("Blinking items");
            title.size(10);
            title.position(0, 0);
            view.add(title);
            // Add text to show FPS
            text = new Text();
            text.set("Refresh rate:");
            text.size(10);
            text.position(0, 0);
            view.add(text);
            // Items
            item1 = new Item(new Model(ModelType.CIRCLE), new Texture(color0, color1));
            item1.size(10, 10);
            item1.distance(90);
            item1.position(0, 0);
            view.add(item1);
            item2 = new Item(new Model(ModelType.MALTESE), new Texture(new double[] { 0, 1, 0, 1 }));
            item2.distance(80);
            item2.position(0, 0);
            item2.size(2, 2);
            view.add(item2);
            timer.start();
            blinkTimer.start();
            modelTimer.start();
            textureTimer.start();
            timerFps.start();
        }

        public void input(PsychoEngine psychoEngine, Command command) {
            if (command != Command.NONE)
                System.out.println(command);
        }

        public void update(PsychoEngine psychoEngine) {
            float[] fov = psychoEngine.getFieldOfView();
            background.size(fov[0], fov[1]);
            item1.rotation(timer.getElapsedTime() / 20);
            double cpd = 0.5 * (Math.cos(timer.getElapsedTime() / 1500) + 1) / 2;
            item1.frequency(0, cpd);
            if (modelTimer.getElapsedTime() > updateModelTime) {
                item1.update(new Model(models[ThreadLocalRandom.current().nextInt(0, 5)]));
                modelTimer.start();
            }
            if (textureTimer.getElapsedTime() > updateTextureTime) {
                item1.update(new Texture(textures[ThreadLocalRandom.current().nextInt(0, 5)], color0, color1));
                textureTimer.start();
            }
            if (blinkTimer.getElapsedTime() > blinkItemTime) {
                if (item2.getEye() == Eye.BOTH)
                    item2.show(Eye.NONE);
                else
                    item2.show(Eye.BOTH);
                blinkTimer.start();
            }
            if (timerFps.getElapsedTime() <= refreshTime)
                fps++;
            else { // restart the timer every second
                timerFps.start();
                text.set("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }
    }

}
