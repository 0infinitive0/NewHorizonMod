package newhorizon.expand.units.ai;

import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.world.Tile;
import newhorizon.expand.block.cargo.Depot;
import newhorizon.expand.block.cargo.Port;

import static mindustry.Vars.tilesize;

/**
 * Hauls items from the port the unit is bound to into the depots. A unit that is not bound to a port does nothing.
 */
public class CargoHaulAI extends AIController{
	public Port.PortBuild port;
	public Depot.DepotBuild depot;
	/** Extra reach on top of the size of the block, in world units. */
	public float reach = 14f;
	
	protected float waited;
	protected final Vec2 dest = new Vec2();
	
	@Override
	public void updateMovement(){
		if(port == null || !port.isValid() || !port.bound.contains(unit.id) || timer.get(timerTarget2, 45f)){
			port = Port.boundPort(unit);
		}
		
		if(port == null){
			unit.mineTile = null;
			return;
		}
		
		if(unit.stack.amount <= 0){
			depot = null;
			waited = 0f;
			
			if(!near(port))travel(port);
			if(near(port) && timer.get(timerTarget3, 15f)){
				Item item = bestItem();
				if(item != null){
					depot = closestDepot(item);
					Call.takeItems(port, item, Math.min(unit.type.itemCapacity, port.items.get(item)), unit);
				}
			}
		}else{
			Item item = unit.item();
			if(depot == null || !depot.isValid() || !depot.wants(item)){
				depot = timer.get(timerTarget4, 30f) ? closestDepot(item) : null;
			}
			
			if(depot == null){
				//Nowhere to go with it: wait at the port, and put it back if that takes too long.
				if(!near(port))travel(port);
				waited += arc.util.Time.delta;
				if(waited > 600f && near(port) && !Vars.net.client()){
					port.handleStack(item, unit.stack.amount, unit);
					unit.clearItem();
				}
				return;
			}
			
			waited = 0f;
			travel(depot);
			if(near(depot)){
				int accepted = depot.acceptStack(item, unit.stack.amount, unit);
				if(accepted > 0)Call.transferItemTo(unit, item, accepted, unit.x, unit.y, depot);
			}
		}
	}
	
	protected boolean near(Building build){
		return unit.within(build, build.block.size * tilesize / 2f + reach);
	}
	
	/** The item with the largest stock in the port that some depot wants. */
	protected Item bestItem(){
		Item best = null;
		int most = 0;
		for(Item item : Vars.content.items()){
			int amount = port.items.get(item);
			if(amount > most && closestDepot(item) != null){
				best = item;
				most = amount;
			}
		}
		return best;
	}
	
	protected Depot.DepotBuild closestDepot(Item item){
		Depot.DepotBuild closest = null;
		float dst = Float.MAX_VALUE;
		for(Depot block : Depot.all){
			for(Building build : unit.team.data().getBuildings(block)){
				Depot.DepotBuild candidate = (Depot.DepotBuild)build;
				if(!candidate.wants(item))continue;
				float d = candidate.dst2(port);
				if(d < dst){
					dst = d;
					closest = candidate;
				}
			}
		}
		return closest;
	}
	
	/** Moves towards a block. Boats follow a path through the water, as they cannot go through anything else. */
	protected void travel(Building target){
		if(!unit.type.naval){
			moveTo(target, target.block.size * tilesize / 2f + reach * 0.5f, 20f);
			return;
		}
		
		Vec2 approach = approachPoint(target);
		if(unit.within(approach, tilesize * 3f)){
			moveTo(approach, 0f, 20f);
			return;
		}
		
		var result = Vars.controlPath.getPathPosition(unit, approach);
		if(result.move){
			moveTo(result.dest, 0f, 40f);
		}
	}
	
	/** The water tile right next to the block that is closest to the unit. */
	protected Vec2 approachPoint(Building target){
		int size = target.block.size;
		//Blocks with an even size have their origin tile in the lower left of the middle.
		int minX = target.tile.x - (size - 1) / 2, minY = target.tile.y - (size - 1) / 2;
		int maxX = minX + size - 1, maxY = minY + size - 1;
		float best = Float.MAX_VALUE;
		dest.set(target.x, target.y);
		
		for(int x = minX - 1; x <= maxX + 1; x++){
			for(int y = minY - 1; y <= maxY + 1; y++){
				boolean inside = x >= minX && x <= maxX && y >= minY && y <= maxY;
				if(inside)continue;
				Tile tile = Vars.world.tile(x, y);
				if(tile == null || !unit.canPass(tile.x, tile.y))continue;
				float d = unit.dst2(tile.worldx(), tile.worldy());
				if(d < best){
					best = d;
					dest.set(tile.worldx(), tile.worldy());
				}
			}
		}
		return dest;
	}
}
