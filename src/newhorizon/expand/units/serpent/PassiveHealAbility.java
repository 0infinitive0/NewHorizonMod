package newhorizon.expand.units.serpent;

import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/** Heals the unit itself, a share of its health every second. */
public class PassiveHealAbility extends Ability{
	/** The share of its maximum health it gets back each second. */
	public float percent = 0.015f;

	public PassiveHealAbility(float percent){
		this.percent = percent;
	}

	@Override
	public void update(Unit unit){
		if(unit.health < unit.maxHealth && !unit.dead) unit.heal(unit.maxHealth * percent * Time.delta / 60f);
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-passive-heal");
	}
}
