package es.optocom.jovp;

import es.optocom.jovp.definitions.Command;

/**
 * Logic for Psychophysics Experiments. Interface with defaults to run
 * psychophysics experiments with the engine
 *
 * @since 0.0.1
 */
public interface PsychoLogic {

  /** Items for the psychophysics experience */
  Items items = new Items();

  /**
   * Initializes the engine
   *
   * @param psychoEngine The engine to retrieve info as required
   *
   * @since 0.0.1
   */
  void init(PsychoEngine psychoEngine);

  /**
   * Reads input from controller
   *
   * @param psychoEngine The engine to retrieve info as required
   * @param command The command received
   * 
   * @since 0.0.1
   */
  void input(PsychoEngine psychoEngine, Command command);

  /**
   * Updates state
   *
   * @param psychoEngine The engine to retrieve info as required
   *
   * @since 0.0.1
   */
  void update(PsychoEngine psychoEngine);

}