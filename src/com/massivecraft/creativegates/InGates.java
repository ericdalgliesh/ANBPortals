package com.massivecraft.creativegates;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.creativegates.zcore.persist.*;

public class InGates extends EntityCollection<InGate>
{
	public static InGates i = new InGates();
	
	CreativeGates p = CreativeGates.p;
	
	private InGates()
	{
		super
		(
			InGate.class,
			new TreeSet<InGate>(new Comparator<InGate>()
			{
				@Override
				public int compare(InGate me, InGate you)
				{
					return me.sourceCoord.toString().compareTo(you.sourceCoord.toString());
				}
			}),
			new LinkedHashMap<String, InGate>(),
			new File(CreativeGates.p.getDataFolder(), "ingates.json"),
			CreativeGates.p.gson
		);
    this.loadFromDisc();
	}
	
	@Override
	public Type getMapType()
	{
		return new TypeToken<Map<String, InGate>>(){}.getType();
	}
	
	// -------------------------------------------- //
	// Find gate from block or coord.
	// -------------------------------------------- //
	
	public InGate findFromContent(WorldCoord coord)
	{
		for (InGate gate : this.get())
		{
			if (gate.getContentCoords().contains(coord))
			{
				return gate;
			}
		}
		return null;
	}
	
	public InGate findFromContent(Block block)
	{
		return findFromContent(new WorldCoord(block));
	}
	
	public InGate findFromFrame(WorldCoord coord)
	{
		for (InGate gate : this.get())
		{
			if (gate.frameCoords.contains(coord)) 
			{
				return gate;
			}
		}
		return null;
	}
	
	public InGate findFromFrame(Block block)
	{
		return findFromFrame(new WorldCoord(block));
	}
	
	public InGate findFrom(WorldCoord coord)
	{
		InGate gate;
		
		gate = findFromContent(coord);
		if (gate != null)
		{
			return gate;
		}
		
		return findFromFrame(coord);
	}
	
	public InGate findFrom(Block block)
	{
		return findFrom(new WorldCoord(block));
	}

	// -------------------------------------------- //
	// Mass Content Management
	// -------------------------------------------- //
	
	public void emptyAll()
	{
		for (InGate gate : this.get())
		{
			gate.empty();
		}
    this.saveToDisc();
	}
	
	public void openAllOrDetach()
	{
		for (InGate gate : this.get())
		{
			try
			{
				gate.open();
			}
			catch (GateOpenException e)
			{
				gate.detach();
				p.log(e.getMessage() + " Gate was removed.");
			}
		}
    this.saveToDisc();
	}
	
	// -------------------------------------------- //
	// Gate Factory
	// -------------------------------------------- //
	
	// TODO: Kolla in open or die saken. Den ska nog bytas ut mot attach.
	public InGate open(WorldCoord sourceCoord, Player player)
	{
		InGate gate = new InGate();
		gate.sourceCoord = sourceCoord;
		//p.log("sourceCoord: "+sourceCoord);
		
		try
		{
			gate.open();
			gate.attach();
			if (player != null)
			{
				gate.informPlayer(player);
			}
      this.saveToDisc();
			return gate;
		}
		catch (GateOpenException e)
		{
			if (player == null)
			{
				p.log(e.getMessage());
			}
			else
			{
				player.sendMessage(e.getMessage());
			}
      this.saveToDisc();
			return null;
		}
	}
	/*
	public InGate open(WorldCoord sourceCoord) {
		return this.open(sourceCoord, null);
	}*/

}
