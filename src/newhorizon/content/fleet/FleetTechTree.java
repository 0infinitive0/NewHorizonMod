package newhorizon.content.fleet;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.content.UnitTypes;
import mindustry.game.Objectives;

/** The Mothership is researched under the Horizon in the Serpulo tree (it is made in the same Air Factory). The Rammers come with it. */
public class FleetTechTree{
	public static void loadOtherPlanets(){
		if(FleetUnits.mothership == null) return;
		TechNode horizon = TechTree.all.find(n -> n.content == UnitTypes.horizon);
		if(horizon == null) return;
		TechNode node = new TechNode(horizon, FleetUnits.mothership, FleetUnits.mothership.researchRequirements());
		node.objectives.add(new Objectives.Research(Blocks.airFactory));
	}
}
