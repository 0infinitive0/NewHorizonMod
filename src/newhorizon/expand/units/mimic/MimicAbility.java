package newhorizon.expand.units.mimic;

import arc.Core;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * Marks a unit as a Mimic and lets a pilot trigger the transformation: while a player flies it and holds fire, it becomes a copy of
 * the unit nearest to where they aim, within {@link #range}. AI-controlled mimics use {@link newhorizon.expand.units.ai.MimicAI}.
 */
public class MimicAbility extends Ability{
	/** How close a unit has to be to be copied. */
	public float range = 90f;

	@Override
	public void update(Unit unit){
		if(Vars.net.client() || !unit.isPlayer() || !unit.isShooting) return;

		Unit target = Mimicry.nearestTo(unit, unit.aimX, unit.aimY);
		if(target != null) Mimicry.transform(unit, target);
	}

	@Override
	public String localized(){
		return Core.bundle.get("ability.nh-mimic", "Mimic");
	}
}
