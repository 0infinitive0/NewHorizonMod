package newhorizon.content.nuke;

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
 * The nukes belong to no faction: their branch hangs on its own from the Serpulo, Erekir and New Horizon trees, behind each tree's
 * silicon block, like the other things that work on every planet.
 */
public class NukeTechTree{
	private static final ItemStack[] cost = ItemStack.with(Items.silicon, 700, Items.graphite, 500, Items.thorium, 400);

	/** Inside the New Horizon tree. */
	public static void load(){
		node(NukeBlocks.nukeBuilder, cost, Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), () -> fill(TechTree.context()));
	}

	/** The vanilla trees of the other planets. */
	public static void loadOtherPlanets(){
		attach(Planets.serpulo == null ? null : Planets.serpulo.techTree, Blocks.siliconSmelter);
		attach(Planets.erekir == null ? null : Planets.erekir.techTree, Blocks.siliconArcFurnace);
	}

	private static void attach(TechNode root, UnlockableContent gate){
		if(root == null || gate == null) return;
		TechNode builder = new TechNode(root, NukeBlocks.nukeBuilder, cost);
		builder.objectives.add(new Objectives.Research(gate));
		fill(builder);
	}

	private static void fill(TechNode builder){
		child(builder, NukeBlocks.nukeLauncher);
		for(UnlockableContent unit : NukeUnits.inactive) child(builder, unit);
	}

	private static TechNode child(TechNode parent, UnlockableContent content){
		return new TechNode(parent, content, content.researchRequirements());
	}
}
