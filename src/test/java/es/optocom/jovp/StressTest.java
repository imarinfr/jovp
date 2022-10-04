package es.optocom.jovp;

import es.optocom.jovp.definitions.*;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests for different patterns and textures
 *
 * @since 0.0.1
 */
public class StressTest {

  /**
   * Unitary tests for different patterns and textures
   *
   * @since 0.0.1
   */
  public StressTest() {
  }

  /**
   * Shows all optotypes moving awkwardly
   *
   * @since 0.0.1
   */
  @Test
  public void movingShapesAround() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.start("mouse", Paradigm.CLICKER);
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

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

    @Override
    public void init(PsychoEngine psychoEngine) {
      addOptotypes();
      for (int i = 0; i < 15; i++) addPolygon(i);
      for (int i = 0; i < 20; i++) addCircle(i);
      addTriangle();
      addSquares();
      addText();
      circle = new Item(new Model(ModelType.CIRCLE), new Texture("ecceIvanito.jpeg"));
      circle.position(0, ypos, zpos);
      circle.size(size, size);
      circle.show(false);
      items.add(circle);
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      timer.start();
      timerFps.start();
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
      double time = timer.getElapsedTime();
      if (time > 13425) {
        // throw myself at observer
        if (time > 14425) {
          circle.show();
          circle.position(0, ypos + Math.sin(time / 100), zpos);
        }
        if (time > 16425) {
          size += 2;
          ypos += 0.6;
          zpos -= 0.5;
          circle.position(0, ypos, zpos);
          circle.size(size, size);
        }
        if (zpos < 2) for (Item item : items) item.show(true);
        return;
      }
      iteration++;
      theta -= 5;
      float xpos = -16;
      int number = 0;
      // process optotypes
      for (Item item : items) {
        if (item.getModel().getType() != ModelType.OPTOTYPE)
          continue;
        double ypos = amplitude * Math.sin((2 * Math.PI * frequency * number / (items.size() - 1)
            * timer.getElapsedTime()) / 500);
        item.position(xpos, ypos);
        item.rotation(theta);
        xpos += 1.25;
        number++;
      }
      // process polygons
      int imageNumber = 0;
      for (Item item : items) {
        if (item.getModel().getType() != ModelType.POLYGON)
          continue;
        int pos = iteration - 38 * imageNumber;
        if (pos < 0)
          pos = 0;
        item.position(rx * Math.cos(pos / 90.0), ry * Math.sin(pos / 90.0), 20);
        item.rotation(-theta / 5, new Vector3f(0.5f, 0.8f, 1.2f));
        imageNumber++;
      }
      // process circles
      imageNumber = 0;
      for (Item item : items) {
        if (item.getModel().getType() != ModelType.CIRCLE)
          continue;
        if (time < 8000)
          continue;
        item.show();
        leadPosition += 1 / 60.0;
        if (imageNumber < 10)
          item.position(leadPosition - 5 * imageNumber, 8, 10);
        if (imageNumber >= 10 & imageNumber < 20)
          item.position(-leadPosition + 5 * (imageNumber - 10), -8, 10);
        imageNumber++;
      }
      // process triangle
      for (Item item : items) {
        if (item.getModel().getType() != ModelType.TRIANGLE)
          continue;
        item.size(4 * Math.sin(iteration / 40.0) + 10, 4 * Math.sin(iteration / 30.0) + 10);
        item.contrast(0.4 * Math.sin(iteration / 50.0) + 0.6);
      }
      // process squares
      int square = 0;
      for (Item item : items) {
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
        text.setText("Refresh rate: " + Math.round(10000.0 * fps / refreshTime) / 10.0 + " fps");
        timerFps.start();
        fps = 0;
      }
    }

    private void addOptotypes() {
      double[] colorWhite = new double[] { 1, 1, 1, 1 };
      double[] colorRed = new double[] { 1, 0, 0, 1 };
      double[] colorGreen = new double[] { 0, 1, 0, 1 };
      double[] colorBlue = new double[] { 0, 0, 1, 1 };
      items.add(new Item(new Model(Optotype.A), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.B), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.C), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.D), new Texture(colorWhite)));
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
      items.add(new Item(new Model(Optotype.Q), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.R), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.S), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.T), new Texture(colorWhite)));
      items.add(new Item(new Model(Optotype.U), new Texture(colorRed)));
      items.add(new Item(new Model(Optotype.V), new Texture(colorRed)));
      items.add(new Item(new Model(Optotype.W), new Texture(colorGreen)));
      items.add(new Item(new Model(Optotype.X), new Texture(colorGreen)));
      items.add(new Item(new Model(Optotype.Y), new Texture(colorBlue)));
      items.add(new Item(new Model(Optotype.Z), new Texture(colorBlue)));
      for (Item optotype : items)
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
      items.add(polygon);
    }

    private void addCircle(int i) {
      String fileName = null;
      if (i % 2 == 0)
        fileName = "ecceIvanito.jpeg";
      if (i % 2 == 1)
        fileName = "ivanito.jpeg";
      Item circle = new Item(new Model(ModelType.CIRCLE), new Texture(fileName));
      circle.size(2.25, 2.25);
      circle.show(false);
      items.add(circle);
    }

    private void addTriangle() {
      Item triangle = new Item(new Model(ModelType.TRIANGLE),
          new Texture(TextureType.SINE, triangleColor1, triangleColor2));
      triangle.position(5, 5);
      triangle.size(2, 2);
      triangle.frequency(0, 1);
      items.add(triangle);
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
      items.add(square1);
      items.add(square2);
    }

    private void addText() {
      double[] textColor = new double[] { 1, 0, 1, 1 };
      text = new Text(textColor);
      text.setText("Optotypes and basic shapes moving");
      text.size(0.75);
      text.position(-5, 5);
      items.add(text);
      text = new Text();
      text.setText("Refresh rate: ");
      text.size(0.6);
      text.position(-5, 4);
      items.add(text);
    }

  }

}
