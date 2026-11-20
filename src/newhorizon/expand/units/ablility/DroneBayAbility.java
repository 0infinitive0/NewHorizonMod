package newhorizon.expand.units.ablility;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.UnitStance;
import mindustry.ai.types.CommandAI;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import newhorizon.expand.units.ai.DroneAI;
import newhorizon.expand.units.ai.RammerAI;

/**
 * The mothership's bay: it keeps {@link #count} bound drones of the <b>chosen type</b> (one of {@link #types}). The first ones leave one by one,
 * {@link #firstInterval} ticks apart, once the mothership has been flying for {@link #startDelay} ticks; one that is lost is replaced after
 * {@link #rebuildTime} ticks. Launching costs nothing and the drones do not count against the unit cap.
 *
 * <p><b>Changing the type</b> ({@link #select}, from the stance buttons of a commanded mothership, see {@link #syncStances}, or from a pilot's key):
 * the drones that are out are <b>recalled</b>: they fly back to the mothership and are absorbed (no refund; any still out after {@link #recallTimeout}
 * ticks are removed). Nothing launches until the last is gone, then the bay starts afresh with four of the new type. A change is refused for
 * {@link #switchCooldown} ticks after the last one. The chosen type is kept in {@code data}, so it is saved with the unit.
 *
 * <p>A drone whose mothership is gone (or that was saved and loaded: the game does not save what a unit is bound to) looks for a mothership
 * with a free slot of its own type ({@link #adopt}), and the bay waits the start delay before launching, so that the ones that were already
 * out are adopted first.
 */
public class DroneBayAbility extends Ability{
	/** The drone types the bay can carry (in the order of the stance buttons), and the stance that picks each. */
	public final Seq<UnitType> types;
	public final Seq<UnitStance> stances;
	/** The Stand down button (null for a bay without one). */
	public UnitStance standDownStance;
	/** Stand down: the drones dock beside the Mothership and do not attack. Set by the stance button of a commanded Mothership, or by a pilot's key. */
	public boolean standDown;
	/** The stand down was turned on by the bay itself (the fleet was empty), and is turned off by it when the fleet is full again. */
	public boolean autoHold;
	/** The drone count the bay last looked at (it looks again only when the count changes), and how many times it has looked. */
	public int lastCount = -1, countChecks;
	public int count = 4;
	/** Ticks between the first launches, ticks before the first, and ticks to replace a lost drone. */
	public float firstInterval = 90f, startDelay = 120f, rebuildTime = 60f * 40f;
	/** Ticks a change of type is refused after the last, and ticks a recall may take before what is left is removed. */
	public float switchCooldown = 60f * 30f, recallTimeout = 60f * 10f;
	public Color color = Color.valueOf("ff9a4d");

	/** The drones bound to this mothership (a copy of the ability per unit has its own list). */
	public Seq<Unit> bound = new Seq<>();
	/** How many have been launched in all, and the launch timer (it runs while a slot is empty). */
	public int launched;
	public float timer;
	/** Whether a recall is under way, ticks until it gives up, and ticks until another change is allowed. */
	public boolean recalling;
	public float recallLeft, switchLeft;

	public DroneBayAbility(int count, Seq<UnitType> types, Seq<UnitStance> stances){
		this.count = count;
		this.types = types;
		this.stances = stances;
		reset();
	}

	/** Back to a fresh bay: the first launch comes after the start delay. */
	public void reset(){
		timer = firstInterval - startDelay;
		launched = 0;
	}

	@Override
	public Ability copy(){
		DroneBayAbility c = (DroneBayAbility)super.copy();
		c.bound = new Seq<>();
		c.lastCount = -1;
		c.autoHold = false;
		c.reset();
		return c;
	}

	/** The index of the chosen type. */
	public int index(){
		return Mathf.clamp((int)data, 0, Math.max(0, types.size - 1));
	}

	/** The chosen type. */
	public UnitType type(){
		return types.get(index());
	}

	public int freeSlots(){
		bound.removeAll(u -> !u.isValid());
		return count - bound.size;
	}

	/**
	 * Changes the type of drone. False (and nothing happens) when it is the type already chosen, when the last change was less than
	 * {@link #switchCooldown} ago, or when there is no such type.
	 */
	public boolean select(int i){
		if(i < 0 || i >= types.size || i == index() || switchLeft > 0f) return false;
		data = i;
		switchLeft = switchCooldown;
		bound.removeAll(u -> !u.isValid());
		for(Unit u : bound){
			if(u.controller() instanceof DroneAI ai) ai.recalling = true;
		}
		recalling = !bound.isEmpty();
		recallLeft = recallTimeout;
		if(!recalling) reset();
		return true;
	}

	/** A recalled drone reached the mothership: it is taken in. */
	public void absorb(Unit drone){
		bound.remove(drone);
		Fx.spawn.at(drone.x, drone.y);
		drone.remove();
	}

	@Override
	public void update(Unit mother){
		if(Vars.net.client() || !mother.isValid()) return;
		switchLeft = Math.max(0f, switchLeft - Time.delta);
		syncStances(mother);
		bound.removeAll(u -> !u.isValid());
		if(bound.size != lastCount){
			lastCount = bound.size;
			countChecks++;
			countChanged(mother, lastCount);
		}

		if(recalling){
			bound.removeAll(u -> !u.isValid());
			recallLeft -= Time.delta;
			if(recallLeft <= 0f){
				//the ones that could not get home are removed where they are
				for(Unit u : bound.copy()) absorb(u);
			}
			if(bound.isEmpty()){
				recalling = false;
				reset();
			}
			return;
		}

		if(freeSlots() <= 0){
			timer = 0f;
			return;
		}
		timer += Time.delta;
		if(timer >= (launched < count ? firstInterval : rebuildTime)){
			timer = 0f;
			launched++;
			launch(mother);
		}
	}

	/**
	 * The type is chosen with the stance buttons of a selected mothership: when a button other than the current type's is switched on, the bay
	 * takes it; whatever happens, the button of the current type is lit again afterwards (so a refused change shows as it is).
	 */
	public void syncStances(Unit mother){
		if(stances == null || !(mother.controller() instanceof CommandAI ai)) return;
		int cur = index();
		for(int i = 0; i < stances.size; i++){
			if(i != cur && ai.hasStance(stances.get(i))){
				select(i);
				break;
			}
		}
		//the button was pressed since the last look: a player's choice, which the automatic stand down must not undo
		if(standDownStance != null && ai.hasStance(standDownStance) != standDown){
			standDown = !standDown;
			autoHold = false;
		}
		UnitStance now = stances.get(index());
		if(!ai.hasStance(now)) ai.setStance(now, true);
		for(int i = 0; i < stances.size; i++) if(i != index() && ai.hasStance(stances.get(i))) ai.setStance(stances.get(i), false);
	}

	/** A player's Stand down (the button, or the pilot's key): it stays as chosen, whatever the fleet does. */
	public void setStandDown(boolean on){
		standDown = on;
		autoHold = false;
	}

	/**
	 * The drone count changed. With none left the bay turns Stand down on by itself, so that the drones that come out park instead of flying off one by
	 * one; when the fleet is full again it turns that off, if it was the one that turned it on.
	 */
	protected void countChanged(Unit mother, int n){
		if(n == 0){
			if(!standDown){
				standDown = true;
				autoHold = true;
				showStance(mother, true);
			}
		}else if(n >= count && autoHold){
			standDown = false;
			autoHold = false;
			showStance(mother, false);
		}
	}

	/** Lights or clears the Stand down button of a commanded mothership. */
	protected void showStance(Unit mother, boolean on){
		if(standDownStance != null && mother.controller() instanceof CommandAI ai) ai.setStance(standDownStance, on);
	}

	/** Sends one drone of the chosen type out of the back of the mothership. */
	public Unit launch(Unit mother){
		Unit u = type().create(mother.team);
		Tmp.v1.trns(mother.rotation + 180f, mother.hitSize * 0.35f).add(mother.x, mother.y);
		u.set(Tmp.v1.x + Mathf.range(4f), Tmp.v1.y + Mathf.range(4f));
		u.rotation = mother.rotation;
		u.vel.set(mother.vel);
		DroneAI ai = u.type.aiController.get() instanceof DroneAI d ? d : new RammerAI();
		ai.owner = mother;
		ai.bay = this;
		u.controller(ai);
		u.add();
		bound.add(u);
		Fx.spawn.at(u.x, u.y);
		return u;
	}

	/** The bay of a unit, or null. */
	public static DroneBayAbility of(Unit u){
		if(u == null || u.abilities == null) return null;
		for(Ability a : u.abilities) if(a instanceof DroneBayAbility b) return b;
		return null;
	}

	/** Binds a drone to the nearest friendly mothership that has a free slot for its type and is not changing type; false when there is none. */
	public static boolean adopt(Unit drone, DroneAI ai, float range){
		DroneBayAbility[] best = {null};
		Unit[] bestUnit = {null};
		Units.nearby(drone.team, drone.x - range, drone.y - range, range * 2f, range * 2f, other -> {
			if(other == drone || !other.isValid()) return;
			DroneBayAbility b = of(other);
			if(b == null || b.type() != drone.type || b.recalling || b.freeSlots() <= 0) return;
			if(bestUnit[0] == null || other.dst2(drone) < bestUnit[0].dst2(drone)){
				bestUnit[0] = other;
				best[0] = b;
			}
		});
		if(best[0] == null) return false;
		best[0].bound.add(drone);
		ai.owner = bestUnit[0];
		ai.bay = best[0];
		return true;
	}

	@Override
	public void displayBars(Unit mother, Table bars){
		bars.add(new Bar(() -> Core.bundle.format("bar.nh-drones", type().localizedName, bound.size, count), () -> color, () -> bound.size / (float)Math.max(1, count))).row();
	}

	@Override
	public void addStats(Table t){
		super.addStats(t);
		StringBuilder names = new StringBuilder();
		for(UnitType u : types) names.append(names.length() == 0 ? "" : ", ").append(u.localizedName);
		t.add(Core.bundle.format("ability.nh-dronebay.stats", count, names, Strings.fixed(rebuildTime / 60f, 0), Strings.fixed(switchCooldown / 60f, 0))).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-dronebay", "Drone bay");
	}
}
