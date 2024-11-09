package es.optocom.jovp;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import static org.lwjgl.glfw.GLFW.glfwGetMonitors;

/**
 * Retrieves all attached monitors and manages them.
 *
 * @since 0.0.1
 */
public class MonitorManager {

    private final int numberOfMonitors;
    private final List<Monitor> monitors = new ArrayList<>();

    /**
     * The monitor manager
     *
     * @since 0.0.1
     */
    public MonitorManager() {
        PointerBuffer glfwMonitors = glfwGetMonitors();
        if (glfwMonitors == null)
            throw new RuntimeException("No monitors could be retrieved");
        numberOfMonitors = glfwMonitors.limit();
        for (int i = 0; i < numberOfMonitors; i++)
            monitors.add(new Monitor(glfwMonitors.get(i)));
    }

    /**
     * Sets the monitor for the window
     *
     * @param monitor Monitor number
     *
     * @return The monitor
     *
     * @since 0.0.1
     */
    public Monitor getMonitor(int monitor) {
        if (monitor < 0 || monitor >= numberOfMonitors)
            throw new RuntimeException("Monitor selected " + monitor + " does not exists." +
                    "Select a monitor number between 0 and " + (numberOfMonitors - 1));
        return monitors.get(monitor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
     public String toString() {
        StringBuilder monitorInformation = new StringBuilder("Number of monitors: " + numberOfMonitors + "\n");
        // get string with Monitor's info
        for (int monitor = 0; monitor < numberOfMonitors; monitor++) {
            monitorInformation.append(getMonitor(monitor).toString());
        }
        return monitorInformation.toString();
    }

}