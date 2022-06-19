package es.optocom.jovp.unit;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.Timer;
import es.optocom.jovp.engine.rendering.*;
import es.optocom.jovp.engine.structures.*;
import org.junit.jupiter.api.Test;

/**
 * OptotypesTest
 *
 * <ul>
 * <li>OptotypesTest test</li>
 * Unitary tests for the optotype rendering
 * </ul>
 *
 * @since 0.0.1
 */
public class OptotypesTest {

    /**
     * Shows text and other items
     *
     * @since 0.0.1
     */
    @Test
    public void showAllOptotypes() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        double theta;
        Timer timer = new Timer();

        @Override
        public void init() {
            double[] colorRed = new double[]{1, 0, 0, 1};
            double[] colorGreen = new double[]{0, 1, 0, 1};
            double[] colorBlue = new double[]{0, 0, 1, 1};
            double[] textColor = new double[]{1, 0, 1, 0.6};
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
            float xpos = -5.3f;
            for (Item optotype : items) {
                optotype.position(xpos, 0.0f);
                optotype.size(0.15f, 0.15f);
                xpos += 0.425f;
            }
            // Add title
            Text title = new Text(textColor);
            title.setText("Optotypes test");
            title.setSize(1.0f);
            title.position(0.0f, -9.0f);
            items.add(title);
            // Start timer
            timer.start();
        }

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update() {
            theta -= 5;
            float xpos = -5.3f;
            float ypos = 0.0f;
            double time = timer.getElapsedTime();
            for (Item item : items) {
                if (item.getModel().getType() == ModelType.OPTOTYPE) {
                    item.position(xpos, ypos);
                    item.rotation(theta);
                    xpos += 0.425f;
                    if (time >= 100) {
                        double[] color = item.getColor();
                        color[0] = 1.0f;
                        item.setColor(color);
                        timer.start();
                    }
                }
            }
        }

    }

}
