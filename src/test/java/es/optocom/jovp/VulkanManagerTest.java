package es.optocom.jovp;

import es.optocom.jovp.definitions.Command;
import es.optocom.jovp.definitions.Eye;
import es.optocom.jovp.definitions.ModelType;
import es.optocom.jovp.definitions.Paradigm;
import es.optocom.jovp.definitions.TextureType;
import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Model;
import es.optocom.jovp.rendering.Text;
import es.optocom.jovp.rendering.Texture;
import es.optocom.jovp.rendering.VulkanManager;

import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 
 * Unitary tests for the Vulkan manager
 *
 * @since 0.0.1
 */
public class VulkanManagerTest {

  /**
   * 
   * Unitary tests for the Vulkan manager
   *
   * @since 0.0.1
   */
  public VulkanManagerTest() {
  }

  /**
   * 
   * Gets information about physical devices (GPUs)
   *
   * @since 0.0.1
   */
  @Test
  public void physicalDevicesInformation() {
    PsychoEngine psychoEngine = new PsychoEngine(new LogicDummy());
    VulkanManager vulkanManager = psychoEngine.getVulkanManager();
    List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
    for (VkPhysicalDevice physicalDevice : physicalDevices)
      System.out.println(vulkanManager.getPhysicalDeviceProperties(physicalDevice));
    psychoEngine.cleanup();
  }

  /**
   * 
   * Gets information about physical devices (GPUs)
   *
   * @since 0.0.1
   */
  @Test
  public void swapChainInformation() {
    PsychoEngine psychoEngine = new PsychoEngine(new LogicDummy());
    VulkanManager vulkanManager = psychoEngine.getVulkanManager();
    List<VkPhysicalDevice> physicalDevices = vulkanManager.getPhysicalDevices();
    for (VkPhysicalDevice physicalDevice : physicalDevices)
      System.out.println(vulkanManager.getSwapChainSupport(physicalDevice));
    psychoEngine.cleanup();
  }

  /**
   * 
   * Render a triangle
   *
   * @since 0.0.1
   */
  @Test
  public void showTriangle() {
    PsychoEngine psychoEngine = new PsychoEngine(new LogicTriangle());
    System.out.println(Arrays.toString(psychoEngine.getFieldOfView()));
    psychoEngine.setPhysicalSize(621, 341);
    System.out.println(Arrays.toString(psychoEngine.getFieldOfView()));
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

    // Psychophysics logic that does nothing
    static class LogicDummy implements PsychoLogic {

      @Override
      public void init(PsychoEngine psychoEngine) {
      }
  
      @Override
      public void input(PsychoEngine psychoEngine, Command command) {
      }
  
      @Override
      public void update(PsychoEngine psychoEngine) {
      }
  
    }
  
  // Psychophysics logic to show a simple triangle
  static class LogicTriangle implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
      Item item = new Item(new Model(ModelType.TRIANGLE), new Texture(new double[] {1, 1, 1, 1}));
      item.position(0, 0);
      item.size(0.1, 0.1);
      item.rotation(0);
      view.add(item);
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE) System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

  // Psychophysics logic to show a simple triangle
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
    ModelType[] models = {ModelType.CIRCLE, ModelType.SQUARE, ModelType.TRIANGLE, ModelType.ANNULUS, ModelType.OPTOTYPE};
    TextureType[] textures = {TextureType.CHECKERBOARD, TextureType.SINE, TextureType.G1, TextureType.G2, TextureType.G3};
    double[] backgroundColor = new double[] {0.5, 0.5, 0.5, 1};
    double[] color0 = new double[] {1, 1, 1, 1};
    double[] color1 = new double[] {0, 0, 0.5, 1};

    @Override
    public void init(PsychoEngine psychoEngine) {
      // Background
      background = new Item(new Model(ModelType.CIRCLE), new Texture(backgroundColor));
      background.position(0,0, 100);
      float[] fov = psychoEngine.getFieldOfView();
      background.size(fov[0],fov[1]);
      view.add(background);
      // Title
      Text title = new Text();
      title.setText("Blinking items");
      title.size(1.5);
      title.position(-5, 8);
      view.add(title);
      // Add text to show FPS
      text = new Text();
      text.setText("Refresh rate:");
      text.size(1);
      text.position(-15, 6.5);
      view.add(text);
      // Items
      item1 = new Item(new Model(ModelType.CIRCLE), new Texture(color0, color1));
      item1.position(0, 0, 90);
      item1.size(10, 10);
      view.add(item1);
      item2 = new Item(new Model(ModelType.MALTESE), new Texture(new double[] {0, 1, 0, 1}));
      item2.position(0, 0, 80);
      item2.size(2, 2);
      view.add(item2);
      timer.start();
      blinkTimer.start();
      modelTimer.start();
      textureTimer.start();
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
      float[] fov = psychoEngine.getFieldOfView();
      background.size(fov[0],fov[1]);
      item1.rotation(timer.getElapsedTime() / 20);
      double cpd = 0.5 * (Math.cos(timer.getElapsedTime() / 1500) + 1) / 2;
      item1.frequency(0, cpd);
      if(modelTimer.getElapsedTime() > updateModelTime) {
        item1.update(new Model(models[ThreadLocalRandom.current().nextInt(0, 5)]));
        modelTimer.start();
      }
      if(textureTimer.getElapsedTime() > updateTextureTime) {
        item1.update(new Texture(textures[ThreadLocalRandom.current().nextInt(0, 5)], color0, color1));
        textureTimer.start();
      }
      if(blinkTimer.getElapsedTime() > blinkItemTime) {
        if (item2.eye() == Eye.BOTH) item2.eye(Eye.NONE);
        else item2.eye(Eye.BOTH);
        blinkTimer.start();
      }
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
