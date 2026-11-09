package newhorizon.content.supply;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.content.UnitTypes;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives;

/** The supply blocks hang under the matching base game blocks in the Serpulo tree. */
public class SupplyTechTree{
	public static void loadOtherPlanets(){
		if(SupplyBlocks.conveyors[0] == null) return;
		belts();
		drills();
		child(find(Blocks.solarPanel), SupplyBlocks.windTurbine);
		walkers();
		towers();
	}

	private static TechNode find(UnlockableContent content){
		return TechTree.all.find(n -> n.content == content);
	}

	/** A child of {@code parent} (a node) for {@code content}; null if the parent is missing. */
	static TechNode child(TechNode parent, UnlockableContent content){
		if(parent == null) return null;
		return new TechNode(parent, content, content.researchRequirements());
	}

	private static void belts(){
		TechNode conveyor = find(Blocks.conveyor), conduit = find(Blocks.conduit), bridge = find(Blocks.itemBridge), pipeBridge = find(Blocks.bridgeConduit);
		TechNode c = conveyor, p = conduit, b = bridge, pb = pipeBridge;
		for(int i = 0; i < 4; i++){
			c = child(c, SupplyBlocks.conveyors[i]);
			p = child(p, SupplyBlocks.pipes[i]);
			b = child(b, SupplyBlocks.bridges[i]);
			pb = child(pb, SupplyBlocks.pipeBridges[i]);
			//The top tier asks for the base game's titanium conveyor first.
			if(i == 3 && c != null) c.objectives.add(new Objectives.Research(Blocks.titaniumConveyor));
		}
	}

	private static void drills(){
		TechNode mech = find(Blocks.mechanicalDrill), pneu = find(Blocks.pneumaticDrill), laser = find(Blocks.laserDrill), blast = find(Blocks.blastDrill);
		child(mech, SupplyBlocks.rotaryDrill);
		TechNode auger = child(pneu, SupplyBlocks.augerDrill);
		if(auger != null) auger.objectives.add(new Objectives.Research(SupplyBlocks.rotaryDrill));
		TechNode heavy = child(laser, SupplyBlocks.heavyLaserDrill);
		TechNode colossus = child(blast, SupplyBlocks.colossusDrill);
		if(colossus != null) colossus.objectives.add(new Objectives.Research(SupplyBlocks.heavyLaserDrill));
	}

	/** The walkers hang next to the crawler (under the crawler's parent), each tier under the last, once the Ground Factory is researched. */
	private static void walkers(){
		if(SupplyUnits.digger == null) return;
		TechNode crawler = find(UnitTypes.crawler);
		if(crawler == null || crawler.parent == null) return;
		TechNode node = child(crawler.parent, SupplyUnits.digger);
		node.objectives.add(new Objectives.Research(Blocks.groundFactory));
		node = child(node, SupplyUnits.delver);
		node = child(node, SupplyUnits.burrower);
		child(node, SupplyUnits.excavator);
	}

	/** Tower I under the Mender Projector; each next tier under the last, asking for a bigger projector first. */
	private static void towers(){
		TechNode one = child(find(Blocks.mendProjector), SupplyBlocks.statusTower1);
		TechNode two = child(one, SupplyBlocks.statusTower2);
		if(two != null) two.objectives.add(new Objectives.Research(Blocks.overdriveProjector));
		TechNode three = child(two, SupplyBlocks.statusTower3);
		if(three != null) three.objectives.add(new Objectives.Research(Blocks.forceProjector));
	}
}
