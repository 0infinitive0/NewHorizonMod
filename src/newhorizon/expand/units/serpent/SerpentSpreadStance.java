package newhorizon.expand.units.serpent;

import arc.Core;
import mindustry.ai.UnitStance;

/** The Spread damage button of a Serpent's head: while it is on, a hit on any body section is shared by all of them. */
public class SerpentSpreadStance extends UnitStance{
	public SerpentSpreadStance(){
		super("nh-serpent-spread", "defense", null);
	}

	@Override
	public String localized(){
		return Core.bundle.get("stance.nh-serpent-spread", "Spread damage");
	}
}
