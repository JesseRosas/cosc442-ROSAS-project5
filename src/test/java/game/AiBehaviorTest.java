package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AiBehaviorTest {

    @Test
    void playerAiRecordsMessagesAndRemembersFieldOfView() {
        World world = world(7, 7, 1);
        Creature player = creature(world, 3, 3, 0, '@', "player");
        FieldOfView fov = new FieldOfView(world);
        fov.update(3, 3, 0, 2);
        List<String> messages = new ArrayList<String>();
        PlayerAi ai = new PlayerAi(player, messages, fov);

        ai.onNotify("hello");
        ai.onEnter(4, 3, 0, Tile.FLOOR);

        assertEquals(List.of("hello"), messages);
        assertEquals(4, player.x);
        assertTrue(ai.canSee(3, 3, 0));
        assertEquals(Tile.FLOOR, ai.rememberedTile(3, 3, 0));
    }

    @Test
    void playerAiDigsWhenEnteringAWall() {
        Tile[][][] tiles = tiles(5, 5, 1);
        tiles[2][2][0] = Tile.WALL;
        World world = new World(tiles);
        Creature player = creature(world, 1, 2, 0, '@', "player");
        FieldOfView fov = new FieldOfView(world);
        List<String> messages = new ArrayList<String>();
        PlayerAi ai = new PlayerAi(player, messages, fov);
        world.add(player);

        ai.onEnter(2, 2, 0, Tile.WALL);

        assertEquals(Tile.FLOOR, world.tile(2, 2, 0));
    }

    @Test
    void goblinUsesBetterEquipmentThenRangedAttack() {
        World world = world(10, 10, 1);
        Creature player = creature(world, 6, 5, 0, '@', "player");
        Creature goblin = creature(world, 5, 5, 0, 'g', "goblin");
        world.add(player);
        world.add(goblin);
        GoblinAi ai = new GoblinAi(goblin, player);
        Item better = new Item(')', Color.WHITE, "sword", null);
        better.modifyAttackValue(20);
        goblin.equip(better);
        goblin.unequip(better);

        ai.onUpdate();
        assertSame(better, goblin.weapon());

        Creature rangedGoblin = creature(world, 6, 4, 0, 'g', "goblin");
        world.add(rangedGoblin);
        GoblinAi rangedAi = new GoblinAi(rangedGoblin, player);
        Item bow = new Item(')', Color.WHITE, "bow", null);
        bow.modifyRangedAttackValue(10);
        rangedGoblin.equip(bow);
        int hp = player.hp();
        rangedAi.onUpdate();
        assertTrue(player.hp() < hp);
    }

    @Test
    void goblinThrowsWhenItHasNoRangedWeapon() {
        World world = world(10, 10, 1);
        Creature player = creature(world, 6, 5, 0, '@', "player");
        Creature goblin = creature(world, 5, 5, 0, 'g', "goblin");
        world.add(player);
        world.add(goblin);
        GoblinAi ai = new GoblinAi(goblin, player);
        Item rock = new Item(',', Color.WHITE, "rock", null);
        rock.modifyThrownAttackValue(10);
        goblin.equip(rock);
        int hp = player.hp();

        ai.onUpdate();

        assertTrue(player.hp() < hp);
        assertFalse(goblin.inventory().contains(rock));
    }

    @Test
    void goblinHuntsVisiblePlayerWithoutEquipment() {
        World world = world(10, 10, 1);
        Creature player = creature(world, 7, 5, 0, '@', "player");
        Creature goblin = creature(world, 5, 5, 0, 'g', "goblin");
        world.add(player);
        world.add(goblin);
        new GoblinAi(goblin, player);

        int initialX = goblin.x;
        goblin.update();

        assertTrue(goblin.x > initialX);
    }

    @Test
    void goblinPicksUpAnItemWhenPlayerIsOnAnotherLevel() {
        World world = world(10, 10, 2);
        Creature player = creature(world, 7, 5, 1, '@', "player");
        Creature goblin = creature(world, 5, 5, 0, 'g', "goblin");
        world.add(player);
        world.add(goblin);
        Item item = new Item('!', Color.WHITE, "potion", null);
        world.addAtEmptySpace(item, goblin.x, goblin.y, goblin.z);
        new GoblinAi(goblin, player);

        goblin.update();

        assertTrue(goblin.inventory().contains(item));
    }

    @Test
    void zombieCanUpdateAndBatCanWanderWithoutCrashing() {
        World world = world(10, 10, 1);
        Creature player = creature(world, 7, 5, 0, '@', "player");
        Creature zombie = creature(world, 5, 5, 0, 'z', "zombie");
        Creature bat = creature(world, 3, 3, 0, 'b', "bat");
        world.add(player);
        world.add(zombie);
        world.add(bat);
        new ZombieAi(zombie, player);
        new BatAi(bat);

        zombie.update();
        bat.update();

        assertNotNull(world.creature(zombie.x, zombie.y, zombie.z));
        assertNotNull(world.creature(bat.x, bat.y, bat.z));
    }

    @Test
    void potionEffectsChangeAndRestoreCreatureStats() {
        World world = world(20, 20, 1);
        StuffFactory factory = new StuffFactory(world);
        Creature creature = creature(world, 10, 10, 0, '@', "player");
        new CreatureAi(creature);

        creature.modifyHp(-20, "damage");
        int hp = creature.hp();
        factory.newPotionOfHealth(0).quaffEffect().start(creature);
        assertTrue(creature.hp() > hp);

        creature.modifyMana(-3);
        factory.newPotionOfMana(0).quaffEffect().start(creature);
        assertEquals(5, creature.mana());

        int attack = creature.attackValue();
        int defense = creature.defenseValue();
        Effect warrior = factory.newPotionOfWarrior(0).quaffEffect();
        warrior.start(creature);
        assertEquals(attack + 5, creature.attackValue());
        assertEquals(defense + 5, creature.defenseValue());
        warrior.end(creature);
        assertEquals(attack, creature.attackValue());
        assertEquals(defense, creature.defenseValue());
    }

    @Test
    void spellbookEffectsHaveObservableTargetedBehavior() {
        World world = world(20, 20, 1);
        StuffFactory factory = new StuffFactory(world);
        Creature creature = creature(world, 10, 10, 0, '@', "player");
        new CreatureAi(creature);

        Item white = factory.newWhiteMagesSpellbook(0);
        int hp = creature.hp();
        creature.modifyHp(-10, "damage");
        Effect copiedEffect = white.writtenSpells().get(0).effect();
        copiedEffect.start(creature);
        assertEquals(hp - 10, creature.hp());

        Item blue = factory.newBlueMagesSpellbook(0);
        int mana = creature.mana();
        blue.writtenSpells().get(0).effect().start(creature);
        assertEquals(mana, creature.mana());
        Effect detect = blue.writtenSpells().get(3).effect();
        detect.start(creature);
        detect.end(creature);
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
}
