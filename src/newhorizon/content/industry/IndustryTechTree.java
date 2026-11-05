package newhorizon.content.industry;

import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.Planets;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;
import newhorizon.content.NHBlocks;

import static mindustry.content.TechTree.node;

/**
 * The industry branch hangs on its own from the Serpulo, Erekir and New Horizon trees, behind each tree's silicon block, like the nukes.
 * Its root is the Ultimate Silicon Factory.
 */
public class IndustryTechTree{
	private static final ItemStack[] cost = ItemStack.with(Items.silicon, 800, Items.graphite, 600, Items.thorium, 400);

	/** Inside the New Horizon tree. */
	public static void load(){
		node(IndustryBlocks.ultimateSiliconFactory, cost, Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), () -> fill(TechTree.context()));
	}

	/** The vanilla trees of the other planets. */
	public static void loadOtherPlanets(){
		attach(Planets.serpulo == null ? null : Planets.serpulo.techTree, Blocks.siliconSmelter);
		attach(Planets.erekir == null ? null : Planets.erekir.techTree, Blocks.siliconArcFurnace);
	}

	private static void attach(TechNode root, UnlockableContent gate){
		if(root == null || gate == null) return;
		TechNode factory = new TechNode(root, IndustryBlocks.ultimateSiliconFactory, cost);
		factory.objectives.add(new Objectives.Research(gate));
		fill(factory);
	}

	private static void fill(TechNode factory){
		TechNode pump = child(factory, IndustryBlocks.turbinePump);
		TechNode quantum = child(pump, IndustryBlocks.quantumPump);
		child(quantum, IndustryBlocks.singularityPump);

		TechNode container = child(factory, IndustryBlocks.armoredContainer);
		TechNode tank = child(container, IndustryBlocks.armoredTank);
		child(tank, IndustryBlocks.singularityTank);

		TechNode bank = child(factory, IndustryBlocks.capacitorBank);
		TechNode surge = child(bank, IndustryBlocks.surgeBattery);
		child(surge, IndustryBlocks.singularityBattery);
	}

	private static TechNode child(TechNode parent, UnlockableContent content){
		return new TechNode(parent, content, content.researchRequirements());
	}
}
