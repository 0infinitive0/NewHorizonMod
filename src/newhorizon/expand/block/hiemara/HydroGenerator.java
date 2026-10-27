package newhorizon.expand.block.hiemara;

import arc.math.Mathf;
import mindustry.content.Liquids;
import mindustry.game.Team;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.meta.Stat;

/**
 * Makes power from the water that flows under it. Deep water gives the most, shallow water less, and ice nothing at all,
 * so a cold wave shuts it down.
 */
public class HydroGenerator extends PowerGenerator{
	/** Output of a tile of shallow water compared to a tile of deep water. */
	public float shallowShare = 0.6f;
	
	public HydroGenerator(String name){
		super(name);
		
		placeableLiquid = true;
	}
	
	/** @return how much a tile gives, from 0 to 1. */
	public float flow(Tile tile){
		Floor floor = tile.floor();
		if(!floor.isLiquid || floor.liquidDrop != Liquids.water)return 0f;
		return floor.isDeep() ? 1f : shallowShare;
	}
	
	@Override
	public boolean canPlaceOn(Tile tile, Team team, int rotation){
		for(Tile other : tile.getLinkedTilesAs(this, tempTiles)){
			if(flow(other) > 0f)return true;
		}
		return false;
	}
	
	public class HydroGeneratorBuild extends GeneratorBuild{
		public float flow;
		
		@Override
		public void onProximityUpdate(){
			super.onProximityUpdate();
			
			float sum = 0f;
			for(Tile other : tile.getLinkedTiles(new arc.struct.Seq<>())){
				sum += flow(other);
			}
			flow = sum / (size * size);
		}
		
		@Override
		public void placed(){
			super.placed();
			onProximityUpdate();
		}
		
		@Override
		public void updateTile(){
			productionEfficiency = enabled ? flow : 0f;
		}
		
		@Override
		public float warmup(){
			return Mathf.clamp(flow);
		}
	}
}
