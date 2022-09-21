package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.structures.*;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests for stereoscopic presentation
 *
 * @since 0.0.1
 */
public class StereoTest {

  /**
   * Unitary tests for stereoscopic presentation
   *
   * @since 0.0.1
   */
  public StereoTest() {
  }

  /**
   * Patterns and their spatial properties
   *
   * @since 0.0.1
   */
  @Test
  public void stereoTest() {
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500, ViewMode.STEREO);
    psychoEngine.getWindow().getMonitor().setPhysicalSize(535, 295);
    // psychoEngine.setFullScreen();
    psychoEngine.start();
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    double[] fixationColor = new double[] { 0, 1, 0, 1 };
    double[] backgroundColor = new double[] { 0.4, 0.4, 0.4, 1 };

    Item background, fixation, stimulus1, stimulus2, stimulus3;
    Timer timer = new Timer();
    Timer timerFps = new Timer();
    int fps = 0;
    Text text;
    int refreshTime = 1000;

    @Override
    public void init(PsychoEngine psychoEngine) {
      background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor)); // background
      background.position(0, 0, 90);
      items.add(background);
      fixation = new Item(new Model(ModelType.MALTESE), new Texture(fixationColor)); // fixation
      fixation.size(2);
      items.add(fixation);
      stimulus1 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
      stimulus1.position(-3, -3);
      stimulus1.size(4.5, 4.5);
      stimulus1.frequency(0, 0.5);
      stimulus1.rotation(45);
      stimulus1.contrast(0.75);
      items.add(stimulus1);
      stimulus2 = new Item(new Model(ModelType.CIRCLE), new Texture(TextureType.SINE));
      stimulus2.frequency(0, 2);
      stimulus2.position(3, 2);
      stimulus2.size(3, 1.5);
      stimulus2.eye(Eye.LEFT);
      stimulus2.contrast(0.25);
      items.add(stimulus2);
      stimulus3 = new Item(new Model(ModelType.ANNULUS, 0.5f), new Texture(TextureType.SINE));
      stimulus3.eye(Eye.RIGHT);
      stimulus3.frequency(0, 2);
      stimulus3.position(3, -2);
      stimulus3.size(2, 2);
      stimulus3.contrast(0.5);
      items.add(stimulus3);
      // Add title
      Text title = new Text();
      title.setText("Stereoscopic view");
      title.eye(Eye.LEFT);
      title.size(0.5);
      title.position(-5, 5);
      items.add(title);
      // Add text to show FPS
      text = new Text();
      text.setText("Refresh rate:");
      text.eye(Eye.LEFT);
      text.size(0.4);
      text.position(-5, 4.5);
      items.add(text);
      // Start timer
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
      double[] fov = psychoEngine.getFieldOfView();
      background.size(fov[0], fov[1]);
      double time = timer.getElapsedTime();
      stimulus1.contrast(Math.sin(time / 1000.0) / 2 + 0.5);
      stimulus3.contrast(Math.sin(time / 200.0) / 2 + 0.5);
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
