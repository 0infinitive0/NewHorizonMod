package newhorizon.expand.block.production;

import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import newhorizon.expand.block.env.IceFloor;

/**
 * Dredges sand from the bottom of the sea. It only works in the shallows, where the sand is within reach.
 * Power is optional and doubles the speed.
 */
public class SandDredger extends GenericCrafter{
	/** Power per tick that doubles the speed. It is not needed: without it the dredger runs at the normal speed. */
	public float boostPower = 1.2f;
	
	public SandDredger(String name){
		super(name);
	}
	
	@Override
	public void init(){
		hasPower = true;
		consumePower(boostPower).optional(true, true);
		super.init();
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add(Stat.boostEffect, 2f, StatUnit.timesSpeed);
	}
	
	/** @return whether the tile is shallow water, frozen or not. */
	public static boolean shallow(Tile tile){
		var floor = tile.floor();
		if(floor instanceof IceFloor)floor = ((IceFloor)floor).melted;
		return floor.isLiquid && !floor.isDeep() && floor.liquidDrop == mindustry.content.Liquids.water;
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, Team team, int rotation){
		for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
			if(!shallow(other))return false;
		}
		return true;
	}
	
	public class SandDredgerBuild extends GenericCrafterBuild{
		/** 1 without power, 2 with all the power it can use. */
		public float boost(){
			return 1f + optionalEfficiency;
		}
		
		@Override
		public float getProgressIncrease(float baseTime){
			return super.getProgressIncrease(baseTime) * boost();
		}
	}
}
