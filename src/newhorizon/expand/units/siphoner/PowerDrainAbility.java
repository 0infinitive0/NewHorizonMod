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
 * The E-Siphoner's power drain. Near an enemy block that is on a power network, it channels that network: it asks the graph for
 * {@code rate} of the network's total energy input (what its generators make), where {@code rate} starts at {@link #startRate} and goes up
 * {@link #rampPerSecond} every second the channel lasts, to {@link #maxRate} (200%). It asks through a hidden consumer in the graph
 * ({@link PowerLeech}), so the graph shares out what it has, batteries included, and only the energy the unit is <b>actually given</b> counts:
 * that becomes the unit's shield ({@code shieldPerPower} for each unit of power), up to {@link #shieldCap} times its maximum health.
 *
 * <p>It drains one network at a time, and keeps the one it started on while any of its blocks is in range. The channel starts over when it
 * moves to another network or when it has had nothing in range for {@link #loseTicks}. It works for whoever controls the unit.
 */
public class PowerDrainAbility extends Ability{
	/** How far from the unit a block must be to be drained. */
	public float range = 140f;
	/** Share of the network's input asked for at the start, how much that goes up each second, and the most. */
	public float startRate = 0.01f, rampPerSecond = 0.01f, maxRate = 2f;
	/** Shield for each unit of power taken, and the shield's limit as a multiple of the unit's maximum health. */
	public float shieldPerPower = 1f, shieldCap = 10f;
	/** Ticks between looking for a block, and ticks with nothing in reach before the channel ends. */
	public float scanInterval = 10f, loseTicks = 90f;
	public Color beamColor = Color.valueOf("6fe8ff");

	/** The network being drained, a block of it in reach, seconds channeled, and ticks since something was in reach. */
	public PowerGraph graph;
	public Building anchor;
	public float channel, lost, scan;
	/** The hidden consumer, what was asked for and what was given last tick. */
	public PowerLeech.LeechBuild leech;
	public float lastDemand, lastGiven, totalGiven;

	private float beamTimer;

	/** The share of the network's input being asked for now (0 when not channeling). */
	public float rate(){
		return graph == null ? 0f : Math.min(startRate + rampPerSecond * channel, maxRate);
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
	 * One step of the channel: asks the graph for the current share of its input, and turns what it was given since the last step into shield.
	 * Public so the tests can drive it.
	 */
	public void step(Unit unit, float dt){
		if(graph == null || leech == null) return;
		//What the last graph update gave it, for what it asked for then.
		lastGiven = leech.given() * dt;
		totalGiven += lastGiven;
		if(lastGiven > 0f){
			float cap = unit.maxHealth * shieldCap;
			unit.shield = Math.max(unit.shield, Math.min(unit.shield + lastGiven * shieldPerPower, cap));
		}
		//What it asks for now: the share of the network's input (per tick). Nothing while it has no block in reach.
		float input = graph.getPowerProduced() / Math.max(Time.delta, 0.0001f);
		leech.demand = lost > 0f ? 0f : rate() * input;
		lastDemand = leech.demand;
		//The graph only counts consumers that say they want power (the buildings of the world do it in their consumption update).
		leech.shouldConsumePower = leech.demand > 0f;
		PowerLeech.attach(leech, graph);
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
		bars.add(new Bar(() -> Core.bundle.format("bar.nh-drain", Strings.fixed(rate() * 100f, 0)), () -> Pal.accent, () -> Math.min(1f, rate() / maxRate))).row();
		bars.add(new Bar(() -> Core.bundle.format("bar.nh-drain-shield", (int)unit.shield, (int)(unit.maxHealth * shieldCap)), () -> beamColor,
			() -> Math.min(1f, unit.shield / Math.max(1f, unit.maxHealth * shieldCap)))).row();
	}

	@Override
	public void addStats(Table t){
		super.addStats(t);
		t.add(Core.bundle.format("ability.nh-powerdrain.stats", (int)(startRate * 100f), (int)(rampPerSecond * 100f), (int)(maxRate * 100f), (int)(range / 8f), (int)shieldCap)).wrap().width(descriptionWidth).row();
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-powerdrain", "Power drain");
	}
}
