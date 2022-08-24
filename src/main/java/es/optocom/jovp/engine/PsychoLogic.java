package es.optocom.jovp.engine;

import es.optocom.jovp.engine.structures.Command;

/**
 *
 * PsychoLogic
 *
 * <ul>
 * <li>Logic for Psychophysics Experiments</li>
 * Interface with defaults to run psychophysics experiments with the engine
 * </ul>
 *
 * @since 0.0.1
 */
public interface PsychoLogic {

    Items items = new Items();

    /**
     *
     * Initializes the engine
     *
     * @param psychoEngine The engine to retrieve info as required
     *
     * @since 0.0.1
     */
    void init(PsychoEngine psychoEngine);

    /**
     *
     * Reads input from controller
     *
     * @since 0.0.1
     */
    void input(Command command, double time);

    /**
     *
     * Updates state
     *
     * @param psychoEngine The engine to retrieve info as required
     *
     * @since 0.0.1
     */
    void update(PsychoEngine psychoEngine);

}