package com.massivecraft.creativegates;

import org.bukkit.Material;

public class Conf
{	
	public static int wand = Material.COMPASS.getId();
	public static int inBlock = Material.GOLD_BLOCK.getId();
	public static int outBlock = Material.IRON_BLOCK.getId();
	public static int maxarea = 200;
  public static int keyStart = Material.GLOWSTONE.getId();
	
	public static transient Conf i = new Conf();
	
	public static void load()
	{
		CreativeGates.p.persist.loadOrSaveDefault(i, Conf.class);
	}
	public static void save()
	{
		CreativeGates.p.persist.save(i);
	}
}
