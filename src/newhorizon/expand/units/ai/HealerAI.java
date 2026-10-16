package newhorizon.expand.units.ai;

import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Unit;

/**
 * Flies to the allied unit that needs healing most and hovers next to it, so the healer's own repair aura (an ability, not a
 * weapon) reaches it. With nobody to heal it waits next to the core.
 */
public class HealerAI extends AIController{
	/** How far away it notices injured allies. */
	public float seekRange = 1000f;
	/** How close it hovers to its patient. */
	public float hoverDst = 26f;

	protected Unit patient;

	@Override
	public void updateMovement(){
		if(retarget()){
			patient = findPatient();
		}
		if(patient != null && (patient.dead() || !patient.damaged() || !patient.isAdded())){
			patient = findPatient();
		}

		if(patient != null){
			moveTo(patient, hoverDst);
			unit.lookAt(patient);
			return;
		}

		Building core = unit.closestCore();
		if(core != null) moveTo(core, 60f + core.block.size * 4f);
	}

	/** The most hurt ally nearby, with distance as a tie-breaker. Allies at full health are not patients. */
	public Unit findPatient(){
		Unit[] best = {null};
		float[] bestScore = {-Float.MAX_VALUE};
		Units.nearby(unit.team, unit.x - seekRange, unit.y - seekRange, seekRange * 2f, seekRange * 2f, other -> {
			if(other == unit || other.dead() || !other.damaged()) return;
			float score = (1f - other.healthf()) * 3f - unit.dst(other) / seekRange;
			if(score > bestScore[0]){
				bestScore[0] = score;
				best[0] = other;
			}
		});
		return best[0];
	}

	public Unit patient(){
		return patient;
	}
}
