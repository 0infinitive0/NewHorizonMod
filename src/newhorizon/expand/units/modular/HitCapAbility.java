package newhorizon.expand.units.modular;

import arc.Core;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * A cap on any single hit: whatever hits the unit, it takes at most {@link #cap} health from one hit (after armor), so that a sniper's one
 * huge shot, a Foreshadow's, does no more than a small one. Many small hits are not touched. The cap is enforced where the unit takes
 * damage, in {@code ChimeraEntity.rawDamage}; this ability only holds the number.
 */
public class HitCapAbility extends Ability{
	public float cap = 200f;

	/** The cap on a unit's hits, or 0 if it has none. */
	public static float capOf(Unit unit){
		float cap = 0f;
		for(Ability a : unit.abilities){
			if(a instanceof HitCapAbility){
				float c = ((HitCapAbility)a).cap;
				if(c > 0f && (cap <= 0f || c < cap)) cap = c;
			}
		}
		return cap;
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-hit-cap", "Hit cap");
	}
}
