package newhorizon.expand.units.ai;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.ObjectFloatMap;
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
	
	/** How far from a block the closest tile with room for a ship may be, in tiles, before the block counts as out of reach. */
	public static int farSearch = 14;
	/** Blocks that ships could not get to, and until when (in game ticks) they are left alone. */
	protected static final ObjectFloatMap<Building> ignored = new ObjectFloatMap<>();
	
	protected final Vec2 dest = new Vec2();
	protected Building approachTarget;
	protected float approachTime;
	/** When the block is boxed in, the radius from which the unit works with it, as it can not get any closer. */
	protected Building dockFor;
	protected float dockRadius;
	protected float stuck;
	
	/** How close the unit has to be to a block to work with it. Bigger units stay further away. */
	protected float nearRadius(Building build){
		float radius = build.block.size * tilesize / 2f + reach + unit.hitSize * 0.75f;
		return build == dockFor ? Math.max(radius, dockRadius) : radius;
	}
	
	/** Leaves a block alone for a while, as the unit could not get to it. */
	public static void ignore(Building build, float ticks){
		ignored.put(build, Time.time + ticks);
	}
	
	public static boolean ignored(Building build){
		float until = ignored.get(build, 0f);
		if(until <= 0f)return false;
		if(until <= Time.time){
			ignored.remove(build, 0f);
			return false;
		}
		return true;
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
			stuck = 0f;
			moveTo(result.dest, 0f, 40f);
		}else{
			//No way to the point: stop, and leave the block alone for a while if it stays that way.
			unit.vel.scl(Math.max(0f, 1f - 0.2f * Time.delta));
			stuck += Time.delta;
			if(stuck > 180f){
				stuck = 0f;
				ignore(target, 1800f);
			}
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
		float best = Float.MAX_VALUE;
		dest.set(target.x, target.y);
		boolean found = false;
		
		for(int dx = -span; dx <= span; dx++){
			for(int dy = -span; dy <= span; dy++){
				Tile tile = Vars.world.tile(target.tile.x + dx, target.tile.y + dy);
				if(tile == null || !Mathf.within(tile.worldx(), tile.worldy(), target.x, target.y, reachRadius - 2f) || !room(tile, clear))continue;
				
				float d = unit.dst2(tile.worldx(), tile.worldy());
				if(d < best){
					best = d;
					dest.set(tile.worldx(), tile.worldy());
					found = true;
				}
			}
		}
		
		if(found){
			dockFor = null;
			return dest;
		}
		
		//Boxed in: the closest tile to the block that has room for the unit, further out. The unit works with the block from there.
		float closest = Float.MAX_VALUE;
		int wide = Math.max(span, farSearch);
		for(int dx = -wide; dx <= wide; dx++){
			for(int dy = -wide; dy <= wide; dy++){
				Tile tile = Vars.world.tile(target.tile.x + dx, target.tile.y + dy);
				if(tile == null || !room(tile, clear))continue;
				
				float d = Mathf.dst2(tile.worldx(), tile.worldy(), target.x, target.y);
				if(d < closest){
					closest = d;
					dest.set(tile.worldx(), tile.worldy());
					found = true;
				}
			}
		}
		
		if(found){
			dockFor = target;
			dockRadius = (float)Math.sqrt(closest) + tilesize * 1.5f;
		}else{
			dockFor = null;
			ignore(target, 1800f);
		}
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
