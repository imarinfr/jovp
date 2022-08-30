package es.optocom.opi.jovp.engine;

import es.optocom.opi.jovp.engine.rendering.Item;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * List of items for the psychophysics experience
 *
 * @since 0.0.1
 */
public class Items implements Iterable<Item> {

    private ArrayList<Item> items;

    /**
     *
     * Init Items
     *
     * @since 0.0.1
     */
    public Items() {
        items = new ArrayList<>();
    }

    /**
     *
     * Add an item
     *
     * @param item The item to add
     *
     * @return Whether the item was succesfully added
     *
     * @since 0.0.1
     */
    public boolean add(@NotNull Item item) {
        return items.add(item);
    }

    /**
     *
     * Remove an item
     *
     * @param item The item to remove
     *
     * @since 0.0.1
     */
    public void remove(@NotNull Item item) {
        items.remove(item);
    }

    /**
     *
     * Get the number of items
     *
     * @return Number of items in the list
     *
     * @since 0.0.1
     */
    public int size() {
        return items.size();
    }

    /**
     *
     * Get an item
     *
     * @param index Index to retrieve
     *
     * @return an item
     *
     * @since 0.0.1
     */
    public Item get(int index) {
        return items.get(index);
    }

    /**
     *
     * Iterate over items
     *
     * @since 0.0.1
     */
    public Iterator<Item> iterator() {
        return items.iterator();
    }

    /**
     *
     * Clean up and empty the list of vulkanObjects
     *
     * @since 0.0.1
     */
    public void destroy() {
        for (Item item: items) item.destroy();
        items = new ArrayList<>();
    }

}
