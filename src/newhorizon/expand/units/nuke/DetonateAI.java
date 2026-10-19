package newhorizon.expand.units.nuke;

import mindustry.entities.units.AIController;

/** The Detonate command: an active nuke that is given it goes off at once. */
public class DetonateAI extends AIController{
	@Override
	public void updateMovement(){
		NukeAbility ability = NukeAI.abilityOf(unit);
		if(ability != null) ability.detonate(unit);
	}

	@Override
	public void updateTargeting(){
	}
}
