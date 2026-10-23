package newhorizon.expand.units.serpent;

import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/** Heals the unit itself, a share of its health every second. */
public class PassiveHealAbility extends Ability{
	/** The share of its maximum health it gets back each second. */
	public float percent = 0.015f;
	/** How much it healed in its last update, so that what the unit gained can be told from what it lost. */
	public float lastHeal;

	public PassiveHealAbility(float percent){
		this.percent = percent;
	}

	@Override
	public void update(Unit unit){
		lastHeal = 0f;
		if(newhorizon.expand.status.NoHeal.blocked(unit)) return;
		if(unit.health < unit.maxHealth && !unit.dead){
			float before = unit.health;
			unit.heal(unit.maxHealth * percent * Time.delta / 60f);
			lastHeal = unit.health - before;
		}
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-passive-heal");
	}
}
