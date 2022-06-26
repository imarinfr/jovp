package es.optocom.jovp.unit;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.Timer;
import es.optocom.jovp.engine.rendering.Item;
import es.optocom.jovp.engine.rendering.Model;
import es.optocom.jovp.engine.rendering.Text;
import es.optocom.jovp.engine.rendering.Texture;
import es.optocom.jovp.engine.structures.Command;
import es.optocom.jovp.engine.structures.ModelType;
import es.optocom.jovp.engine.structures.TextureType;
import es.optocom.jovp.engine.structures.ViewMode;
import org.junit.jupiter.api.Test;

/**
 *
 * Testing stereoscopic presentation
 *
 * @since 0.0.1
 */
public class StereoTest {

    /**
     *
     * Patterns and their spatial properties
     *
     * @since 0.0.1
     */
    @Test
    public void funWithContrast() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500, ViewMode.MONO);
        psychoEngine.getWindow().getMonitor().setPhysicalSize(621, 341);
        psychoEngine.start();
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        double[] fixation = new double[] {0, 1, 0, 1};

        Item stimulus1, stimulus2, stimulus3;
        Timer timer = new Timer();
        Timer timerFps = new Timer();
        int fps = 0;
        Text text;
        int refreshTime = 1000;

        @Override
        public void init() {
            Item item = new Item(new Model(ModelType.MALTESE), new Texture(fixation));
            item.size(1, 1);
            items.add(item);
            stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus1.position(-8, -4);
            stimulus1.size(6, 6);
            stimulus1.frequency(0, 0.5);
            stimulus1.rotation(45);
            stimulus1.contrast(0.75);
            items.add(stimulus1);
            stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
            stimulus2.frequency(0, 2);
            stimulus2.position(-12, 2);
            stimulus2.size(6, 3);
            stimulus1.contrast(0.25);
            items.add(stimulus2);
            stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
            stimulus3.frequency(0, 2);
            stimulus3.position(-3, 0);
            stimulus3.size(2, 2);
            stimulus1.contrast(0.5);
            items.add(stimulus3);
            // Add title
            Text title = new Text();
            title.setText("Fun with contrasts");
            title.size(1.5);
            title.position(-5, 8);
            items.add(title);
            // Add text to show FPS
            text = new Text();
            text.setText("Refresh rate:");
            text.size(0.75);
            text.position(-15, 7);
            items.add(text);
            // Start timer
            timer.start();
            timerFps.start();
        }

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update() {
            double time = timer.getElapsedTime();
            stimulus1.contrast(Math.sin(time / 1000.0) / 2 + 0.5);
            stimulus3.contrast(Math.sin(time / 200.0) / 2 + 0.5);
            stimulus1.frequency(Math.sin(time / 250.0), 0.5);
            stimulus1.rotation(time / 10.0);
            stimulus2.rotation(-time / 20.0);
            stimulus2.texRotation(time / 5.0);
            if (timerFps.getElapsedTime() <= refreshTime) fps++;
            else { // restart the timer every second
                timerFps.start();
                text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
                fps = 0;
            }
        }

    }
}
