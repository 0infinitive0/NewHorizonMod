package newhorizon.expand.units.siphoner;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.blocks.power.PowerGraph;

/**
 * The E-Siphoner's power drain. Near an enemy block that is on a power network, it channels that network: it takes {@link #startPerSecond} power
 * a second at first, {@link #rampPerSecond} more for every second the channel lasts, and after {@link #infiniteAfter} seconds it takes
 * everything: all that the network's generators make and all that its batteries hold. It takes from the generators' output <b>first</b> (through
 * a hidden producer with a negative output in the graph, see {@link PowerLeech}), before the enemy's own buildings get any, however busy the
 * grid is; what the generators do not make comes out of the batteries. The energy it really takes also becomes the unit's shield
 * ({@code shieldPerPower} for each unit of power), up to {@link #shieldCap} times its maximum health, but the drain does not depend on the
 * shield: with a full shield it drains all the same.
 *
 * <p>It drains one network at a time, and keeps the one it started on while any of its blocks is in range. The channel starts over when it
 * moves to another network or when it has had nothing in range for {@link #loseTicks}. It works for whoever controls the unit.
 */
public class PowerDrainAbility extends Ability{
	/** How far from the unit a block must be to be drained. */
	public float range = 140f;
	/** Power taken a second at the start, how much more a second for every second channeled, and the seconds after which it takes everything. */
	public float startPerSecond = 1000f, rampPerSecond = 1000f, infiniteAfter = 60f;
	/** Shield for each unit of power taken, and the shield's limit as a multiple of the unit's maximum health. */
	public float shieldPerPower = 1f, shieldCap = 10f;
	/** Ticks between looking for a block, and ticks with nothing in reach before the channel ends. */
	public float scanInterval = 10f, loseTicks = 90f;
	public Color beamColor = Color.valueOf("6fe8ff");

	/** The network being drained, a block of it in reach, seconds channeled, and ticks since something was in reach. */
	public PowerGraph graph;
	public Building anchor;
	public float channel, lost, scan;
	/** The hidden producer, what it asked for a tick, and what it took last tick (from the generators and from the batteries) and in all. */
	public PowerLeech.LeechBuild leech;
	public float lastDemand, lastGiven, lastBattery, totalGiven;

	private float beamTimer;

	/** Whether it takes everything now. */
	public boolean unlimited(){
		return graph != null && channel >= infiniteAfter;
	}

	/** The power a second being asked for now (0 when not channeling, infinite once unlimited). */
	public float rate(){
		if(graph == null) return 0f;
		return unlimited() ? Float.POSITIVE_INFINITY : startPerSecond + rampPerSecond * channel;
	}

	@Override
	public void update(Unit unit){
		if(PowerLeech.block == null || !unit.isValid()) return;

		scan -= Time.delta;
		if(scan <= 0f){
			scan = scanInterval;
			Building found = find(unit);
			if(found != null){
				if(graph != found.power.graph) start(unit, found.power.graph);
				anchor = found;
				lost = 0f;
			}else if(graph != null){
				lost += scanInterval;
				if(lost >= loseTicks) release();
			}
		}
		if(graph == null) return;

		channel += Time.delta / 60f;
		step(unit, Time.delta);

		beamTimer -= Time.delta;
		if(beamTimer <= 0f && anchor != null && lost <= 0f && lastDemand > 0f){
			beamTimer = 8f;
			Fx.chainLightning.at(unit.x, unit.y, 0f, beamColor, anchor);
		}
	}

	/**
	 * One step of the channel. It takes what the rate asks for a tick: first off the generators' output (the leech's negative production, read by
	 * the graph when it next updates), then the rest out of the batteries. Nothing while it has no block in reach. Public so the tests can drive it.
	 */
	public void step(Unit unit, float dt){
		if(graph == null || leech == null) return;
		PowerLeech.attach(leech, graph);
		boolean active = lost <= 0f;
		float perTick = active ? rate() / 60f : 0f;
		lastDemand = perTick;

		//The generators first.
		leech.steal = Math.min(perTick, Math.max(0f, leech.othersProduction()));
		float fromProduction = -leech.getPowerProduction() * dt;
		//Then the batteries, for what the generators do not make (all they hold, once it is unlimited).
		float extra = perTick * dt - fromProduction;
		float fromBatteries = 0f;
		if(active && extra > 0f){
			float stored = graph.getBatteryStored();
			if(stored > 0f) fromBatteries = graph.useBatteries(Math.min(extra, stored));
		}
		lastBattery = fromBatteries;
		lastGiven = fromProduction + fromBatteries;
		totalGiven += lastGiven;

		//What it took also becomes shield (secondary: the drain does not depend on it).
		if(lastGiven > 0f){
			float cap = unit.maxHealth * shieldCap;
			unit.shield = Math.max(unit.shield, Math.min(unit.shield + lastGiven * shieldPerPower, cap));
		}
	}

	/** The block to drain: one of the current network's if any is in reach (so that it keeps its channel), else the nearest. */
	public Building find(Unit unit){
		Building[] best = {null}, same = {null};
		float[] bestD = {Float.MAX_VALUE}, sameD = {Float.MAX_VALUE};
		Units.nearbyBuildings(unit.x, unit.y, range, b -> {
			if(b.team == unit.team || b.team == Team.derelict || !b.isValid() || b.power == null || b.power.graph == null) return;
			float d = b.dst2(unit);
			if(d < bestD[0]){
				bestD[0] = d;
				best[0] = b;
			}
			if(b.power.graph == graph && d < sameD[0]){
				sameD[0] = d;
				same[0] = b;
			}
		});
		return same[0] != null ? same[0] : best[0];
	}

	private void start(Unit unit, PowerGraph g){
		if(leech != null) PowerLeech.detach(leech);
		graph = g;
		channel = 0f;
		lost = 0f;
		if(leech == null) leech = PowerLeech.create(unit);
		leech.owner = unit;
	}

	/** Ends the channel: the rate starts over next time. */
	public void release(){
		if(leech != null) PowerLeech.detach(leech);
		graph = null;
		anchor = null;
		channel = 0f;
		lost = 0f;
		lastDemand = 0f;
	}

	@Override
	public void death(Unit unit){
		release();
	}

	@Override
	public void displayBars(Unit unit, Table bars){
		bars.add(new Bar(() -> unlimited() ? Core.bundle.get("bar.nh-drain-unlimited") : Core.bundle.format("bar.nh-drain", Strings.fixed(rate(), 0)), () -> Pal.accent,
			() -> graph == null ? 0f : Math.min(1f, channel / infiniteAfter))).row();
		bars.add(new Bar(() -> Core.bundle.format("bar.nh-drain-shield", (int)unit.shield, (int)(unit.maxHealth * shieldCap)), () -> beamColor,
			() -> Math.min(1f, unit.shield / Math.max(1f, unit.maxHealth * shieldCap)))).row();
	}

	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.nh-powerdrain.stats", (int)startPerSecond, (int)rampPerSecond, (int)infiniteAfter, (int)(range / 8f), (int)shieldCap)).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-powerdrain", "Power drain");
	}
}
