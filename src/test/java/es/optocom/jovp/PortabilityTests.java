package es.optocom.jovp;

import es.optocom.jovp.structures.Command;
import org.junit.jupiter.api.Test;

/**
 * Unitary tests to check portability
 *
 * @since 0.0.1
 */
public class PortabilityTests {

  /**
   * Unitary tests to check portability
   *
   * @since 0.0.1
   */
  public PortabilityTests() {
  }

  /**
   * Desktop portabilibty
   * 
   * @since 0.0.1
   */
  @Test
  public void portabilityDesktop() {
    // TODO
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.cleanup();
  }

  /**
   * Mac OSX portability
   *
   * @since 0.0.1
   */
  @Test
  public void portabilityMacOS() {
    // TODO
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.cleanup();
  }

  /**
   * iOS portability
   * @since 0.0.1
   */
  @Test
  public void portabilityIOS() {
    // TODO
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.cleanup();
  }

  /**
   * minimum Vulkan portability
   * 
   * @since 0.0.1
   */
  @Test
  public void portabilityMinimumVulkan() {
    // TODO
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.cleanup();
  }

  /**
   * minimum Vulkan specifications
   *
   * @since 0.0.1
   */
  @Test
  public void specMinimumVulkan() {
    // TODO
    PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500);
    psychoEngine.cleanup();
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(Command command, double time) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
