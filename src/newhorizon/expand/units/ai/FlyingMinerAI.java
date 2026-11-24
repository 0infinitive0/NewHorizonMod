package newhorizon.expand.units.ai;

import arc.math.geom.Position;
import mindustry.Vars;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.gen.Building;

import static mindustry.Vars.indexer;

/**
 * {@link GroundMinerAI} for a unit that flies (the Omni Miner, the Gather): the same item choice, but it flies straight at the ore (no path
 * finder), and it also mines ore in walls (omni-matter deposits), as the base game's miner does.
 */
public class FlyingMinerAI extends GroundMinerAI{
	@Override
	protected Tile findOre(Building core, Item item){
		Tile t = null;
		if(unit.type.mineFloor) t = indexer.findClosestOre(core.x, core.y, item);
		if(t == null && unit.type.mineWalls) t = indexer.findClosestWallOre(core.x, core.y, item);
		return t;
	}

	@Override
	protected boolean hasOre(Item item){
		return (unit.type.mineFloor && indexer.hasOre(item)) || (unit.type.mineWalls && indexer.hasWallOre(item));
	}

	@Override
	protected boolean wanted(Item item){
		return unit.canMine(item) && hasOre(item) && skipUntil.get(item, 0f) <= arc.util.Time.time;
	}

	@Override
	protected boolean walkTo(Position target, float stop){
		noPath = false;
		if(!unit.within(target, stop)) moveTo(target, stop, 20f);
		return true;
	}
}
