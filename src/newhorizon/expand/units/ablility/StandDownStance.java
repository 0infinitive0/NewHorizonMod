package newhorizon.expand.units.ablility;

import arc.Core;
import mindustry.ai.UnitStance;

/** The Stand down button of a Mothership: while it is on, the drones dock beside the Mothership and do not attack. See {@link DroneBayAbility#standDown}. */
public class StandDownStance extends UnitStance{
	public StandDownStance(){
		super("nh-stand-down", "cancel", null);
	}

	@Override
	public String localized(){
		return Core.bundle.get("stance.nh-stand-down", "Stand down");
	}
}
