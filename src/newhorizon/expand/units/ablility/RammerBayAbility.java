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
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import newhorizon.expand.units.ai.RammerAI;

/**
 * The mothership's bay: it keeps {@link #count} bound Rammers. The first ones leave one by one, {@link #firstInterval} ticks apart, once the
 * mothership has been flying for {@link #startDelay} ticks; one that is lost is replaced after {@link #rebuildTime} ticks. Launching costs nothing
 * and the Rammers do not count against the unit cap.
 *
 * <p>A Rammer whose mothership is gone (or that was saved and loaded: the game does not save what a unit is bound to) looks for a mothership
 * with a free slot ({@link #adopt}), and the bay waits the start delay before launching, so that the ones that were already out are adopted first.
 */
public class RammerBayAbility extends Ability{
	public UnitType unit;
	public int count = 4;
	/** Ticks between the first launches, ticks before the first, and ticks to replace a lost Rammer. */
	public float firstInterval = 90f, startDelay = 120f, rebuildTime = 60f * 40f;
	public Color color = Color.valueOf("ff9a4d");

	/** The Rammers bound to this mothership (a copy of the ability per unit has its own list). */
	public Seq<Unit> bound = new Seq<>();
	/** How many have been launched in all, and the launch timer (it runs while a slot is empty). */
	public int launched;
	public float timer;

	public RammerBayAbility(UnitType unit, int count){
		this.unit = unit;
		this.count = count;
		reset();
	}

	/** Back to a fresh bay: the first launch comes after the start delay. */
	public void reset(){
		timer = firstInterval - startDelay;
		launched = 0;
	}

	@Override
	public Ability copy(){
		RammerBayAbility c = (RammerBayAbility)super.copy();
		c.bound = new Seq<>();
		c.reset();
		return c;
	}

	public int freeSlots(){
		bound.removeAll(u -> !u.isValid());
		return count - bound.size;
	}

	@Override
	public void update(Unit mother){
		if(Vars.net.client() || unit == null || !mother.isValid()) return;
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

	/** Sends one Rammer out of the back of the mothership. */
	public Unit launch(Unit mother){
		Unit u = unit.create(mother.team);
		Tmp.v1.trns(mother.rotation + 180f, mother.hitSize * 0.35f).add(mother.x, mother.y);
		u.set(Tmp.v1.x + Mathf.range(4f), Tmp.v1.y + Mathf.range(4f));
		u.rotation = mother.rotation;
		u.vel.set(mother.vel);
		RammerAI ai = new RammerAI();
		ai.owner = mother;
		ai.bay = this;
		u.controller(ai);
		u.add();
		bound.add(u);
		Fx.spawn.at(u.x, u.y);
		return u;
	}

	/** The bay of a unit, or null. */
	public static RammerBayAbility of(Unit u){
		if(u == null || u.abilities == null) return null;
		for(Ability a : u.abilities) if(a instanceof RammerBayAbility b) return b;
		return null;
	}

	/** Binds a Rammer to the nearest friendly mothership with a free slot; false when there is none. */
	public static boolean adopt(Unit rammer, RammerAI ai, float range){
		RammerBayAbility[] best = {null};
		Unit[] bestUnit = {null};
		Units.nearby(rammer.team, rammer.x - range, rammer.y - range, range * 2f, range * 2f, other -> {
			if(other == rammer || !other.isValid()) return;
			RammerBayAbility b = of(other);
			if(b == null || b.unit != rammer.type || b.freeSlots() <= 0) return;
			if(bestUnit[0] == null || other.dst2(rammer) < bestUnit[0].dst2(rammer)){
				bestUnit[0] = other;
				best[0] = b;
			}
		});
		if(best[0] == null) return false;
		best[0].bound.add(rammer);
		ai.owner = bestUnit[0];
		ai.bay = best[0];
		return true;
	}

	@Override
	public void displayBars(Unit mother, Table bars){
		bars.add(new Bar(() -> Core.bundle.format("bar.nh-rammers", bound.size, count), () -> color, () -> bound.size / (float)Math.max(1, count))).row();
	}

	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.nh-rammerbay.stats", count, unit == null ? "" : unit.localizedName, Strings.fixed(rebuildTime / 60f, 0))).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-rammerbay", "Rammer bay");
	}
}
