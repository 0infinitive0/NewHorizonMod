package newhorizon.expand.units.serpent;

import mindustry.content.StatusEffects;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;

/**
 * On the head: while the Serpent has sections the head cannot take damage (it is kept invincible a few ticks at a time), and a second
 * head of the same team, which the Nest does not make but something else might, is removed.
 */
public class SerpentGuardAbility extends Ability{
	@Override
	public void update(Unit unit){
		if(unit.dead) return;
		if(SerpentChain.head(unit.team) != unit){
			if(!mindustry.Vars.net.client()) unit.kill();
			return;
		}
		if(SerpentChain.sections(unit.team) > 0) unit.apply(StatusEffects.invincible, 8f);
	}

	@Override
	public String localized(){
		return arc.Core.bundle.get("ability.nh-serpent-guard");
	}
}
