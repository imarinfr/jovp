package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.structures.Command;
import es.optocom.jovp.structures.ModelType;
import es.optocom.jovp.structures.Optotype;
import es.optocom.jovp.structures.TextureType;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests for the optotype rendering
 *
 * @since 0.0.1
 */
public class OptotypesTest {

  /**
   * Unitary tests for the optotype rendering
   *
   * @since 0.0.1
   */
  public OptotypesTest() {
  }

  /**
   * Shows optotypes
   *
   * @since 0.0.1
   */
  @Test
  public void showAllOptotypes() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.start();
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

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

    @Override
    public void init(PsychoEngine psychoEngine) {
      items.add(new Item(new Model(Optotype.A), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.B), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.C), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.D), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.E), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.F), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.G), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.H), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.I), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.J), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.K), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.L), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.M), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.N), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.O), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.P), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.Q), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.R), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.S), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.T), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.U), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.V), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.W), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.X), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      items.add(new Item(new Model(Optotype.Y), new Texture(TextureType.SINE, colorBlue, colorYellow)));
      items.add(new Item(new Model(Optotype.Z), new Texture(TextureType.CHECKERBOARD, colorRed, colorGreen)));
      float xpos = initialPos;
      for (Item item : items) {
        item.position(xpos, 0.0f);
        item.size(size, size);
        xpos += spacing;
      }
      // Add title
      Text title = new Text();
      title.setText("Optotype test");
      title.size(1.5);
      title.position(-3, 8);
      items.add(title);
      // Add text to show FPS
      text = new Text(textColor1);
      text.setText("Refresh rate:");
      text.size(0.75);
      text.position(-15, 7.5);
      items.add(text);
      timer.start();
      // Start timer
      timer.start();
    }

    @Override
    public void input(Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
      theta -= 5;
      float xpos = initialPos;
      float ypos = 0.0f;
      for (Item item : items) {
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
          for (Item item : items) {
            if (item.getTexture().getType() == TextureType.CHECKERBOARD)
              item.setColors(colorGreen, colorRed);
            if (item.getTexture().getType() == TextureType.SINE)
              item.setColors(colorYellow, colorBlue);
          }
          text.setColor(textColor2);
        } else {
          for (Item item : items) {
            if (item.getTexture().getType() == TextureType.CHECKERBOARD)
              item.setColors(colorRed, colorGreen);
            if (item.getTexture().getType() == TextureType.SINE)
              item.setColors(colorBlue, colorYellow);
          }
          text.setColor(textColor1);
        }
        inverted = !inverted;
        timer.start();
        text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
        fps = 0;
      }
    }

  }

}
