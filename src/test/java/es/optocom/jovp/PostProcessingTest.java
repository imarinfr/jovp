package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.ModelType;
import es.optocom.jovp.structures.PostType;
import es.optocom.jovp.structures.TextureType;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests for item post-processing
 *
 * @since 0.0.1
 */
public class PostProcessingTest {

  /**
   * Unitary tests for item post-processing
   *
   * @since 0.0.1
   */
  public PostProcessingTest() {
  }

  /**
   * Shows optotypes
   *
   * @since 0.0.1
   */
  @Test
  public void showAllOptotypes() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.getWindow().getMonitor().setPhysicalSize(621, 341);
    psychoEngine.start();
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    Text text;
    int fps = 0;
    Timer timer = new Timer();
    Timer timerFps = new Timer();
    int refreshTime = 500;

    double[] fixation = new double[] { 0, 1, 0, 1 };
    double[] red = new double[] { 1, 0, 0, 1 };
    double[] green = new double[] { 0, 1, 0, 1 };
    double[] blue = new double[] { 0, 0, 1, 1 };
    boolean inverted = false;
    double envelope1 = 1;
    double[] envelope2 = new double[] { 1, 0.75 };
    double envelope3 = 0.5;
    double[] envelope4 = new double[] { 0.4, 1.2 };

    @Override
    public void init(PsychoEngine psychoEngine) {
      Item item = new Item(new Model(ModelType.MALTESE), new Texture(fixation));
      item.size(1, 1);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.G1));
      item.position(3, -2);
      item.size(2, 6);
      item.frequency(0.5, 0.25);
      item.envelope(PostType.GAUSSIAN, envelope4[0], envelope4[1]);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.G2));
      item.position(-3, -2);
      item.size(2, 6);
      item.frequency(0.5, 0.25);
      item.envelope(PostType.GAUSSIAN, envelope4[0], envelope4[1]);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture());
      item.position(-7.75, -3.25, 20);
      item.size(2, 2);
      item.envelope(PostType.GAUSSIAN, 0.25);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
      item.position(-10, 0);
      item.size(10);
      item.frequency(0.5, 0.5);
      item.envelope(PostType.GAUSSIAN, envelope1);
      items.add(item);
      item = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
      item.position(10, 0);
      item.size(10);
      item.frequency(0.5, 0.5);
      item.defocus(1);
      items.add(item);
      item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.SQUARESINE, green, red));
      item.position(0, -4);
      item.size(3, 3);
      item.frequency(0, 0.5);
      item.rotation(90);
      item.envelope(PostType.GAUSSIAN, envelope3);
      items.add(item);
      item = new Item(new Model(ModelType.SQUARE), new Texture(TextureType.CHECKERBOARD, red, blue));
      item.position(-2, 4);
      item.size(3, 3);
      item.frequency(0.5, 0.5);
      item.envelope(PostType.CIRCLE, envelope2[0], envelope2[1], 45);
      items.add(item);
      item = new Item(new Model(ModelType.ANNULUS, 0.3f), new Texture(TextureType.G3));
      item.position(2, 4);
      item.size(3, 3);
      item.frequency(0, 1);
      items.add(item);
      // Add title
      Text title = new Text();
      title.setText("Post-processing testing");
      title.size(1.5);
      title.position(-8, 8);
      items.add(title);
      // Add text to show FPS
      text = new Text();
      text.setText("Refresh rate:");
      text.size(0.75);
      text.position(-15, 7);
      items.add(text);
      timer.start();
      // Start timers
      timer.start();
      timerFps.start();
    }

    @Override
    public void input(Command command, double time) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
      double time = timer.getElapsedTime();
      items.get(1).rotation(time / 10.0);
      items.get(2).rotation(-time / 10.0);
      double theta = time / 500.0;
      items.get(3).position(3.75 * Math.cos(theta) - 10, 3.75 * Math.sin(theta));
      items.get(4).rotation(-time / 20.0);
      if (timerFps.getElapsedTime() <= refreshTime)
        fps++;
      else {
        if (inverted) {
          for (Item item : items) {
            if (item.getTexture().getType() == TextureType.CHECKERBOARD)
              item.envelope(PostType.CIRCLE, envelope2[0], envelope2[1], 45);
            if (item.getTexture().getType() == TextureType.SQUARESINE)
              item.removeEnvelope();
          }
        } else {
          for (Item item : items) {
            if (item.getTexture().getType() == TextureType.CHECKERBOARD)
              item.removeEnvelope();
            if (item.getTexture().getType() == TextureType.SQUARESINE)
              item.envelope(PostType.GAUSSIAN, envelope3);
          }
        }
        inverted = !inverted;
        timerFps.start();
        text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
        fps = 0;
      }
    }

  }

}
