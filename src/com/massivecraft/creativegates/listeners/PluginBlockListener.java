package com.massivecraft.creativegates.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.massivecraft.creativegates.InGate;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.InGates;
import com.massivecraft.creativegates.OutGate;
import com.massivecraft.creativegates.OutGates;


public class PluginBlockListener extends BlockListener
{
	CreativeGates p = CreativeGates.p;
	
	@Override
	public void onBlockPistonExtend(BlockPistonExtendEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		
		for (Block block : event.getBlocks())
		{
			if (InGates.i.findFrom(block) != null || OutGates.i.findFrom(block) != null)
			{
				event.setCancelled(true);
				return;
			}
		}
	}
	
	// The purpose is to stop the water from falling
	public void onBlockFromTo(BlockFromToEvent event)
	{
		if (event.isCancelled()) return;


		Block blockFrom = event.getBlock();
		boolean isWater = blockFrom.getTypeId() == 9;
		
		if ( ! isWater)
		{
			return;
		}

		if (InGates.i.findFrom(blockFrom) != null)
		{
			event.setCancelled(true);
		}

		if (OutGates.i.findFrom(blockFrom) != null)
		{
			event.setCancelled(true);
		}
	}
	
	// The gate content is invulnerable
	@Override
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.isCancelled()) return;
		
		if (InGates.i.findFromContent(event.getBlock()) != null)
			event.setCancelled(true);

    if (OutGates.i.findFromContent(event.getBlock()) != null)
			event.setCancelled(true);
	}
	
	// Is the player allowed to destroy gates?
	@Override
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) return;

		if (InGates.i.findFromContent(event.getBlock()) != null)
			event.setCancelled(true);

    if (OutGates.i.findFromContent(event.getBlock()) != null)
			event.setCancelled(true);
		
	}
}
