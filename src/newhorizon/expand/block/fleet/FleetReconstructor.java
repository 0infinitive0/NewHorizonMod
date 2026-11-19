package newhorizon.expand.block.fleet;

import arc.struct.ObjectFloatMap;
import arc.struct.ObjectMap;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.consumers.ConsumeItemDynamic;
import newhorizon.expand.units.ablility.DroneBayAbility;
import newhorizon.expand.units.ai.DroneAI;

/**
 * A reconstructor for the fleet: <b>one block for every upgrade</b>, each upgrade with <b>its own cost and its own time</b> ({@link #addUpgrade}; the
 * base game's reconstructor has one cost and one time for the whole block, which is why it needs a block for each tier). A new upgrade is one line.
 *
 * <ul>
 * <li><b>Cost per step</b>: the items taken are those of the upgrade the unit inside is going through. They are worked out from the unit that is
 * <i>going in</i> and kept until the step is done, because the base class swaps in the new unit just before it takes the items.</li>
 * <li><b>Time per step</b>: the build speed is scaled by the block's base time over the step's time.</li>
 * <li><b>One step at a time</b>: a unit that has just been upgraded is let out, not fed into the next step, so the player decides when to pay for it.</li>
 * <li><b>A Mothership's drones</b> that are left outside wait at the reconstructor (see {@link DroneAI}) and are taken over by the new ship: the
 * drone type the old ship had chosen is copied to the new one, and drones of any other type that are docked here are taken in (removed) when the
 * step is done, instead of wasting away.</li>
 * </ul>
 */
public class FleetReconstructor extends Reconstructor{
	/** The cost and the time (ticks) of the upgrade from each unit type. */
	public final ObjectMap<UnitType, ItemStack[]> costs = new ObjectMap<>();
	public final ObjectFloatMap<UnitType> times = new ObjectFloatMap<>();
	protected static final ItemStack[] none = {};

	public FleetReconstructor(String name){
		super(name);
		consume(new ConsumeItemDynamic((FleetReconstructorBuild b) -> b.cost()));
	}

	/** Adds an upgrade with its own cost and time (in seconds). */
	public void addUpgrade(UnitType from, UnitType to, ItemStack[] cost, float seconds){
		addUpgrade(from, to);
		costs.put(from, cost);
		times.put(from, seconds * 60f);
	}

	@Override
	public void initCapacities(){
		super.initCapacities();
		for(var e : costs){
			for(ItemStack stack : e.value){
				capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
				itemCapacity = Math.max(itemCapacity, stack.amount * 2);
			}
		}
	}

	@Override
	public void init(){
		super.init();
		//the items it takes are decided at run time, so the game has to be told which they can be
		for(var e : costs) for(ItemStack stack : e.value) itemFilter[stack.item.id] = true;
	}

	/** The step time of an upgrade, or the block's own time for one that has none of its own. */
	public float timeOf(UnitType from){
		return times.get(from, constructTime);
	}

	/** The friendly Fleet Reconstructor nearest a drone that has a Mothership inside it, within {@code range}; null if none. */
	public static FleetReconstructorBuild findFor(Unit drone, float range){
		FleetReconstructorBuild[] best = {null};
		Vars.indexer.eachBlock(drone.team, drone.x, drone.y, range, b -> b instanceof FleetReconstructorBuild f && f.hasShip(), b -> {
			if(best[0] == null || b.dst2(drone) < best[0].dst2(drone)) best[0] = (FleetReconstructorBuild)b;
		});
		return best[0];
	}

	public class FleetReconstructorBuild extends ReconstructorBuild{
		/** The type being upgraded, kept for the tick on which its unit is swapped for the new one. */
		protected UnitType working;
		/** The unit has just been upgraded: it leaves instead of going into the next step. */
		protected boolean fresh;

		/** What the step the unit inside is going through costs. */
		public ItemStack[] cost(){
			UnitType t = working;
			if(t == null && payload != null && !fresh && hasUpgrade(payload.unit.type)) t = payload.unit.type;
			ItemStack[] c = t == null ? null : costs.get(t);
			return c == null ? none : c;
		}

		/** Whether a Mothership is inside (the drones outside wait for it). */
		public boolean hasShip(){
			return payload != null && DroneBayAbility.of(payload.unit) != null;
		}

		@Override
		public boolean hasUpgrade(UnitType type){
			return !fresh && super.hasUpgrade(type);
		}

		@Override
		public float edelta(){
			float f = super.edelta();
			if(payload == null || fresh) return f;
			float t = timeOf(payload.unit.type);
			return t <= 0f ? f : f * constructTime / t;
		}

		@Override
		public void updateTile(){
			Unit before = payload == null ? null : payload.unit;
			working = before != null && !fresh && super.hasUpgrade(before.type) ? before.type : null;
			super.updateTile();
			if(payload == null){
				fresh = false;
				working = null;
				return;
			}
			if(before != null && payload.unit != before){
				fresh = true;
				carryOver(before, payload.unit);
			}
		}

		/** The new ship takes over what the old one had chosen, and the drones docked here of any other type are taken in. */
		protected void carryOver(Unit oldShip, Unit newShip){
			DroneBayAbility ob = DroneBayAbility.of(oldShip), nb = DroneBayAbility.of(newShip);
			if(ob == null || nb == null) return;
			nb.data = ob.data;
			nb.standDown = ob.standDown;
			nb.switchLeft = ob.switchLeft;
			UnitType want = nb.type();
			for(Unit u : Groups.unit){
				if(u.team == team && u.controller() instanceof DroneAI ai && ai.dockedAt == this && u.type != want){
					Fx.spawn.at(u.x, u.y);
					u.remove();
				}
			}
		}
	}
}
