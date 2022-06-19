package es.optocom.jovp.portability;

import es.optocom.jovp.PsychoEngine;
import es.optocom.jovp.engine.PsychoLogic;
import es.optocom.jovp.engine.structures.Command;
import es.optocom.jovp.engine.structures.Eye;
import org.junit.jupiter.api.Test;

/**
 * PortabilityTests
 *
 * <ul>
 * <li>Portability test</li>
 * Unitary tests to check portability
 * </ul>
 *
 * @since 0.0.1
 */
public class PortabilityTests {

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityDesktop() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityMacOS() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityIOS() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void portabilityMinimumVulkan() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        psychoEngine.cleanup();
    }

    /**
     * TODO
     *
     * @since 0.0.1
     */
    @Test
    public void specMinimumVulkan() {
        PsychoEngine psychoEngine = new PsychoEngine(new Logic(), Eye.BOTH, 500) ;
        psychoEngine.cleanup();
    }

    // Psychophysics logic class
    static class Logic implements PsychoLogic {

        @Override
        public void init() {}

        @Override
        public void input(Command command, double time) {
            if (command != Command.NONE) System.out.println(command);
        }

        @Override
        public void update() {
        }

    }

}
