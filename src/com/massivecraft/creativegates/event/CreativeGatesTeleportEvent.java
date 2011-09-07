package com.massivecraft.creativegates.event;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;

@SuppressWarnings("serial")
public class CreativeGatesTeleportEvent extends Event implements Cancellable
{
    
    private Location location;
    private Iterable<Integer> key;
    private boolean cancelled;
    private PlayerMoveEvent event;

    public CreativeGatesTeleportEvent(PlayerMoveEvent event, Location location, Iterable<Integer> key)
    {
        super("CreativeGatesTeleportEvent");
        this.event = event;
        this.location = location;
        this.key = key;
        this.cancelled = false;
    }
    
    public boolean isCancelled()
    {
        return this.cancelled;
    }
    
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }
    
    public Location getLocation()
    {
        return this.location;
    }
    
    public void setLocation(Location location)
    {
        this.location = location;
    }
    
    public Iterable<Integer> getKey()
    {
        return this.key;
    }
    
    public PlayerMoveEvent getPlayerMoveEvent()
    {
        return this.event;
    }
}
