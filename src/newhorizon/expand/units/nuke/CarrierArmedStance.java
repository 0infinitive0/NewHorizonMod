package newhorizon.expand.units.nuke;

import arc.Core;
import mindustry.ai.UnitStance;

/** The Armed button of a carrier ship: while it is on, the nuke aboard is armed (and disarmed again when it is switched off). */
public class CarrierArmedStance extends UnitStance{
	public CarrierArmedStance(){
		super("nh-carrier-armed", "power", null);
	}

	@Override
	public String localized(){
		return Core.bundle.get("stance.nh-carrier-armed", "Armed");
	}
}
