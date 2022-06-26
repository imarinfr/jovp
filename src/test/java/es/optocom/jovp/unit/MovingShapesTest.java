package es.optocom.jovp.unit;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.Timer;
import es.optocom.jovp.engine.rendering.*;
import es.optocom.jovp.engine.structures.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 *
 * MovingShapesTest
 *
 * <ul>
 * <li>Test playing around with textures</li>
 * Unitary tests for different patterns and textures
 * </ul>
 *
 * @since 0.0.1
 */
public class MovingShapesTest {

    /**
     *
     * Shows all optotypes moving awkwardly
     *
     * @since 0.0.1
     */
    @Test
    public void movingShapesAround() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 1000);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        double theta;
        double amplitude = 5;
        double frequency = 1;
        double rx = 10;
        double ry = 8;
        int iteration = 0;
        int fps = 0;
        Timer timer = new Timer();
        Timer timerFps = new Timer();
        Text text;
        int refreshTime = 1000;

        double[] triangleColor1 = new double[] {1, 1, 1, 1};
        double[] triangleColor2 = new double[] {0, 0, 1, 1};

        @Override
        public void init() {
            addOptotypes();
            addCircle();
            addTriangle();
            addSquares();
            addText();
            timer.start();
            timerFps.start();
        }

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update() {
            iteration++;
            theta -= 5;
            float xpos = -22;
            int number = 0;
            // process optotypes
            for (Item item : items) {
                if(item.getModel().getType() != ModelType.OPTOTYPE) continue;
                double ypos = amplitude * Math.sin((2 * Math.PI * frequency * number / (items.size() - 1)
                        * timer.getElapsedTime()) / 500);
                item.position(xpos, ypos);
                item.rotation(theta);
                xpos += 1.75;
                number++;
            }
            // process circle
            for (Item item : items) {
                if(item.getModel().getType() != ModelType.CIRCLE) continue;
                item.position(rx * Math.cos(iteration / 90.0), ry * Math.sin(iteration / 90.0), 6);
                item.rotation(-theta / 2, new Vector3f(0.5f, 0.8f, 1.2f));
            }
            // process triangle
            for (Item item : items) {
                if(item.getModel().getType() != ModelType.TRIANGLE) continue;
                item.size(5 * Math.sin(iteration / 10.0) + 10, 5 * Math.sin(iteration / 15.0) + 10);
                item.frequency(0, 1);
                item.contrast(0.4 * Math.sin(iteration / 50.0) + 0.6);
            }
            // process squares
            int square = 0;
            for (Item item : items) {
                if(item.getModel().getType() != ModelType.SQUARE) continue;
                if (square == 0) item.position(-10.025 + Math.sin(iteration / 20.0) / 2, -8);
                if (square == 1) item.position(-8.975 - Math.sin(iteration / 20.0) / 2, -8);
                square++;
            }
            // check FPS
            if (timerFps.getElapsedTime() <= refreshTime)  // restart the timer every second
                fps++;
            else {
                text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                timerFps.start();
                fps = 0;
            }
        }

        private void addOptotypes() {
            double[] colorRed = new double[] {1, 0, 0, 1};
            double[] colorGreen = new double[] {0, 1, 0, 1};
            double[] colorBlue = new double[] {0, 0, 1, 1};
            items.add(new Item(new Model(Optotype.A), new Texture()));
            items.add(new Item(new Model(Optotype.B), new Texture()));
            items.add(new Item(new Model(Optotype.C), new Texture()));
            items.add(new Item(new Model(Optotype.D), new Texture()));
            items.add(new Item(new Model(Optotype.E), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.F), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.G), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.H), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.I), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.J), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.K), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.L), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.M), new Texture(colorBlue)));
            items.add(new Item(new Model(Optotype.N), new Texture(colorBlue)));
            items.add(new Item(new Model(Optotype.O), new Texture(colorBlue)));
            items.add(new Item(new Model(Optotype.P), new Texture(colorBlue)));
            items.add(new Item(new Model(Optotype.Q), new Texture()));
            items.add(new Item(new Model(Optotype.R), new Texture()));
            items.add(new Item(new Model(Optotype.S), new Texture()));
            items.add(new Item(new Model(Optotype.T), new Texture()));
            items.add(new Item(new Model(Optotype.U), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.V), new Texture(colorRed)));
            items.add(new Item(new Model(Optotype.W), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.X), new Texture(colorGreen)));
            items.add(new Item(new Model(Optotype.Y), new Texture(colorBlue)));
            items.add(new Item(new Model(Optotype.Z), new Texture(colorBlue)));
            for (Item optotype : items) optotype.size(1.25, 1.25);
        }

        private void addCircle() {
            Item circle = new Item(new Model(ModelType.CIRCLE), new Texture("ecceIvanito.jpeg"));
            circle.size(8, 8);
            items.add(circle);
        }

        private void addTriangle() {
            Item triangle = new Item(new Model(ModelType.TRIANGLE),
                    new Texture(TextureType.SINE, triangleColor1, triangleColor2));
            triangle.position(5, 5);
            triangle.size(2, 2);
            items.add(triangle);
        }

        private void addSquares() {
            double[] color1 = new double[] {1, 0, 0, 0.5};
            double[] color2 = new double[] {0, 0, 1, 0.5};
            Item square1 = new Item(new Model(ModelType.SQUARE), new Texture(color1));
            Item square2 = new Item(new Model(ModelType.SQUARE), new Texture(color2));
            square1.position(-10, -12);
            square2.position(-9, -12);
            square1.size(0.05, 10);
            square2.size(0.05, 10);
            items.add(square1);
            items.add(square2);
        }

        private void addText() {
            double[] textColor = new double[] {1, 0, 1, 1};
            text = new Text(textColor);
            text.setText("Optotypes and basic shapes moving");
            text.size(1.25);
            text.position(-14, 12);
            items.add(text);
            text = new Text();
            text.setText("Refresh rate: ");
            text.size(1);
            text.position(-22, 11);
            items.add(text);
        }

    }

}
