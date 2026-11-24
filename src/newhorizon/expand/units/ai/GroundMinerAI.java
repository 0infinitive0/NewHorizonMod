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
 * <p>It goes for the highest tier first and leaves an item alone once the core is full of it ({@link MinerPriority}).
 *
 * <p>It goes for the ore nearest the core, however far it is, as long as the path finder finds a way there ({@link #maxOreDistance} can set a limit),
 * and an item whose ore cannot be reached is left alone for
 * {@link #skipTime} ticks while it picks another item, so it does not stand against a wall forever.
 */
public class GroundMinerAI extends MinerAI{
	/** How far from the core it will look for ore, in world units. No limit by default: titanium and thorium are often far from the core. */
	public float maxOreDistance = Float.MAX_VALUE;
	/** Ticks an item with no reachable ore is left out of its choices. */
	public float skipTime = 60f * 30f;
	/**
	 * How near the core it hands the load over from. The base game lets a miner (and a player's own unit) hand over from this far, and a core in
	 * a base is shut in by the player's own buildings, which a walker cannot pass, so it could never get next to it.
	 */
	public float depositRange = Vars.mineTransferRange;

	/** The tile (item, tile) it chose is far from or unreachable from the core; time until the item may be tried again. */
	protected final ObjectFloatMap<Item> skipUntil = new ObjectFloatMap<>();
	private final Vec2 goal = new Vec2();
	/** Set by {@link #walkTo} when the path finder says there is no way there. */
	public boolean noPath;
	protected boolean stancesDone;

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

	/** The item to go for now: the highest tier the core is not full of (see {@link MinerPriority}), or null. */
	protected Item chooseItem(Building core, CommandAI ai){
		if(byStance(ai)){
			return MinerPriority.pick(Vars.content.items(), core, i -> wanted(i) && ai.hasStance(mindustry.ai.ItemUnitStance.getByItem(i)));
		}
		return MinerPriority.pick(unit.type.mineItems, core, this::wanted);
	}

	/** A Command AI starts the base game's own Mine command with "mine anything" on; this command has to do that itself. */
	protected void initStances(CommandAI ai){
		if(stancesDone) return;
		stancesDone = true;
		if(ai != null && !ai.hasStance(UnitStance.mineAuto) && !mindustry.ai.ItemUnitStance.all().contains(ai::hasStance)){
			ai.setStance(UnitStance.mineAuto, true);
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
		initStances(ai);

		if(mining){
			if(timer.get(timerTarget2, 60 * 4) || targetItem == null || MinerPriority.full(core, targetItem)){
				targetItem = chooseItem(core, ai);
			}

			//carrying what the core is full of: there is no use in taking it home
			if(unit.stack.amount > 0 && MinerPriority.full(core, unit.stack.item)){
				unit.clearItem();
			}

			//nothing to mine (the core is full of all of it, or there is no ore): hand over what it carries, else wait
			if(targetItem == null){
				unit.mineTile = null;
				if(unit.stack.amount > 0) mining = false;
				return;
			}

			//full: take it home
			if(unit.stack.amount >= unit.type.itemCapacity || !unit.acceptsItem(targetItem)){
				mining = false;
			}else{
				if(timer.get(timerTarget3, 60)){
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

				if(ore != null && targetItem != null){
					if(!walkTo(ore, unit.type.mineRange / 2f)){
						//the ore cannot be reached: leave this item alone for a while and choose again
						skipUntil.put(targetItem, Time.time + skipTime);
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

			if(unit.within(core, depositRange)){
				if(!MinerPriority.full(core, unit.stack.item) && core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
					Call.transferItemTo(unit, unit.stack.item, unit.stack.amount, unit.x, unit.y, core);
				}
				unit.clearItem();
				mining = true;
			}else if(!walkTo(core, depositRange * 0.8f)){
				//no way through to the core (it is shut in): get as near as the walls allow, which is still in reach from outside
				moveTo(core, depositRange * 0.8f, 20f);
			}
		}
	}
}
