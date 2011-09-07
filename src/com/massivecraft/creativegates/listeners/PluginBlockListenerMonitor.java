package com.massivecraft.creativegates.listeners;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

import com.massivecraft.creativegates.InGate;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.InGates;
import com.massivecraft.creativegates.OutGate;
import com.massivecraft.creativegates.OutGates;


public class PluginBlockListenerMonitor extends BlockListener
{
	CreativeGates p = CreativeGates.p;
	
	// Destroy the gate if the frame breaks
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) {
			return;
		}
		
		InGate gate = InGates.i.findFromFrame(event.getBlock());
		if (gate != null)
		{
			gate.close();
		}

    OutGate outGate = OutGates.i.findFromFrame(event.getBlock());
    if (outGate != null)
    {
      outGate.close();
    }
	}
}
