package newhorizon.expand.block.production;

import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import newhorizon.expand.block.env.IceFloor;

/** Dredges sand from the bottom of the sea. It only works in the shallows, where the sand is within reach. */
public class SandDredger extends GenericCrafter{
	public SandDredger(String name){
		super(name);
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
}
