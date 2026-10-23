package newhorizon.expand.units.chronos;

import arc.Core;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.power.PowerGraph;
import newhorizon.content.chronos.ChronosColor;
import newhorizon.expand.block.chronos.TemporalDisable;

/**
 * Feeds on enemy power grids within {@link #range}: it empties their batteries a pulse at a time, turns the energy into repairs for
 * itself and knocks the generators it is next to offline for a few seconds, so the whole grid runs short.
 */
public class GridDrainAbility extends Ability{
	public float range = 150f;
	/** Ticks between two pulses. */
	public float interval = 15f;
	/** Power units taken from each grid per pulse. */
	public float drain = 80f;
	/** Health the unit gets back per power unit taken. */
	public float healPerPower = 0.12f;
	/** How long a generator in reach stays down. */
	public float disableTime = 60f * 3f;

	public float timer;

	@Override
	public void update(Unit unit){
		timer += Time.delta;
		if(timer < interval) return;
		timer = 0f;

		Seq<Building> targets = new Seq<>();
		Units.nearbyBuildings(unit.x, unit.y, range, b -> {
			if(b.team != unit.team && b.team != mindustry.game.Team.derelict && b.power != null) targets.add(b);
		});
		pulse(unit, targets);
	}

	/** One pulse against the given buildings. Returns the power taken. */
	public float pulse(Unit unit, Seq<Building> targets){
		Seq<PowerGraph> graphs = new Seq<>();
		float taken = 0f;
		for(Building b : targets){
			if(b.power.graph != null && !graphs.contains(b.power.graph)){
				graphs.add(b.power.graph);
				float got = b.power.graph.useBatteries(Math.min(drain, b.power.graph.getBatteryStored()));
				if(got > 0f){
					taken += got;
					Fx.chainLightning.at(unit.x, unit.y, 0f, ChronosColor.chrono, b);
				}
			}
			if(b.block instanceof PowerGenerator && TemporalDisable.disable(b, disableTime)){
				Fx.chainLightning.at(unit.x, unit.y, 0f, ChronosColor.violet, b);
			}
		}
		if(taken > 0f) unit.heal(taken * healPerPower);
		return taken;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-griddrain", "Grid drain");
	}
}
