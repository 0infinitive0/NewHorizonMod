package newhorizon.expand.units.ai;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.ObjectFloatMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.MinerAI;
import mindustry.ai.UnitStance;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.type.Item;
import mindustry.world.Tile;

import static mindustry.Vars.indexer;
import static mindustry.Vars.tilesize;

/**
 * The auto-miner for ground units. The base game's {@link MinerAI} flies straight at the ore, which a walker cannot do, so this one
 * finds the ore closest to the core, <b>walks</b> there along the path finder's route (the same one the Command AI uses), mines, and walks
 * back to hand what it carries to the core.
 *
 * <p>It only goes for ore within {@link #maxOreDistance} of the core, and an item whose ore cannot be reached is left alone for
 * {@link #skipTime} ticks while it picks another item, so it does not stand against a wall forever.
 */
public class GroundMinerAI extends MinerAI{
	/** How far from the core it will look for ore, in world units. */
	public float maxOreDistance = 45f * tilesize;
	/** Ticks an item with no reachable ore is left out of its choices. */
	public float skipTime = 60f * 30f;

	/** The tile (item, tile) it chose is far from or unreachable from the core; time until the item may be tried again. */
	protected final ObjectFloatMap<Item> skipUntil = new ObjectFloatMap<>();
	private final Vec2 goal = new Vec2();
	/** Set by {@link #walkTo} when the path finder says there is no way there. */
	public boolean noPath;

	/** The ore tile closest to the core for an item, or null. Overridable (a test supplies its own). */
	protected Tile findOre(Building core, Item item){
		return indexer.findClosestOre(core.x, core.y, item);
	}

	protected boolean hasOre(Item item){
		return indexer.hasOre(item);
	}

	/** Whether it may choose this item now. */
	protected boolean wanted(Item item){
		return unit.type.mineFloor && unit.canMine(item) && hasOre(item) && skipUntil.get(item, 0f) <= Time.time;
	}

	/** Walks one step along the route to {@code target}; true when it is within {@code stop} of it or on its way, false when there is no way there. */
	protected boolean walkTo(Position target, float stop){
		noPath = false;
		if(unit.within(target, stop)) return true;
		goal.set(target.getX(), target.getY());
		var result = Vars.controlPath.getPathPosition(unit, goal);
		if(result.unreachable){
			noPath = true;
			return false;
		}
		if(result.move) moveTo(result.dest, 1f, 20f);
		return true;
	}

	/**
	 * Whether the player has told the unit, with its stance buttons, which items to mine. The base game's Command AI turns on "mine anything"
	 * by itself, but only for its own Mine command, not for this one, so a unit with no item stance at all just mines anything (it used to
	 * choose nothing and stand still).
	 */
	protected boolean byStance(CommandAI ai){
		return ai != null && !ai.hasStance(UnitStance.mineAuto) && mindustry.ai.ItemUnitStance.all().contains(ai::hasStance);
	}

	/** The base game drops the item and stops when the stances change to ones that do not allow it; only item stances can do that here. */
	@Override
	public void stanceChanged(){
		if(targetItem != null && unit.controller() instanceof CommandAI ai && byStance(ai) && !ai.hasStance(mindustry.ai.ItemUnitStance.getByItem(targetItem))){
			mining = false;
			targetItem = null;
		}
	}

	@Override
	public void updateMovement(){
		Building core = unit.closestCore();
		if(!unit.canMine() || core == null) return;

		if(!unit.validMine(unit.mineTile)){
			unit.mineTile(null);
		}

		CommandAI ai = unit.controller() instanceof CommandAI a ? a : null;

		if(mining){
			if(timer.get(timerTarget2, 60 * 4) || targetItem == null){
				if(byStance(ai)){
					targetItem = Vars.content.items().min(i -> wanted(i) && ai.hasStance(mindustry.ai.ItemUnitStance.getByItem(i)), i -> core.items.get(i));
				}else{
					targetItem = unit.type.mineItems.min(this::wanted, i -> core.items.get(i));
				}
			}

			//the core is full of it: nothing to do
			if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
				unit.clearItem();
				unit.mineTile = null;
				return;
			}

			//full: take it home
			if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
				mining = false;
			}else{
				if(timer.get(timerTarget3, 60) && targetItem != null){
					ore = findOre(core, targetItem);
					if(ore != null && Mathf.dst(ore.worldx(), ore.worldy(), core.x, core.y) > maxOreDistance){
						ore = null;
					}
					//no ore close enough: try another item for a while
					if(ore == null){
						skipUntil.put(targetItem, Time.time + skipTime);
						targetItem = null;
					}
				}

				if(ore != null){
					if(!walkTo(ore, unit.type.mineRange / 2f)){
						//the ore cannot be reached: leave this item alone for a while and choose again
						if(targetItem != null) skipUntil.put(targetItem, Time.time + skipTime);
						ore = null;
						targetItem = null;
					}else if(unit.within(ore, unit.type.mineRange) && unit.validMine(ore)){
						unit.mineTile = ore;
					}
				}
			}
		}else{
			unit.mineTile = null;

			if(unit.stack.amount == 0){
				mining = true;
				return;
			}

			float reach = core.hitSize() / 2f + 14f;
			if(unit.within(core, reach)){
				if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
					Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
				}
				unit.clearItem();
				mining = true;
			}else{
				walkTo(core, reach);
			}
		}
	}
}
