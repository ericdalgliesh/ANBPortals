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

public class OutGate extends Entity implements Comparable<OutGate>
{
	public static transient CreativeGates p = CreativeGates.p;
	
	public transient Set<WorldCoord> contentCoords;
	public transient Set<WorldCoord> frameCoords;
  public transient List<Integer> key;
	public WorldCoord sourceCoord;
	public transient boolean frameDirIsNS; // True means NS direction. false means WE direction.

  void printContent()
  {

    if(true) return;

    println("wc");
    for (WorldCoord c : contentCoords) println(c);
    println("fc");
    for (WorldCoord c : frameCoords) println(c);
  }


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
	
	public OutGate()
	{
		this.dataClear();
	}
	
	/**
	 * Is this gate open right now?
	 */
	public boolean isOpen()
	{
		return OutGates.i.findFrom(sourceCoord) != null;
	}
	
	public void open() throws GateOpenException
	{
		Block sourceBlock = sourceCoord.getBlock();
		
		if (this.isOpen()) return;
		
		if (sourceBlock.getTypeId() != Conf.outBlock)
		{
			throw new GateOpenException(p.txt.get("openfail.wrong_source_material", TextUtil.getMaterialName(Conf.outBlock)));
		}
		
		if ( ! this.dataPopulate())
		{
			throw new GateOpenException(p.txt.get("openfail.no_frame"));
		}
		printContent();
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
		if (this.sourceCoord.getBlock().getTypeId() != Conf.outBlock)
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
			if (block.getTypeId() != Conf.outBlock) return false;
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
           if(potentialBlock.getTypeId() != Conf.outBlock) return false;
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

  public static List<Block> getTopBlocks(Block startBlock)
	{

    int height = 0;
    while (startBlock.getTypeId() != Conf.outBlock && height < 32)
    {
      startBlock = startBlock.getRelative(BlockFace.UP);
      height++;
    }

    List<Block> result = getTopBlocks(startBlock, BlockFace.NORTH, BlockFace.SOUTH);
   return result != null ? result : getTopBlocks(startBlock, BlockFace.EAST, BlockFace.WEST);
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

  private static boolean isFrame(Block b)
  {
    return b.getTypeId() == Conf.outBlock;
  }

	/*
	 * If someone arrives to this gate, where should we place them?
	 * This method returns a Location telling us just that.
	 * It might also return null if the gate exit is blocked.
	 */
	public Location getMyOwnExitLocation()
	{
		Block overSourceBlock = sourceCoord.getBlock().getRelative(BlockFace.UP);
		Location firstChoice;
		Location secondChoice;
		
		if (frameDirIsNS)
		{
			firstChoice = overSourceBlock.getRelative(BlockFace.EAST).getLocation();
			firstChoice.setYaw(180);
			
			secondChoice = overSourceBlock.getRelative(BlockFace.WEST).getLocation();
			secondChoice.setYaw(0);
		}
		else
		{
			firstChoice = overSourceBlock.getRelative(BlockFace.NORTH).getLocation();
			firstChoice.setYaw(90);
			
			secondChoice = overSourceBlock.getRelative(BlockFace.SOUTH).getLocation();
			secondChoice.setYaw(270);
		}
		
		// We want to stand in the middle of the block. Not in the corner.
		firstChoice.add(0.5, 0, 0.5);
		secondChoice.add(0.5, 0, 0.5);
		
		firstChoice.setPitch(0);
		secondChoice.setPitch(0);
		
		if (BlockUtil.canPlayerStandInBlock(firstChoice.getBlock()))
		{
			return firstChoice;
		}
		else if (BlockUtil.canPlayerStandInBlock(secondChoice.getBlock()))
		{
			return secondChoice;
		}
		
		return null;
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
	public int compareTo(OutGate o)
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

  private static void println(Object c) {
    System.out.println(c);
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
