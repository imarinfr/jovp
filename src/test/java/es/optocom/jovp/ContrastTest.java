package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.ModelType;
import es.optocom.jovp.structures.TextureType;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests for different contrasts and changes
 *
 * @since 0.0.1
 */
public class ContrastTest {

  /**
   * Unitary tests for different contrasts and changes
   *
   * @since 0.0.1
   */
  public ContrastTest() {
  }

  /**
   * Patterns and their spatial properties
   *
   * @since 0.0.1
   */
  @Test
  public void funWithContrast() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 1000);
    psychoEngine.getWindow().getMonitor().setPhysicalSize(535, 295);
    psychoEngine.start();
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

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

    @Override
    public void init(PsychoEngine psychoEngine) {
      Item item = new Item(new Model(ModelType.MALTESE), new Texture(fixation));
      item.size(1, 1);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE, black, white));
      item.position(14, 4);
      item.frequency(0, 3);
      item.rotation(45);
      item.contrast(0.5);
      items.add(item);
      item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.SQUARESINE, black, white));
      item.position(6, -3);
      item.frequency(0.25, 2);
      item.size(6, 3);
      item.contrast(0.1);
      items.add(item);
      item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD, black, white));
      item.position(5, 3);
      item.frequency(0.5, 3, 0.25, 2);
      item.size(4, 2);
      item.rotation(75);
      items.add(item);
      item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD, black, white));
      item.position(9, 0);
      item.frequency(0.25, 0.5);
      item.size(15, 12);
      items.add(item);
      stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE, black, white));
      stimulus1.position(-8, -4);
      stimulus1.size(6, 6);
      stimulus1.frequency(0, 0.5);
      stimulus1.rotation(45);
      items.add(stimulus1);
      stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
      stimulus2.frequency(0, 2);
      stimulus2.position(-12, 2);
      stimulus2.size(6, 3);
      items.add(stimulus2);
      stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE, red, green));
      stimulus3.frequency(0, 2);
      stimulus3.position(-3, 0);
      stimulus3.size(2, 2);
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
      // Start timers
      timer.start();
      timerFps.start();
    }

    @Override
    public void input(Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
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
        text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
        fps = 0;
      }
    }

  }

}
