package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GameCoreMutationTest {

    @Test
    void tilePropertiesDescribeEachTile() {
        for (Tile tile : Tile.values()) {
            assertNotNull(tile.glyph());
            assertNotNull(tile.color());
            assertNotNull(tile.details());
            assertEquals(tile != Tile.WALL && tile != Tile.BOUNDS, tile.isGround());
            assertEquals(tile == Tile.WALL, tile.isDiggable());
        }
    }

    @Test
    void pointEqualityHashAndNeighborsAreConsistent() {
        Point point = new Point(2, 3, 1);
        assertEquals(point, new Point(2, 3, 1));
        assertEquals(point.hashCode(), new Point(2, 3, 1).hashCode());
        assertFalse(point.equals(new Point(2, 3, 2)));

        List<Point> neighbors = point.neighbors8();
        Set<Point> unique = new HashSet<Point>(neighbors);
        assertEquals(8, neighbors.size());
        assertEquals(8, unique.size());
        assertFalse(neighbors.contains(point));
    }

    @Test
    void effectsUpdateAndCopyTheirDuration() {
        Effect effect = new Effect(2);
        Effect copy = new Effect(effect);
        assertFalse(effect.isDone());
        effect.update(null);
        assertFalse(effect.isDone());
        effect.update(null);
        assertTrue(effect.isDone());
        assertFalse(copy.isDone());
    }

    @Test
    void itemAndSpellValuesCanBeUpdated() {
        Item item = new Item('!', Color.RED, "test", "known");
        item.modifyFoodValue(4);
        item.modifyAttackValue(5);
        item.modifyDefenseValue(3);
        item.modifyThrownAttackValue(2);
        item.modifyRangedAttackValue(1);
        item.setQuaffEffect(new Effect(1));
        item.addWrittenSpell("test spell", 2, new Effect(3));

        assertEquals(4, item.foodValue());
        assertEquals(5, item.attackValue());
        assertEquals(3, item.defenseValue());
        assertEquals(3, item.thrownAttackValue());
        assertEquals(1, item.rangedAttackValue());
        assertNotNull(item.quaffEffect());
        assertEquals("  attack:5  thrown:3  ranged:1  defense:3  food:4", item.details());
        assertEquals(1, item.writtenSpells().size());

        Spell spell = item.writtenSpells().get(0);
        assertEquals("test spell", spell.name());
        assertEquals(2, spell.manaCost());
        assertTrue(spell.requiresTarget());
        assertNotNull(spell.effect());
    }

    @Test
    void creatureAiNamesItemsAndHandlesVisibility() {
        Tile[][][] tiles = tiles(7, 7, 1);
        tiles[4][3][0] = Tile.WALL;
        World world = new World(tiles);
        Creature creature = creature(world, 3, 3, 0, 'c', "creature");
        TestAi ai = new TestAi(creature);
        Item item = new Item('!', Color.WHITE, "potion", "strange potion");

        assertEquals("strange potion", ai.getName(item));
        ai.setName(item, "known potion");
        assertEquals("known potion", ai.getName(item));
        assertTrue(ai.canSee(3, 3, 0));
        assertFalse(ai.canSee(3, 3, 1));

        assertFalse(ai.canSee(5, 3, 0));
        assertTrue(ai.canSee(4, 3, 0));
    }

    @Test
    void creatureAiSelectsUsefulEquipmentAndThrownItems() {
        World world = world(7, 7, 1);
        Creature creature = creature(world, 3, 3, 0, 'c', "creature");
        TestAi ai = new TestAi(creature);
        Item better = new Item(')', Color.WHITE, "better", null);
        better.modifyAttackValue(10);
        Item rock = new Item(',', Color.WHITE, "rock", null);
        rock.modifyThrownAttackValue(7);
        creature.equip(better);
        creature.unequip(better);
        creature.equip(rock);

        assertTrue(ai.betterEquipment());
        assertSame(rock, ai.weaponToThrow());
        ai.equipBetter();
        assertSame(better, creature.weapon());
    }

    @Test
    void fieldOfViewRemembersVisibleTilesAndStopsAtWalls() {
        Tile[][][] tiles = tiles(7, 7, 1);
        tiles[4][3][0] = Tile.WALL;
        World world = new World(tiles);
        FieldOfView fov = new FieldOfView(world);

        fov.update(3, 3, 0, 3);

        assertTrue(fov.isVisible(3, 3, 0));
        assertTrue(fov.isVisible(4, 3, 0));
        assertEquals(Tile.WALL, fov.tile(4, 3, 0));
        assertFalse(fov.isVisible(3, 3, 1));
    }

    @Test
    void levelUpControllerExposesAndFindsAllOptions() {
        LevelUpController controller = new LevelUpController();
        List<String> names = controller.getLevelUpOptions();
        Creature creature = creature(world(3, 3, 1), 1, 1, 0, '@', "player");

        assertEquals(7, names.size());
        for (String name : names) {
            LevelUpOption option = controller.getLevelUpOption(name);
            assertNotNull(option);
            option.invoke(creature);
        }
        assertNull(controller.getLevelUpOption("missing"));
        assertTrue(creature.maxHp() > 100);
        assertTrue(creature.maxMana() > 5);
    }

    @Test
    void worldBuilderCreatesUsableCaveWorld() {
        World world = new WorldBuilder(30, 30, 2).makeCaves().build();

        assertEquals(30, world.width());
        assertEquals(30, world.height());
        assertEquals(2, world.depth());
        boolean hasExit = false;
        for (int x = 0; x < world.width(); x++) {
            for (int y = 0; y < world.height(); y++) {
                assertNotNull(world.tile(x, y, 0));
                assertNotNull(world.tile(x, y, 1));
                hasExit |= world.tile(x, y, 0) == Tile.STAIRS_UP;
            }
        }
        assertTrue(hasExit);
    }

    @Test
    void stuffFactoryCreatesCoreItemsAndCreatures() {
        World world = world(20, 20, 1);
        StuffFactory factory = new StuffFactory(world);
        FieldOfView fov = new FieldOfView(world);
        List<String> messages = new ArrayList<String>();
        Creature player = factory.newPlayer(messages, fov);

        assertEquals("player", player.name());
        assertEquals("rock", factory.newRock(0).name());
        assertEquals("teddy bear", factory.newVictoryItem(0).name());
        assertEquals("bread", factory.newBread(0).name());
        assertEquals("apple", factory.newFruit(0).name());
        assertEquals("dagger", factory.newDagger(0).name());
        assertEquals("sword", factory.newSword(0).name());
        assertEquals("staff", factory.newStaff(0).name());
        assertEquals("bow", factory.newBow(0).name());
        assertEquals("baguette", factory.newEdibleWeapon(0).name());
        assertEquals("tunic", factory.newLightArmor(0).name());
        assertEquals("chainmail", factory.newMediumArmor(0).name());
        assertEquals("platemail", factory.newHeavyArmor(0).name());
        assertEquals("health potion", factory.newPotionOfHealth(0).name());
        assertEquals("mana potion", factory.newPotionOfMana(0).name());
        assertEquals(4, factory.newWhiteMagesSpellbook(0).writtenSpells().size());
        assertEquals(4, factory.newBlueMagesSpellbook(0).writtenSpells().size());
        assertNotNull(factory.newBat(0));
        assertNotNull(factory.newFungus(0));
        assertNotNull(factory.newZombie(0, player));
        assertNotNull(factory.newGoblin(0, player));
    }

    private static World world(int width, int height, int depth) {
        return new World(tiles(width, height, depth));
    }

    private static Tile[][][] tiles(int width, int height, int depth) {
        Tile[][][] tiles = new Tile[width][height][depth];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                for (int z = 0; z < depth; z++)
                    tiles[x][y][z] = Tile.FLOOR;
        return tiles;
    }

    private static Creature creature(World world, int x, int y, int z, char glyph, String name) {
        Creature creature = new Creature(world, glyph, Color.WHITE, name, 100, 10, 5);
        creature.x = x;
        creature.y = y;
        creature.z = z;
        new CreatureAi(creature);
        return creature;
    }

    private static class TestAi extends CreatureAi {
        TestAi(Creature creature) {
            super(creature);
        }

        boolean betterEquipment() {
            return canUseBetterEquipment();
        }

        void equipBetter() {
            useBetterEquipment();
        }

        Item weaponToThrow() {
            return getWeaponToThrow();
        }
    }
}
