package es.optocom.jovp.engine;

import es.optocom.jovp.engine.rendering.Item;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * Items
 *
 * <ul>
 * <li>Items</li>
 * List of items for the psychophysics experience
 * </ul>
 *
 * @since 0.0.1
 */
public class Items implements Iterable<Item> {

    private ArrayList<Item> items;
    private ArrayList<Item> delete;
    private boolean update = false;

    /**
     *
     * Init Items
     *
     * @since 0.0.1
     */
    public Items() {
        items = new ArrayList<>();
        delete = new ArrayList<>();
    }

    /**
     *
     * Add an item
     *
     * @param item The item to add
     *
     * @since 0.0.1
     */
    public boolean add(@NotNull Item item) {
        update = true;
        item.create();
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
        update = true;
        delete.add(item);
        items.remove(item);
    }

    /**
     *
     * Destroy items flagged for deletion
     *
     * @since 0.0.1
     */
    public void delete() {
        for (Item item : delete) item.destroy();
        delete = new ArrayList<>();
        update = false;
    }
    /**
     *
     * @return Whether command buffers need to be updated
     *
     * @since 0.0.1
     */
    public boolean update() {
        // check if any of the items have changed
        for (Item item : items) if(item.update()) {
            update = true;
            item.updateBuffers();
            break;
        }
        return update;
    }

    /**
     *
     * @param update Whether command buffers need to be updated
     *
     * @since 0.0.1
     */
    public void update(boolean update) {
        this.update = update;
    }

    /**
     *
     * Get the number of items
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
        items = null;
    }

}
