package newhorizon.expand.units.ai;

import arc.struct.IntFloatMap;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Unit;
import newhorizon.expand.units.scavenger.Scavenging;

/**
 * The Scavenger's Sacrifice command: goes to the nearest friendly unit that has a weapon or ability to give and consumes it, taking
 * one random weapon and one random ability of its kind. Where you leave the Scavenger decides who is eaten. With nobody suitable in
 * reach it stays where it is, and its own gun still fires at whatever comes near.
 */
public class SacrificeAI extends AIController{
	/** How far it looks for something to consume. */
	public float seekRange = 360f;
	/** Ticks a unit that gave nothing is left alone. */
	public float skipTime = 60f * 10f;

	private Unit prey;
	private float searchTimer = 0f;
	private final IntFloatMap skipped = new IntFloatMap();

	@Override
	public void updateMovement(){
		if(prey != null && (!Scavenging.canSacrifice(unit, prey) || !unit.within(prey, seekRange + prey.hitSize / 2f))){
			prey = null;
		}

		searchTimer -= Time.delta;
		if(prey == null || searchTimer <= 0f){
			searchTimer = 30f;
			prey = findPrey();
		}
		if(prey == null) return;

		float reach = unit.hitSize / 2f + prey.hitSize / 2f + 10f;
		moveTo(prey, Math.max(prey.hitSize / 2f + unit.hitSize / 2f, reach * 0.5f));

		if(unit.within(prey, reach) && !Scavenging.sacrifice(unit, prey)){
			//It had nothing the Scavenger could use: try someone else for a while.
			skipped.put(prey.id, Time.time + skipTime);
			prey = null;
		}
	}

	protected Unit findPrey(){
		Unit[] best = {null};
		float[] bestDst = {Float.MAX_VALUE};
		Units.nearby(unit.x - seekRange, unit.y - seekRange, seekRange * 2f, seekRange * 2f, u -> {
			if(skipped.get(u.id, 0f) > Time.time || !Scavenging.canSacrifice(unit, u)) return;
			float d = u.dst2(unit);
			if(d < bestDst[0]){
				best[0] = u;
				bestDst[0] = d;
			}
		});
		return best[0];
	}
}
