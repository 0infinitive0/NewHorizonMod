package newhorizon.expand.units.ai;

import arc.struct.IntFloatMap;
import arc.util.Time;
import mindustry.entities.units.AIController;
import mindustry.gen.Unit;
import newhorizon.expand.units.mimic.Mimicry;

/**
 * Goes to the nearest unit worth copying, friendly or enemy, and becomes one of its kind. Where you leave it decides what it
 * copies: it takes whatever is closest. A target it cannot become where it stands (a boat on dry land, a walker over a wall) is
 * set aside for a while and the next one tried; with nothing to copy it stays where it is.
 */
public class MimicAI extends AIController{
	/** How far it looks for something to copy. */
	public float seekRange = 360f;
	/** Ticks a target it cannot become is left alone. */
	public float skipTime = 60f * 6f;

	private Unit prey;
	private float searchTimer = 0f;
	private final IntFloatMap skipped = new IntFloatMap();

	@Override
	public void updateMovement(){
		if(prey != null && (Mimicry.blocker(unit, prey, true) != null || !unit.within(prey, seekRange + prey.hitSize / 2f))){
			prey = null;
		}

		searchTimer -= Time.delta;
		if(prey == null || searchTimer <= 0f){
			searchTimer = 30f;
			prey = findPrey();
		}
		if(prey == null) return;

		unit.lookAt(prey);
		float range = Mimicry.range(unit);
		moveTo(prey, Math.max(prey.hitSize / 2f + unit.hitSize / 2f, range * 0.5f));

		if(unit.within(prey, range + prey.hitSize / 2f)){
			String why = Mimicry.blockerHere(unit, prey, false);
			if(why == null){
				Mimicry.transform(unit, prey);
			}else{
				//Terrain or cap: try something else for a while.
				skipped.put(prey.id, Time.time + skipTime);
				prey = null;
			}
		}
	}

	protected Unit findPrey(){
		for(Unit u : Mimicry.candidates(unit, seekRange, true)){
			if(skipped.get(u.id, 0f) > Time.time) continue;
			return u;
		}
		return null;
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
