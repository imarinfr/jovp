package es.optocom.jovp;

import es.optocom.jovp.rendering.Item;
import es.optocom.jovp.rendering.Text;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * View for the PsychoEngine. It contains an Observer, a list of Items presented to
 * both eyes (in MONO and STEREO modes) or only the left or right eye (STEREO
 * MODE), the distortion of the Optics of the system displaying the Items, and the
 * point of view, which should be centered at (0,0,0) and looking at the z axis (0,0,1)
 * for running visual experiments, but it can be modified for illustration purposes
 * by showing around the virtual world.
 *
 * @since 0.0.1
 */
public class View {

    ArrayList<Item> items;
    ArrayList<Text> texts;

    /**
     *
     * Init View
     *
     * @since 0.0.1
     */
    public View() {
        items = new ArrayList<>();
        texts = new ArrayList<>();
    }

    /**
     * 
     * Get list of items
     *
     * @return all items
     *
     * @since 0.0.1
     */
    public ArrayList<Item> items() {
        return items;
    }

    /**
     * 
     * Get list of text objects
     *
     * @return all text objects
     *
     * @since 0.0.1
     */
    public ArrayList<Text> texts() {
        return texts;
    }

    /**
     * 
     * Clean up and empty the list of vulkanObjects
     *
     * @since 0.0.1
     */
    public void destroy() {
        for (Item item : items) item.destroy();
        for (Text text : texts) text.destroy();
        items = new ArrayList<>();
        texts = new ArrayList<>();
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
    public boolean add(Item item) {
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
    public void remove(Item item) {
        items.remove(item);
        item.destroy();
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
    public Item item(int index) {
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
     * Add a text object
     *
     * @param text The text to add
     *
     * @return Whether the text was succesfully added
     *
     * @since 0.0.1
     */
    public boolean add(Text text) {
        return texts.add(text);
    }

    /**
     * 
     * Remove an text object
     *
     * @param text The text to remove
     *
     * @since 0.0.1
     */
    public void remove(Text text) {
        texts.remove(text);
        text.destroy();
    }

    /**
     * 
     * Get the number of text objects
     *
     * @return Number of text objects in the list
     *
     * @since 0.0.1
     */
    public int textSize() {
        return texts.size();
    }

    /**
     * 
     * Get a text object
     *
     * @param index Index to retrieve
     *
     * @return a text object
     *
     * @since 0.0.1
     */
    public Text text(int index) {
        return texts.get(index);
    }

    /**
     * 
     * Iterate over text objects
     *
     * @since 0.0.1
     */
    public Iterator<Text> textIter() {
        return texts.iterator();
    }

}