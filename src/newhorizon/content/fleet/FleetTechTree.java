package newhorizon.content.fleet;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.content.UnitTypes;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives;

/**
 * The Mothership is researched under the Horizon in the Serpulo tree (it is made in the same Air Factory). The Fleet Reconstructor hangs under the
 * Multiplicative Reconstructor (needs the Mothership), Mothership II and III under the Mothership (each needs the Fleet Reconstructor), and the big
 * payload conveyors in a chain under the Payload Conveyor. The drones come with the Mothership.
 */
public class FleetTechTree{
	public static void loadOtherPlanets(){
		if(FleetUnits.mothership == null) return;
		TechNode horizon = TechTree.all.find(n -> n.content == UnitTypes.horizon);
		if(horizon == null) return;
		TechNode ship = new TechNode(horizon, FleetUnits.mothership, FleetUnits.mothership.researchRequirements());
		ship.objectives.add(new Objectives.Research(Blocks.airFactory));

		TechNode multi = find(Blocks.multiplicativeReconstructor);
		TechNode reconstructor = new TechNode(multi != null ? multi : ship, FleetBlocks.fleetReconstructor, FleetBlocks.fleetReconstructor.researchRequirements());
		reconstructor.objectives.add(new Objectives.Research(FleetUnits.mothership));

		TechNode two = new TechNode(ship, FleetUnits.mothershipII, FleetUnits.mothershipII.researchRequirements());
		two.objectives.add(new Objectives.Research(FleetBlocks.fleetReconstructor));
		TechNode three = new TechNode(two, FleetUnits.mothershipIII, FleetUnits.mothershipIII.researchRequirements());
		three.objectives.add(new Objectives.Research(FleetUnits.mothershipII));

		//the payload conveyors that carry them
		TechNode payload = find(Blocks.payloadConveyor);
		TechNode large = new TechNode(payload != null ? payload : reconstructor, FleetBlocks.largePayloadConveyor, FleetBlocks.largePayloadConveyor.researchRequirements());
		TechNode huge = new TechNode(large, FleetBlocks.hugePayloadConveyor, FleetBlocks.hugePayloadConveyor.researchRequirements());
		huge.objectives.add(new Objectives.Research(FleetUnits.mothership));
		TechNode giant = new TechNode(huge, FleetBlocks.giantPayloadConveyor, FleetBlocks.giantPayloadConveyor.researchRequirements());
		new TechNode(giant, FleetBlocks.colossalPayloadConveyor, FleetBlocks.colossalPayloadConveyor.researchRequirements());
	}

	private static TechNode find(UnlockableContent content){
		return TechTree.all.find(n -> n.content == content);
	}
}
