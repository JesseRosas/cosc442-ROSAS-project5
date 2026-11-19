package game;

/**
 * Read-only view of a creature's inventory.
 */
public interface InventoryView {
    Item[] getItems();
    Item get(int i);
    boolean isFull();
    boolean contains(Item item);
}
