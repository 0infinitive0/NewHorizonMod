package newhorizon.expand.units.nuke;

import mindustry.Vars;
import mindustry.entities.units.AIController;
import newhorizon.content.nuke.NukeUnits;

/** The Arm command: the inactive nuke turns itself into its active form on the spot, and then waits for orders. */
public class ArmAI extends AIController{
	@Override
	public void updateMovement(){
		if(!Vars.net.client()) NukeUnits.arm(unit);
	}

	/** No weapons, nothing to aim. */
	@Override
	public void updateTargeting(){
	}
}
