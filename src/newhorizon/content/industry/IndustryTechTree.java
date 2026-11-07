package newhorizon.content.industry;

import arc.Core;
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
import newhorizon.content.jade.JadeBlocks;

import static mindustry.content.TechTree.node;

/**
 * The industry branch hangs on its own from the Serpulo, Erekir and New Horizon trees, behind each tree's silicon block, like the nukes.
 * Its root is the Ultimate Silicon Factory.
 */
public class IndustryTechTree{
	private static final ItemStack[] cost = ItemStack.with(Items.silicon, 800, Items.graphite, 600, Items.thorium, 400);

	/** Inside the New Horizon tree. */
	public static void load(){
		node(IndustryBlocks.ultimateSiliconFactory, cost, Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), () -> fill(TechTree.context(), null, false));
	}

	/** The vanilla trees of the other planets. */
	public static void loadOtherPlanets(){
		//On Serpulo the normal Liquid Mass Driver goes under the base game's Mass Driver, where one looks for it.
		attach(Planets.serpulo == null ? null : Planets.serpulo.techTree, Blocks.siliconSmelter, Blocks.massDriver);
		attach(Planets.erekir == null ? null : Planets.erekir.techTree, Blocks.siliconArcFurnace, null);
	}

	private static void attach(TechNode root, UnlockableContent gate, UnlockableContent driverParent){
		if(root == null || gate == null) return;
		TechNode factory = new TechNode(root, IndustryBlocks.ultimateSiliconFactory, cost);
		factory.objectives.add(new Objectives.Research(gate));
		TechNode parent = driverParent == null ? null : TechTree.all.find(n -> n.content == driverParent);
		fill(factory, parent, true);
	}

	/**
	 * @param liquidParent where the normal Liquid Mass Driver goes (null: under the factory)
	 * @param addLiquidDriver false inside the New Horizon tree, where the Jade branch already has that block's node (a block has one node per tree)
	 */
	private static void fill(TechNode factory, TechNode liquidParent, boolean addLiquidDriver){
		TechNode pump = child(factory, IndustryBlocks.turbinePump);
		TechNode quantum = child(pump, IndustryBlocks.quantumPump);
		child(quantum, IndustryBlocks.singularityPump);

		TechNode container = child(factory, IndustryBlocks.armoredContainer);
		TechNode tank = child(container, IndustryBlocks.armoredTank);
		child(tank, IndustryBlocks.singularityTank);

		//The normal Liquid Mass Driver (a Jade block with a universal cost), then the advanced one, which needs it.
		TechNode driver = child(factory, IndustryBlocks.advancedMassDriver);
		TechNode advancedLiquid = child(driver, IndustryBlocks.advancedLiquidMassDriver);
		if(JadeBlocks.liquidMassDriver != null){
			if(addLiquidDriver) child(liquidParent == null ? factory : liquidParent, JadeBlocks.liquidMassDriver);
			advancedLiquid.objectives.add(new Objectives.Research(JadeBlocks.liquidMassDriver));
		}

		TechNode teleporter = child(factory, IndustryBlocks.itemTeleporter);
		child(teleporter, IndustryBlocks.liquidTeleporter);

		TechNode separator = child(factory, IndustryBlocks.electricSeparator);
		child(separator, IndustryBlocks.fusionReactor);

		TechNode bank = child(factory, IndustryBlocks.capacitorBank);
		TechNode surge = child(bank, IndustryBlocks.surgeBattery);
		child(surge, IndustryBlocks.singularityBattery);
	}

	private static TechNode child(TechNode parent, UnlockableContent content){
		return new TechNode(parent, content, content.researchRequirements());
	}

	/** Settings key of the one-time re-lock below. */
	public static final String relockKey = "nh-industry-relock-1";

	/**
	 * Unlocks are kept in the player's settings, and a block that was unlocked while this branch was being built (or with the Debug panel's
	 * unlock buttons) stays unlocked in every campaign after that. The Electric Separator and the Fusion Reactor are locked again, once, so
	 * that they have to be researched like the rest of the branch.
	 */
	public static void relockOnce(){
		if(Core.settings == null || Core.settings.getBool(relockKey, false)) return;
		Core.settings.put(relockKey, true);
		if(IndustryBlocks.electricSeparator != null) IndustryBlocks.electricSeparator.clearUnlock();
		if(IndustryBlocks.fusionReactor != null) IndustryBlocks.fusionReactor.clearUnlock();
	}
}
