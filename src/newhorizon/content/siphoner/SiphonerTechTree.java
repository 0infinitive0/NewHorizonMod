package newhorizon.content.siphoner;

import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.content.UnitTypes;

/** The E-Siphoner is researched under the Horizon in the Serpulo tree (it is made in the same Air Factory). */
public class SiphonerTechTree{
	public static void loadOtherPlanets(){
		if(SiphonerUnits.eSiphoner == null) return;
		TechNode horizon = TechTree.all.find(n -> n.content == UnitTypes.horizon);
		if(horizon == null) return;
		new TechNode(horizon, SiphonerUnits.eSiphoner, SiphonerUnits.eSiphoner.researchRequirements());
	}
}
