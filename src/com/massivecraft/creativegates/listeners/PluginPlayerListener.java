package com.massivecraft.creativegates.listeners;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.massivecraft.creativegates.Conf;
import com.massivecraft.creativegates.InGate;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.InGates;
import com.massivecraft.creativegates.OutGate;
import com.massivecraft.creativegates.OutGates;
import com.massivecraft.creativegates.WorldCoord;
import com.massivecraft.creativegates.event.CreativeGatesTeleportEvent;
import java.util.List;

public class PluginPlayerListener extends PlayerListener
{
	
	CreativeGates p = CreativeGates.p;
	
	public void onPlayerMove(PlayerMoveEvent event)
	{
		if (event.isCancelled()) return;
		if (event.getFrom().getBlock().equals(event.getTo().getBlock())) return;
		
		// We look one up due to half blocks.
		Block blockToTest = event.getTo().getBlock().getRelative(BlockFace.UP);
		
		// Fast material check 
		if (blockToTest.getType() != Material.STATIONARY_WATER) return;
		
		// Find the gate if there is one
		InGate gateFrom = InGates.i.findFromContent(blockToTest);
		if (gateFrom == null) return;
		
		// Is the gate intact?
		if ( ! gateFrom.isIntact())
		{
			gateFrom.close();
			return;
		}
		
		// Find the target location
    List<Integer> fromKey = gateFrom.getKey();
    OutGate destination = OutGates.i.findByKey(fromKey);

    if (destination == null)
    {
			event.getPlayer().sendMessage("No destination gate.");
			return;
    }
    
    Location targetLocation = destination.getMyOwnExitLocation();
		if (targetLocation == null)
		{
			event.getPlayer().sendMessage(p.txt.get("usefail.no_target_location"));
			return;
		}
		
		CreativeGatesTeleportEvent gateevent = new CreativeGatesTeleportEvent(event, targetLocation, destination.getKey());
		p.getServer().getPluginManager().callEvent(gateevent);
	}
	
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.isCancelled()) return;
		
		// We are only interested in left clicks with a wand
		if
		(
			event.getAction() != Action.LEFT_CLICK_BLOCK ||
			event.getPlayer().getItemInHand().getTypeId() != Conf.wand
		)
		{
			return;
		}
		
		Block clickedBlock = event.getClickedBlock();
		Player player = event.getPlayer();
		
		// Did we hit an existing gate?
		// In such case send information.
		InGate gate = InGates.i.findFrom(clickedBlock);
		if (gate != null)
		{
			gate.informPlayer(player);
			return;
		}

    OutGate outGate = OutGates.i.findFrom(clickedBlock);
    if (outGate != null)
    {
      outGate.informPlayer(player);
      return;
    }
		
		if (clickedBlock.getTypeId() == Conf.inBlock)
		{
      InGates.i.open(new WorldCoord(clickedBlock), player);
		}
    else if (clickedBlock.getTypeId() == Conf.outBlock)
    {
      OutGates.i.open(new WorldCoord(clickedBlock), player);
    }
	}
	
	@Override
	public void onPlayerBucketFill(PlayerBucketFillEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		
		if ( InGates.i.findFromContent(event.getBlockClicked()) != null )
		{
			event.setCancelled(true);
		}
		if ( OutGates.i.findFromContent(event.getBlockClicked()) != null )
		{
			event.setCancelled(true);
		}
	}
	
	@Override
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
	{
		if (event.isCancelled())
		{
			return;
		}
		
		if ( InGates.i.findFromContent(event.getBlockClicked().getRelative(event.getBlockFace())) != null )
		{
			event.setCancelled(true);
		}
		if ( OutGates.i.findFromContent(event.getBlockClicked().getRelative(event.getBlockFace())) != null )
		{
			event.setCancelled(true);
		}
	}
}
