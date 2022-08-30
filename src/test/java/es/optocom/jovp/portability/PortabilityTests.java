package es.optocom.jovp.portability;

import es.optocom.jovp.engine.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.structures.Command;
import org.junit.jupiter.api.Test;

/**
 *
 * Unitary tests to check portability
 *
 * @since 0.0.1
 */
public class PortabilityTests {

    /**
     *
     * Unitary tests to check portability
     *
     * @since 0.0.1
     */
    public PortabilityTests() {}

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityDesktop() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityMacOS() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityIOS() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityMinimumVulkan() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void specMinimumVulkan() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), 500) ;
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        @Override
        public void init(PsychoEngine psychoEngine) {}

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update(PsychoEngine psychoEngine) {
        }

    }

}
