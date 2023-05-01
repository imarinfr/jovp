package es.optocom.jovp;

import org.junit.jupiter.api.Test;

import es.optocom.jovp.definitions.Command;

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
    // TODO portabilityDesktop testing
  }

  /**
   * Mac OSX portability
   *
   * @since 0.0.1
   */
  @Test
  public void portabilityMacOS() {
    // TODO portabilityMacOS testing
  }

  /**
   * iOS portability
   * @since 0.0.1
   */
  @Test
  public void portabilityIOS() {
    // TODO portabilityIOS testing
  }

  /**
   * minimum Vulkan portability
   * 
   * @since 0.0.1
   */
  @Test
  public void portabilityMinimumVulkan() {
    // TODO portabilityMinimumVulkan testing
  }

  /**
   * minimum Vulkan specifications
   *
   * @since 0.0.1
   */
  @Test
  public void specMinimumVulkan() {
    // TODO specMinimumVulkan testing
  }

  // Psychophysics logic class
  static class Logic implements PsychoLogic {

    @Override
    public void init(PsychoEngine psychoEngine) {
    }

    @Override
    public void input(PsychoEngine psychoEngine, Command command) {
      if (command != Command.NONE)
        System.out.println(command);
    }

    @Override
    public void update(PsychoEngine psychoEngine) {
    }

  }

}
