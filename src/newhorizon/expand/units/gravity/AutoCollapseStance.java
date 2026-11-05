package newhorizon.expand.units.gravity;

import arc.Core;
import mindustry.ai.UnitStance;

/** The Auto collapse button of the Gravity Base: while it is on, a base under orders drops its black hole by itself (see {@link CollapseAbility}). */
public class AutoCollapseStance extends UnitStance{
	public AutoCollapseStance(){
		super("nh-auto-collapse", "warning", null);
	}

	@Override
	public String localized(){
		return Core.bundle.get("stance.nh-auto-collapse", "Auto collapse");
	}
}
