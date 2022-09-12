package es.optocom.jovp;

import es.optocom.jovp.structures.Command;

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
   * @param command The command received
   * @param time    The time the command was received
   *
   * @since 0.0.1
   */
  void input(Command command, double time);

  /**
   * Updates state
   *
   * @param psychoEngine The engine to retrieve info as required
   *
   * @since 0.0.1
   */
  void update(PsychoEngine psychoEngine);

}