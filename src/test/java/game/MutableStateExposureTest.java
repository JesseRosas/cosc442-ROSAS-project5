package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class MutableStateExposureTest {

    @Test
    void inventoryArrayIsASnapshot() {
        Inventory inventory = new Inventory(1);
        Item sword = new Item(')', Color.WHITE, "sword", null);
        Item replacement = new Item(')', Color.GRAY, "replacement", null);
        inventory.add(sword);

        Item[] exposedItems = inventory.getItems();
        exposedItems[0] = replacement;

        assertSame(sword, inventory.get(0));
    }

    @Test
    void effectsCannotBeChangedThroughTheGetter() {
        Creature creature = creature();

        assertThrows(UnsupportedOperationException.class, () -> creature.effects().clear());
        assertEquals(0, creature.effects().size());
    }

    @Test
    void writtenSpellsCannotBeChangedThroughTheGetter() {
        Item scroll = new Item('?', Color.WHITE, "scroll", null);
        scroll.addWrittenSpell("heal", 1, new Effect(1));

        assertThrows(UnsupportedOperationException.class, () -> scroll.writtenSpells().clear());
        assertEquals(1, scroll.writtenSpells().size());
    }

    @Test
    void linePointsAreImmutableSnapshots() {
        Line line = new Line(0, 0, 2, 0);
        Point exposedPoint = line.getPoints().get(0);
        exposedPoint.x = 99;

        assertEquals(0, line.getPoints().get(0).x);
        assertThrows(UnsupportedOperationException.class, () -> line.getPoints().clear());
    }

    @Test
    void worldCopiesItsTileInput() {
        Tile[][][] source = {{{Tile.WALL}}};
        World world = new World(source);

        source[0][0][0] = Tile.FLOOR;

        assertEquals(Tile.WALL, world.tile(0, 0, 0));
    }

    private static Creature creature() {
        World world = new World(new Tile[][][] {{{Tile.FLOOR}}});
        Creature creature = new Creature(world, '@', Color.WHITE, "hero", 10, 1, 1);
        new CreatureAi(creature);
        return creature;
    }
}
