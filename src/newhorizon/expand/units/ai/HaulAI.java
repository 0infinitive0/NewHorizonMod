package newhorizon.expand.units.ai;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.world.Tile;

import static mindustry.Vars.tilesize;

/** Movement that the hauling units share: to a block, along a path if the unit is a boat. */
public abstract class HaulAI extends AIController{
	/** Extra reach on top of the size of the block, in world units. */
	public float reach = 14f;
	
	protected final Vec2 dest = new Vec2();
	protected Building approachTarget;
	protected float approachTime;
	
	/** How close the unit has to be to a block to work with it. Bigger units stay further away. */
	protected float nearRadius(Building build){
		return build.block.size * tilesize / 2f + reach + unit.hitSize * 0.75f;
	}
	
	protected boolean near(Building build){
		return unit.within(build, nearRadius(build));
	}
	
	/** Moves towards a block. Boats follow a path through the water, as they cannot go through anything else. */
	protected void travel(Building target){
		//Close enough: stop, a big ship would otherwise coast right past.
		if(near(target)){
			unit.vel.scl(Math.max(0f, 1f - 0.2f * Time.delta));
			return;
		}
		
		if(!unit.type.naval){
			moveTo(target, target.block.size * tilesize / 2f + reach * 0.5f, 20f);
			return;
		}
		
		Vec2 approach = approachPoint(target);
		//Ships turn slowly, so they have to slow down early or they circle the target without ever getting there.
		float slow = Math.max(40f, unit.hitSize * 2f);
		if(unit.within(approach, slow)){
			moveTo(approach, 0f, slow);
			return;
		}
		
		var result = Vars.controlPath.getPathPosition(unit, approach);
		if(result.move){
			moveTo(result.dest, 0f, 40f);
		}
	}
	
	/**
	 * The water tile next to the block that is closest to the unit and has room for the whole unit around it, so big ships
	 * do not try to reach a tile they cannot fit on. The result is kept for a moment, as it takes a lot of checks.
	 */
	protected Vec2 approachPoint(Building target){
		if(target == approachTarget && Time.time - approachTime < 300f)return dest;
		approachTarget = target;
		approachTime = Time.time;
		
		int size = target.block.size;
		int clear = Math.max(0, (int)Math.ceil(unit.hitSize / 2f / tilesize));
		float reachRadius = nearRadius(target);
		int span = (int)Math.ceil(reachRadius / tilesize) + 1;
		float best = Float.MAX_VALUE, bestAny = Float.MAX_VALUE;
		float anyX = target.x, anyY = target.y;
		dest.set(target.x, target.y);
		boolean found = false;
		
		for(int dx = -span; dx <= span; dx++){
			for(int dy = -span; dy <= span; dy++){
				Tile tile = Vars.world.tile(target.tile.x + dx, target.tile.y + dy);
				if(tile == null || !room(tile, clear))continue;
				
				float d = unit.dst2(tile.worldx(), tile.worldy());
				boolean inReach = Mathf.within(tile.worldx(), tile.worldy(), target.x, target.y, reachRadius - 2f);
				if(inReach && d < best){
					best = d;
					dest.set(tile.worldx(), tile.worldy());
					found = true;
				}else if(d < bestAny){
					bestAny = d;
					anyX = tile.worldx();
					anyY = tile.worldy();
				}
			}
		}
		
		if(!found)dest.set(anyX, anyY);
		return dest;
	}
	
	/** @return whether every tile around this one, up to the radius, can be passed by the unit. */
	protected boolean room(Tile tile, int radius){
		for(int x = -radius; x <= radius; x++){
			for(int y = -radius; y <= radius; y++){
				if(!unit.canPass(tile.x + x, tile.y + y))return false;
			}
		}
		return true;
	}
}
