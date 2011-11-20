package com.massivecraft.creativegates.tasks;

import com.massivecraft.creativegates.CreativeGates;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;

public class SpawnAnimalsTask implements Runnable {
  public static final int SPAWN_DISTANCE = 32;

  private final CreativeGates p;

  public SpawnAnimalsTask(CreativeGates p) {
    this.p = p;
  }
  private AtomicLong lastRun = new AtomicLong(System.currentTimeMillis());

  public void run() {
    long time = System.currentTimeMillis();
    if (System.currentTimeMillis() < lastRun.get() +  3 * 60 * 1000) { // only run every 3 mins, even if invoked
      return;
    }
    lastRun.set(System.currentTimeMillis()); // do it at the start to more accurately hit the time.

    Player[] players = p.getServer().getOnlinePlayers();
    if (players.length < 1) {
      return;
    }
    final Random random = new Random();

    Player targetPlayer = players[random.nextInt(players.length)];
    final Location location = targetPlayer.getLocation();

    if (location.getBlockY() <= 40) {
      spawn(location, random, players, CreatureType.SLIME, Material.STONE);
    } else {
      spawn(location, random, players, randomCreature(random), Material.DIRT, Material.GRASS);
    }

  }

  private void spawn(Location location, Random random, Player[] players, CreatureType creature, Material... spawnOn) {
    Block startBlock = location.getBlock();

    BlockFace direction;
    switch (random.nextInt(4)) {
      case 0:
        direction = BlockFace.NORTH;
        break;
      case 1:
        direction = BlockFace.EAST;
        break;
      case 2:
        direction = BlockFace.SOUTH;
        break;
      default:
        direction = BlockFace.WEST;
        break;
    }

    for (int i = 0; i < SPAWN_DISTANCE; i++) {
      startBlock = startBlock.getRelative(direction);
    }

    while (startBlock.getType() != Material.AIR) {
      startBlock = startBlock.getRelative(BlockFace.UP);
    }

    while (startBlock.getType() == Material.AIR) {
      startBlock = startBlock.getRelative(BlockFace.DOWN);
    }

    if (!in(startBlock, spawnOn)) {
      return;
    }
    final Block targetBlock = startBlock.getRelative(BlockFace.UP);

    if (targetBlock.getRelative(BlockFace.UP).getType() != Material.AIR)
      return;
    for (Block b : neighbours(targetBlock)) {
      if (b.getType() != Material.AIR) return;
    }

    for (int i = 0; i < 4 + random.nextInt(players.length); i++) {
      startBlock.getWorld().spawnCreature(targetBlock.getLocation(), creature);
    }

  }

  private CreatureType randomCreature(Random random) {
    final int nextInt = random.nextInt(1000);
    if (nextInt < 240) return CreatureType.COW;
    if (nextInt < 480) return CreatureType.PIG;
    if (nextInt < 720) return CreatureType.SHEEP;
    if (nextInt < 960) return CreatureType.CHICKEN;
    return CreatureType.WOLF;
  }

  private Iterable<Block> neighbours(Block targetBlock) {
    ArrayList<Block> result = new ArrayList<Block>(4);
    result.add(targetBlock.getRelative(BlockFace.NORTH));
    result.add(targetBlock.getRelative(BlockFace.SOUTH));
    result.add(targetBlock.getRelative(BlockFace.EAST));
    result.add(targetBlock.getRelative(BlockFace.WEST));
    return result;
  }

  private boolean in(Block startBlock, Material[] spawnOn) {
    for (Material m : spawnOn) {
      if(startBlock.getType() == m)
        return true;
    }
    return false;
  }
}
