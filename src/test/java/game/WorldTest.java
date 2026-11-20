package game;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class WorldTest {

    @Test
    void reportsDimensionsAndBounds() {
        World world = world(3, 4, 2);

        assertEquals(3, world.width());
        assertEquals(4, world.height());
        assertEquals(2, world.depth());
        assertEquals(Tile.FLOOR, world.tile(1, 2, 1));
        assertEquals(Tile.BOUNDS, world.tile(-1, 0, 0));
        assertEquals(Tile.BOUNDS, world.tile(3, 0, 0));
        assertEquals(Tile.BOUNDS, world.tile(0, 4, 0));
        assertEquals(Tile.BOUNDS, world.tile(0, 0, 2));
    }

    @Test
    void digChangesOnlyDiggableTiles() {
        Tile[][][] tiles = tiles(3, 3, 1);
        tiles[1][1][0] = Tile.WALL;
        World world = new World(tiles);

        world.dig(1, 1, 0);
        world.dig(0, 0, 0);

        assertEquals(Tile.FLOOR, world.tile(1, 1, 0));
        assertEquals(Tile.FLOOR, world.tile(0, 0, 0));
    }

    @Test
    void glyphAndColorPreferCreaturesThenItemsThenTiles() {
        World world = world(3, 3, 1);
        Item item = new Item('!', Color.RED, "Potion", "Potion");
        Creature creature = creature(world, 1, 1, 0, 'c', "goblin");

        world.addAtEmptySpace(item, 0, 0, 0);
        assertEquals('!', world.glyph(0, 0, 0));
        assertEquals(Color.RED, world.color(0, 0, 0));

        world.add(creature);
        assertEquals('c', world.glyph(1, 1, 0));
        assertEquals(Color.GRAY, world.color(1, 1, 0));
    }

    @Test
    void addsAndRemovesItemsByCoordinateAndReference() {
        World world = world(3, 3, 1);
        Item item = new Item('!', Color.RED, "Potion", "Potion");

        assertTrue(world.addAtEmptySpace(item, 1, 1, 0));
        assertSame(item, world.item(1, 1, 0));

        world.remove(item);
        assertNull(world.item(1, 1, 0));

        world.addAtEmptySpace(item, 0, 0, 0);
        world.remove(0, 0, 0);
        assertNull(world.item(0, 0, 0));
    }

    @Test
    void searchesNeighboringSpacesWhenInitialSpaceIsOccupied() {
        World world = world(3, 3, 1);
        Item first = new Item('!', Color.RED, "Potion", "Potion");
        Item second = new Item('?', Color.BLUE, "Scroll", "Scroll");
        world.addAtEmptySpace(first, 1, 1, 0);

        assertTrue(world.addAtEmptySpace(second, 1, 1, 0));
        assertNotNull(world.item(1, 1, 0));
        assertTrue(containsItem(world, second));
    }

    @Test
    void returnsFalseWhenNoSpaceIsAvailable() {
        Tile[][][] tiles = tiles(2, 2, 1);
        World world = new World(tiles);
        Item first = new Item('!', Color.RED, "Potion", "Potion");
        Item second = new Item('?', Color.BLUE, "Scroll", "Scroll");

        world.addAtEmptySpace(first, 0, 0, 0);
        world.addAtEmptySpace(first, 1, 0, 0);
        world.addAtEmptySpace(first, 0, 1, 0);
        world.addAtEmptySpace(first, 1, 1, 0);

        assertFalse(world.addAtEmptySpace(second, 0, 0, 0));
    }

    @Test
    void placesCreatureOnAValidRandomLocation() {
        World world = world(4, 4, 1);
        Creature creature = creature(world, 0, 0, 0, 'g', "goblin");

        world.addAtEmptyLocation(creature, 0);

        assertSame(creature, world.creature(creature.x, creature.y, creature.z));
        assertEquals(0, creature.z);
        assertTrue(world.tile(creature.x, creature.y, creature.z).isGround());
    }

    @Test
    void findsPathAcrossOpenGround() {
        World world = world(5, 5, 1);
        Creature creature = creature(world, 1, 1, 0, 'g', "goblin");
        world.add(creature);

        Path path = new Path(creature, 3, 3);

        assertNotNull(path.points());
        assertFalse(path.points().isEmpty());
        Point last = path.points().get(path.points().size() - 1);
        assertEquals(3, last.x);
        assertEquals(3, last.y);
    }

    private static World world(int width, int height, int depth) {
        return new World(tiles(width, height, depth));
    }

    private static Tile[][][] tiles(int width, int height, int depth) {
        Tile[][][] tiles = new Tile[width][height][depth];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    tiles[x][y][z] = Tile.FLOOR;
                }
            }
        }
        return tiles;
    }

    private static Creature creature(World world, int x, int y, int z, char glyph, String name) {
        Creature creature = new Creature(world, glyph, Color.GRAY, name, 100, 10, 5);
        creature.x = x;
        creature.y = y;
        creature.z = z;
        new CreatureAi(creature);
        return creature;
    }

    private static boolean containsItem(World world, Item expected) {
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                for (int z = 0; z < world.depth(); z++) {
                    if (world.item(x, y, z) == expected) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
