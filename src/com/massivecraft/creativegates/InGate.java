package com.massivecraft.creativegates;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.massivecraft.creativegates.util.BlockUtil;
import com.massivecraft.creativegates.zcore.persist.*;
import com.massivecraft.creativegates.zcore.util.*;

public class InGate extends Entity implements Comparable<InGate>
{
	public static transient CreativeGates p = CreativeGates.p;
	
	public transient Set<WorldCoord> contentCoords;
	public transient Set<WorldCoord> frameCoords;
  public transient List<Integer> key;
	public WorldCoord sourceCoord;
	public transient boolean frameDirIsNS; // True means NS direction. false means WE direction.
	
	private static transient final Set<BlockFace> expandFacesWE = new HashSet<BlockFace>();
	private static transient final Set<BlockFace> expandFacesNS = new HashSet<BlockFace>();
	static
	{
		expandFacesWE.add(BlockFace.UP);
		expandFacesWE.add(BlockFace.DOWN);
		expandFacesWE.add(BlockFace.WEST);
		expandFacesWE.add(BlockFace.EAST);
		
		expandFacesNS.add(BlockFace.UP);
		expandFacesNS.add(BlockFace.DOWN);
		expandFacesNS.add(BlockFace.NORTH);
		expandFacesNS.add(BlockFace.SOUTH);
	}
	
	public InGate()
	{
		this.dataClear();
	}
	
	/**
	 * Is this gate open right now?
	 */
	public boolean isOpen()
	{
		return InGates.i.findFrom(sourceCoord) != null;
	}
	
	public void open() throws GateOpenException
	{
		Block sourceBlock = sourceCoord.getBlock();
		
		if (this.isOpen()) return;

		if (sourceBlock.getTypeId() != Conf.inBlock)
		{
			throw new GateOpenException(p.txt.get("openfail.wrong_source_material", TextUtil.getMaterialName(Conf.inBlock)));
		}
		
		if ( ! this.dataPopulate())
		{
			throw new GateOpenException(p.txt.get("openfail.no_frame"));
		}
		
		// Finally we set the content blocks material to water
		this.fill();
	}
	
	public void close()
	{
		this.empty();
		this.detach();
	}
	
	/**
	 * This method is used to check the gate on use as a safety measure.
	 * If a player walks through a non intact gate, the frame was probably destroyed by a super pick axe.
	 */
	public boolean isIntact()
	{
		if (this.sourceCoord.getBlock().getTypeId() != Conf.inBlock)
		{
			return false;
		}
		
		for (WorldCoord coord : frameCoords)
		{
			if (this.sourceCoord.equals(coord))
			{
				continue;
			}
			
			Block block = coord.getBlock();
			if (block.getTypeId() != Conf.inBlock) return false;
		}
		return true;
	}
	
	/**
	 * This method clears the "data" (coords and material ids).
	 */
	public void dataClear()
	{
		contentCoords = new HashSet<WorldCoord>();
		frameCoords = new HashSet<WorldCoord>();
    key = new ArrayList<Integer>();
	}
	
	/**
	 * This method populates the "data" (coords and material ids).
	 * It will return false if there was no possible frame.
	 */
	public boolean dataPopulate()
	{
		this.dataClear();
		Block sourceBlock = sourceCoord.getBlock();
		
		// Search for content WE and NS
		Block floodStartBlock = sourceBlock.getRelative(BlockFace.UP);

		Set<Block> contentBlocksWE = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesWE);
		Set<Block> contentBlocksNS = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesNS);
		
		// Figure out dir and content... or throw no frame fail. 
		Set<Block> contentBlocks;
		
		if (contentBlocksWE == null && contentBlocksNS == null)
		{
			//throw new Exception("There is no frame, or it is broken, or it is to large.");
			return false;
		}
		
		if (contentBlocksNS == null)
		{
			contentBlocks = contentBlocksWE;
			frameDirIsNS = false;
		}
		else if (contentBlocksWE == null)
		{
			contentBlocks = contentBlocksNS;
			frameDirIsNS = true;
		}
		else if (contentBlocksWE.size() > contentBlocksNS.size())
		{
			contentBlocks = contentBlocksNS;
			frameDirIsNS = true;
		}
		else
		{
			contentBlocks = contentBlocksWE;
			frameDirIsNS = false;
		}

    // Find the frame blocks and materials
		Set<Block> frameBlocks = new HashSet<Block>();

     Set<BlockFace> expandFaces = frameDirIsNS ? expandFacesNS : expandFacesWE;
     for (Block currentBlock : contentBlocks)
     {
       for (BlockFace face : expandFaces)
       {
         Block potentialBlock = currentBlock.getRelative(face);
         if ( ! contentBlocks.contains(potentialBlock))
         {
           if(potentialBlock.getTypeId() != Conf.inBlock) return false;
           frameBlocks.add(potentialBlock);
         }
       }
     }
		
		// Now we add the frame and content blocks as world coords to the lookup maps.
		for (Block frameBlock : frameBlocks)
		{
			this.frameCoords.add(new WorldCoord(frameBlock));
		}
		for (Block contentBlock : contentBlocks)
		{
			this.contentCoords.add(new WorldCoord(contentBlock));
		}

		List<Integer> populateKey =  populateKeys(floodStartBlock);
    if(populateKey == null || populateKey.size() < 3 || populateKey.get(0) != Conf.keyStart)
      return false;
    key = populateKey;
    
		return true; 
	}

  private static List<Block> getTopBlocks(Block startBlock, BlockFace dirA, BlockFace dirB) {
    // check NS first,
    if (isFrame(startBlock.getRelative(dirA)) && isFrame(startBlock.getRelative(dirB))) {
    List<Block> result = new ArrayList<Block>();
      // prefer NS over EW.
      Block first = startBlock.getRelative(dirA);
      while (isFrame(first.getRelative(dirA))) {
        first = first.getRelative(dirA);
      }
      Block newFirst = first.getRelative(BlockFace.UP);
      while (isFrame(newFirst.getRelative(BlockFace.DOWN))) {
        result.add(newFirst);
        newFirst = newFirst.getRelative(dirB);
      }
      return result;
    }
    return null;
  }

  public static List<Block> getTopBlocks(Block startBlock)
	{

    int height = 0;
    while (startBlock.getTypeId() != Conf.inBlock && height < 32)
    {
      startBlock = startBlock.getRelative(BlockFace.UP);
      height++;
    }

    List<Block> result = getTopBlocks(startBlock, BlockFace.NORTH, BlockFace.SOUTH);
   return result != null ? result : getTopBlocks(startBlock, BlockFace.EAST, BlockFace.WEST);
	}

  private static boolean isFrame(Block b)
  {
    return b.getTypeId() == Conf.inBlock;
  }

  private List<Integer> populateKeys(Block startBlock)
  {
    List<Integer> key = new ArrayList<Integer>();
    List<Block> topBlocks = getTopBlocks(startBlock);
    if (topBlocks == null) return null;
    Collections.sort(topBlocks, new Comparator<Block>() {

      public int compare(Block t, Block t1) {
        return (int) ((long)t.getZ() - (long)t1.getZ() + (long)t.getX() - (long)t1.getX());
      }
    });
    if (topBlocks.get(0).getTypeId() != Conf.keyStart) {
      Collections.reverse(topBlocks);
    }
    for (Block block : topBlocks) {
      key.add(block.getTypeId());
    }
    return key;
  }
	
	//----------------------------------------------//
	// Gate information
	//----------------------------------------------//
	
	public String getInfoMsgMaterial()
	{
		ArrayList<String> keyStringArray = new ArrayList<String>();
		for (Integer frameMaterialId : this.key)
		{
			keyStringArray.add(p.txt.tags("<h>") + TextUtil.getMaterialName(Material.getMaterial(frameMaterialId)));
		}
		
		String materials = TextUtil.implode(keyStringArray, p.txt.tags("<i>, "));
		
		return p.txt.get("info.materials", materials);
	}
	
	public void informPlayer(Player player)
	{
		player.sendMessage("");
		player.sendMessage(this.getInfoMsgMaterial());
	}
	
	//----------------------------------------------//
	// Content management
	//----------------------------------------------//
	
	public void fill()
	{
		for (WorldCoord coord : this.contentCoords)
		{
			coord.getBlock().setType(Material.STATIONARY_WATER);
		}
	}
	
	public void empty()
	{
		for (WorldCoord coord : this.contentCoords)
		{
			coord.getBlock().setType(Material.AIR);
		}
	}
	
	//----------------------------------------------//
	// Flood
	//----------------------------------------------//

	public static Set<Block> getFloodBlocks(Block startBlock, Set<Block> foundBlocks, Set<BlockFace> expandFaces)
	{
		if (foundBlocks == null)
		{
			return null;
		}
		
		if  (foundBlocks.size() > Conf.maxarea)
		{
			return null;
		}
		
		if (foundBlocks.contains(startBlock))
		{
			return foundBlocks;
		}
		
		if (startBlock.getType() == Material.AIR || startBlock.getType() == Material.WATER || startBlock.getType() == Material.STATIONARY_WATER)
		{
			// ... We found a block :D ...
			foundBlocks.add(startBlock);
			
			// ... And flood away !
			for (BlockFace face : expandFaces)
			{
				Block potentialBlock = startBlock.getRelative(face);
				foundBlocks = getFloodBlocks(potentialBlock, foundBlocks, expandFaces);
			}
		}
		
		return foundBlocks;
	}

	//----------------------------------------------//
	// Comparable
	//----------------------------------------------//
	
	@Override
	public int compareTo(InGate o)
	{
		return this.sourceCoord.toString().compareTo(o.sourceCoord.toString());
	}

  public List<Integer> getKey()
  {
    return this.key;
  }

  private List<Block> getHighestBlocks(Set<Block> contentBlocks)
  {
    int max = Integer.MIN_VALUE;
    List<Block> results = new ArrayList<Block>();
    for (Block block : contentBlocks)
    {
      if (block.getY() > max)
      {
        results = new ArrayList<Block>();
        max = block.getY();
      }
      if (block.getY() >= max)
      {
        results.add(block);
      }
    }
    return results;
  }

  public Set<WorldCoord> getContentCoords() {
    if(contentCoords == null || contentCoords.isEmpty())
    {
      this.dataPopulate();
      if(this.isOpen()) fill();
      else empty();
    }
    return contentCoords;
  }
}
