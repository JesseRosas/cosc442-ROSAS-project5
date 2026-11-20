package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CreatureBehaviorTest {

    @Test
    void modifiesStatsAndReportsIdentity() {
        World world = world(5, 5, 1);
        Creature creature = creature(world, 2, 2, 0, '@', "hero");

        assertTrue(creature.isPlayer());
        assertEquals(100, creature.maxHp());
        assertEquals(100, creature.hp());
        assertEquals(10, creature.attackValue());
        assertEquals(5, creature.defenseValue());
        assertEquals(9, creature.visionRadius());
        assertEquals(1000, creature.maxFood());
        assertEquals(5, creature.maxMana());
        assertEquals(5, creature.mana());

        creature.modifyMaxHp(10);
        creature.modifyAttackValue(2);
        creature.modifyDefenseValue(3);
        creature.modifyVisionRadius(2);
        creature.modifyMaxMana(4);
        creature.modifyRegenHpPer1000(5);
        creature.modifyRegenManaPer1000(5);
        creature.modifyDetectCreatures(1);

        assertEquals(110, creature.maxHp());
        assertEquals(12, creature.attackValue());
        assertEquals(8, creature.defenseValue());
        assertEquals(11, creature.visionRadius());
        assertEquals(9, creature.maxMana());
        assertEquals("  level:1  attack:12  defense:8  hp:100", creature.details());
    }

    @Test
    void movementHandlesNoOpWallsAndOpenGround() {
        Tile[][][] tiles = tiles(5, 5, 1);
        tiles[3][2][0] = Tile.WALL;
        World world = new World(tiles);
        Creature creature = creature(world, 2, 2, 0, '@', "hero");
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);
        int food = creature.food();

        creature.moveBy(0, 0, 0);
        assertEquals(food, creature.food());

        creature.moveBy(1, 0, 0);
        assertEquals(2, creature.x);
        assertTrue(ai.messages.stream().anyMatch(message -> message.contains("bump into a wall")));

        creature.moveBy(0, 1, 0);
        assertEquals(2, creature.x);
        assertEquals(3, creature.y);
        assertEquals(food - 2, creature.food());
    }

    @Test
    void movementUsesStairsAndBlocksWrongDirection() {
        Tile[][][] tiles = tiles(3, 3, 2);
        tiles[1][1][0] = Tile.STAIRS_DOWN;
        tiles[1][1][1] = Tile.WALL;
        World world = new World(tiles);
        Creature creature = creature(world, 1, 1, 1, '@', "hero");
        RecordingAi ai = new RecordingAi(creature);
        world.add(creature);

        creature.moveBy(0, 0, -1);
        assertEquals(0, creature.z);

        creature.moveBy(0, 0, 1);
        assertEquals(0, creature.z);
        assertTrue(ai.messages.stream().anyMatch(message -> message.contains("cave floor")));
    }

    @Test
    void regenerationRestoresMissingResources() {
        World world = world(5, 5, 1);
        Creature creature = creature(world, 2, 2, 0, 'c', "creature");
        new RecordingAi(creature);
        creature.modifyHp(-10, "damage");
        creature.modifyMana(-3);
        creature.modifyRegenHpPer1000(1000);
        creature.modifyRegenManaPer1000(1000);
        int food = creature.food();

        creature.update();

        assertEquals(91, creature.hp());
        assertEquals(3, creature.mana());
        assertEquals(food - 3, creature.food());
    }

    @Test
    void visibilityAndMemoryRespectCreatureDetection() {
        World world = world(5, 5, 1);
        Creature observer = creature(world, 2, 2, 0, '@', "observer");
        Creature target = creature(world, 3, 2, 0, 'g', "target");
        RecordingAi ai = new RecordingAi(observer);
        world.add(observer);
        world.add(target);

        assertSame(target, observer.creature(3, 2, 0));
        observer.modifyDetectCreatures(1);
        assertSame(target, observer.creature(3, 2, 0));
        assertEquals(Tile.FLOOR, observer.realTile(3, 2, 0));
        assertEquals(Tile.FLOOR, observer.tile(3, 2, 0));
        assertNotNull(ai);
    }

    @Test
    void pickupDropEatAndEquipMaintainInventory() {
        World world = world(7, 7, 1);
        Creature creature = creature(world, 3, 3, 0, '@', "hero");
        new RecordingAi(creature);
        world.add(creature);
        Item bread = new Item('%', Color.YELLOW, "bread", null);
        bread.modifyFoodValue(20);
        world.addAtEmptySpace(bread, 3, 3, 0);

        creature.pickup();
        assertTrue(creature.inventory().contains(bread));
        int food = creature.food();
        creature.eat(bread);
        assertFalse(creature.inventory().contains(bread));
        assertEquals(food + 20, creature.food());

        Item sword = new Item(')', Color.WHITE, "sword", null);
        sword.modifyAttackValue(5);
        creature.equip(sword);
        assertSame(sword, creature.weapon());
        creature.unequip(sword);
        assertNull(creature.weapon());
        creature.drop(sword);
        assertFalse(creature.inventory().contains(sword));
    }

    @Test
    void spellCastingChecksManaAndTarget() {
        World world = world(5, 5, 1);
        Creature caster = creature(world, 2, 2, 0, '@', "caster");
        Creature target = creature(world, 3, 2, 0, 'g', "target");
        new RecordingAi(caster);
        new RecordingAi(target);
        world.add(caster);
        world.add(target);
        Spell spell = new Spell("slow", 2, new Effect(2));

        caster.castSpell(spell, 3, 2);
        assertEquals(3, caster.mana());
        assertEquals(1, target.effects().size());

        caster.modifyMana(-10);
        caster.castSpell(spell, 3, 2);
        caster.castSpell(spell, 4, 4);
        assertEquals(0, caster.mana());
    }

    @Test
    void throwingHitsTargetAndAppliesItemEffect() {
        World world = world(7, 7, 1);
        Creature attacker = creature(world, 2, 2, 0, '@', "attacker");
        Creature target = creature(world, 3, 2, 0, 'g', "target");
        new RecordingAi(attacker);
        new RecordingAi(target);
        world.add(attacker);
        world.add(target);
        Item potion = new Item('!', Color.RED, "potion", null);
        potion.setQuaffEffect(new Effect(2));
        attacker.equip(potion);

        attacker.throwItem(potion, target.x, target.y, target.z);

        assertEquals(1, target.effects().size());
        assertFalse(attacker.inventory().contains(potion));
    }

    @Test
    void gainXpAndLearnNameNotifyTheAi() {
        World world = world(5, 5, 1);
        Creature creature = creature(world, 2, 2, 0, '@', "hero");
        RecordingAi ai = new RecordingAi(creature);
        Item item = new Item('!', Color.WHITE, "potion", "strange potion");

        creature.learnName(item);
        creature.gainXp(new Creature(world, 'g', Color.GRAY, "enemy", 1, 1, 1));
        creature.notify("hello %s", "world");

        assertEquals("potion", ai.getName(item));
        assertTrue(creature.xp() > 0);
        assertTrue(ai.messages.stream().anyMatch(message -> message.contains("hello world")));
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

    private static class RecordingAi extends CreatureAi {
        private final List<String> messages = new ArrayList<String>();

        RecordingAi(Creature creature) {
            super(creature);
        }

        @Override
        public void onNotify(String message) {
            messages.add(message);
        }
    }
}
