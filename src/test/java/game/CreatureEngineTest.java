package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class CreatureEngineTest {

    @Test
    void movesAcrossGroundAndConsumesFood() {
        World world = world();
        Creature creature = creature(world, 1, 1, 0, '@', "player");
        world.add(creature);
        int initialFood = creature.food();

        creature.moveBy(1, 0, 0);

        assertEquals(2, creature.x);
        assertEquals(1, creature.y);
        assertEquals(initialFood - 1, creature.food());
    }

    @Test
    void cannotMoveThroughAWall() {
        Tile[][][] tiles = tiles();
        tiles[2][1][0] = Tile.WALL;
        World world = new World(tiles);
        Creature creature = creature(world, 1, 1, 0, '@', "player");
        world.add(creature);

        creature.moveBy(1, 0, 0);

        assertEquals(1, creature.x);
        assertEquals(1, creature.y);
    }

    @Test
    void randomizedAttackStaysWithinDamageBounds() {
        World world = world();
        Creature attacker = creature(world, 1, 1, 0, 'a', "attacker");
        Creature target = creature(world, 2, 1, 0, 't', "target");
        world.add(attacker);
        world.add(target);

        attacker.meleeAttack(target);

        assertTrue(target.hp() >= 90);
        assertTrue(target.hp() <= 99);
    }

    @Test
    void dyingCreatureLeavesCorpseAndIsRemoved() {
        World world = world();
        Creature creature = creature(world, 1, 1, 0, 'g', "goblin");
        world.add(creature);

        creature.modifyHp(-100, "test death");

        assertNull(world.creature(1, 1, 0));
        assertNotNull(world.item(1, 1, 0));
        assertEquals("test death", creature.causeOfDeath());
    }

    @Test
    void equipsWeaponAndArmorAccordingToTheirRatings() {
        World world = world();
        Creature creature = creature(world, 1, 1, 0, '@', "player");
        Item sword = new Item('/', Color.GRAY, "Sword", "Sword");
        sword.modifyAttackValue(5);
        Item shield = new Item('[', Color.BLUE, "Shield", "Shield");
        shield.modifyDefenseValue(5);

        creature.equip(sword);
        creature.equip(shield);

        assertSame(sword, creature.weapon());
        assertSame(shield, creature.armor());
        assertTrue(creature.inventory().contains(sword));
        assertTrue(creature.inventory().contains(shield));
    }

    @Test
    void castingSpellConsumesManaAndAddsEffectToTarget() {
        World world = world();
        Creature caster = creature(world, 1, 1, 0, '@', "player");
        Creature target = creature(world, 2, 1, 0, 'g', "goblin");
        world.add(caster);
        world.add(target);
        Spell spell = new Spell("slow", 2, new Effect(2));

        caster.castSpell(spell, 2, 1);

        assertEquals(3, caster.mana());
        assertEquals(1, target.effects().size());
    }

    @Test
    void cannotCastSpellWithoutEnoughMana() {
        World world = world();
        Creature caster = creature(world, 1, 1, 0, '@', "player");
        Creature target = creature(world, 2, 1, 0, 'g', "goblin");
        world.add(caster);
        world.add(target);
        caster.modifyMana(-5);

        caster.castSpell(new Spell("slow", 1, new Effect(2)), 2, 1);

        assertEquals(0, caster.mana());
        assertTrue(target.effects().isEmpty());
    }

    @Test
    void effectExpiresDuringCreatureUpdate() {
        World world = world();
        Creature caster = creature(world, 1, 1, 0, '@', "player");
        Creature target = creature(world, 2, 1, 0, 'g', "goblin");
        world.add(caster);
        world.add(target);
        caster.castSpell(new Spell("slow", 1, new Effect(1)), 2, 1);

        target.update();

        assertTrue(target.effects().isEmpty());
    }

    @Test
    void playerStarvesAndIsRemovedFromWorld() {
        World world = world();
        Creature player = creature(world, 1, 1, 0, '@', "player");
        world.add(player);

        player.modifyFood(-1000);

        assertTrue(CombatRules.isDead(player.hp()));
        assertNull(world.creature(1, 1, 0));
    }

    @Test
    void manaIsClampedToItsValidRange() {
        World world = world();
        Creature creature = creature(world, 1, 1, 0, '@', "player");

        creature.modifyMana(100);
        assertEquals(creature.maxMana(), creature.mana());

        creature.modifyMana(-100);
        assertEquals(0, creature.mana());
    }

    private static World world() {
        return new World(tiles());
    }

    private static Tile[][][] tiles() {
        Tile[][][] tiles = new Tile[5][5][1];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y][0] = Tile.FLOOR;
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
}
