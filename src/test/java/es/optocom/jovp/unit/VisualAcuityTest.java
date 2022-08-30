package es.optocom.jovp.unit;

import es.optocom.jovp.engine.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.rendering.*;
import es.optocom.jovp.engine.structures.*;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 *
 * Unitary tests for a visual acuity psychophysical experience
 *
 * @since 0.0.1
 */
public class VisualAcuityTest {

    /**
     *
     * Unitary tests for a visual acuity psychophysical experience
     *
     * @since 0.0.1
     */
    public VisualAcuityTest() {}
    /**
     *
     * Performs a visual acuity test
     *
     * @since 0.0.1
     */
    @Test
    public void visualAcuityTest() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500, Input.MOUSE);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        double[] white = new double[] {1, 1, 1, 1};
        double[] black = new double[] {0, 0, 0, 1};

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
        @Override
        public void init(PsychoEngine psychoEngine) {
            // Background
            Item bg = new Item(new Model(ModelType.SQUARE), new Texture(white)); // background
            bg.size(8, 8);
            bg.position(0, 0, 99);
            items.add(bg);
            // Optotype
            optotype = new Item(new Model(Optotype.E), new Texture(black));
            optotype.position(0, 0, 10);
            optotype.size(size, size);
            theta = 180 * random.nextInt(2);
            optotype.rotation(theta);
            items.add(optotype);
            // Title
            Text title = new Text();
            title.setText("Visual Acuity test");
            title.size(0.5);
            title.position(-2, 5);
            items.add(title);
            // Info text
            info = new Text();
            info.setText("VA: " + String.format("%.2f", 12 * size) + " arc min; " +
                    "LogMAR: " + String.format("%.2f", Math.log10(12 * size)) + "; " +
                    "Reversals: " + reversals);
            info.size(0.5);
            info.position(-8, -5);
            items.add(info);
        }

        @Override
        public void input(Command command, double time) {
            if(command != Command.NONE) {
                if (command == Command.ITEM1) nextDeltaSize(180);
                if (command == Command.ITEM2) nextDeltaSize(0);
                theta = 180 * random.nextInt(2);
            }
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
            optotype.size(size);
            optotype.rotation(theta);
            info.setText("VA: " + String.format("%.2f", 12 * size) + " arc min; " +
                    "LogMAR: " + String.format("%.2f", Math.log10(12 * size)) + "; " +
                    "Reversals: " + reversals);
        }

        private void nextDeltaSize(double correctTheta) {
            if (theta == correctTheta) {
                if(!lastSeen) {
                    reversals++;
                    deltaSize = deltaSize / 2;
                }
                size -= deltaSize;
                lastSeen = true;
            } else {
                if(lastSeen) {
                    reversals++;
                    deltaSize = deltaSize / 2;
                }
                size += deltaSize;
                lastSeen = false;
            }
            if(size < minSize) size = minSize;
            if(size > maxSize) size = maxSize;
            if(deltaSize < minDeltaSize) size = minDeltaSize;
        }

    }

}
