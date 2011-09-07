package com.massivecraft.creativegates;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.google.gson.reflect.TypeToken;
import com.massivecraft.creativegates.zcore.persist.*;

public class OutGates extends EntityCollection<OutGate>
{
	public static OutGates i = new OutGates();
	
	CreativeGates p = CreativeGates.p;
	
	private OutGates()
	{
		super
		(
			OutGate.class,
			new TreeSet<OutGate>(new Comparator<OutGate>()
			{
				@Override
				public int compare(OutGate me, OutGate you)
				{
					return me.sourceCoord.toString().compareTo(you.sourceCoord.toString());
				}
			}),
			new LinkedHashMap<String, OutGate>(),
			new File(CreativeGates.p.getDataFolder(), "outgates.json"),
			CreativeGates.p.gson
		);
    this.loadFromDisc();
	}
	
	@Override
	public Type getMapType()
	{
		return new TypeToken<Map<String, OutGate>>(){}.getType();
	}
	
	// -------------------------------------------- //
	// Find gate from block or coord.
	// -------------------------------------------- //
	
	public OutGate findFromContent(WorldCoord coord)
	{
		for (OutGate gate : this.get())
		{
			if (gate.getContentCoords().contains(coord))
			{
				return gate;
			}
		}
		return null;
	}
	
	public OutGate findFromContent(Block block)
	{
		return findFromContent(new WorldCoord(block));
	}
	
	public OutGate findFromFrame(WorldCoord coord)
	{
		for (OutGate gate : this.get())
		{
			if (gate.frameCoords.contains(coord)) 
			{
				return gate;
			}
		}
		return null;
	}
	
	public OutGate findFromFrame(Block block)
	{
		return findFromFrame(new WorldCoord(block));
	}
	
	public OutGate findFrom(WorldCoord coord)
	{
		OutGate gate;
		
		gate = findFromContent(coord);
		if (gate != null)
		{
			return gate;
		}
		
		return findFromFrame(coord);
	}
	
	public OutGate findFrom(Block block)
	{
		return findFrom(new WorldCoord(block));
	}

	// -------------------------------------------- //
	// Mass Content Management
	// -------------------------------------------- //
	
	public void emptyAll()
	{
		for (OutGate gate : this.get())
		{
			gate.empty();
		}
    this.saveToDisc();
	}
	
	public void openAllOrDetach()
	{
		for (OutGate gate : this.get())
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
	public OutGate open(WorldCoord sourceCoord, Player player)
	{
		OutGate gate = new OutGate();
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
	
	public OutGate open(WorldCoord sourceCoord) {
		return this.open(sourceCoord, null);
	}

  public OutGate findByKey(List<Integer> fromKey) {
		outer: for (OutGate gate : this.get())
		{
      Iterator<Integer> fromKeyIterator = fromKey.iterator();
      Iterator<Integer> toKeyIterator = gate.getKey().iterator();
      
      while (fromKeyIterator.hasNext() && toKeyIterator.hasNext())
      {
        int from = fromKeyIterator.next();
        int to = toKeyIterator.next();

        if (from != to)
          continue outer;
      }

      if (fromKeyIterator.hasNext() || toKeyIterator.hasNext())
        continue;

      return gate;
		}
    
    return null;
  }

}
