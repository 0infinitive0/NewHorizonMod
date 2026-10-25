package newhorizon.content.chronos;

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
 * Chronos is researched on every planet: its branch hangs from the Serpulo tree, the Erekir tree and the New Horizon tree alike, each
 * time behind that planet's own silicon block, since silicon is what everything here is made from. The same blocks and units are in all
 * of them, so researching one anywhere unlocks it everywhere.
 */
public class ChronosTechTree{
	/** Attaches the whole branch under the New Horizon root. Must be called inside it. */
	public static void load(){
		node(ChronosBlocks.temporalFoundry, ItemStack.with(Items.silicon, 400, Items.graphite, 300), Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), ChronosTechTree::branch);
	}

	/** Attaches the branch to the vanilla trees of the other planets. Safe to call when a tree does not exist. */
	public static void loadOtherPlanets(){
		attach(Planets.serpulo == null ? null : Planets.serpulo.techTree, Blocks.siliconSmelter);
		attach(Planets.erekir == null ? null : Planets.erekir.techTree, Blocks.siliconArcFurnace);
	}

	private static void attach(TechNode root, UnlockableContent gate){
		if(root == null || gate == null) return;

		TechNode foundry = new TechNode(root, ChronosBlocks.temporalFoundry, ItemStack.with(Items.silicon, 400, Items.graphite, 300));
		foundry.objectives.add(new Objectives.Research(gate));
		//The tree is built by the node() helper, which hangs new nodes under whatever it was last given.
		fill(foundry);
	}

	private static void fill(TechNode foundry){
		TechNode paradox = child(foundry, ChronosBlocks.paradoxForge);
		child(paradox, ChronosBlocks.chronoAccelerator);
		child(paradox, ChronosBlocks.eventHorizon);

		TechNode wall = child(foundry, ChronosBlocks.chronoWall);
		child(wall, ChronosBlocks.chronoWallLarge);
		TechNode stasis = child(wall, ChronosBlocks.stasisTower);
		child(stasis, ChronosBlocks.dilationField);

		TechNode arc = child(foundry, ChronosBlocks.arcFreezer);
		child(arc, ChronosBlocks.dissonanceArray);

		TechNode relay = child(foundry, ChronosBlocks.chronoRelay);
		child(relay, ChronosBlocks.warpPad);

		TechNode fab = child(foundry, ChronosBlocks.temporalFabricator);
		TechNode nano = child(fab, ChronosUnits.nanobot);
		child(nano, ChronosUnits.siphon);
		TechNode dis = child(nano, ChronosUnits.dissonance);
		child(dis, ChronosUnits.singularity);
	}

	private static TechNode child(TechNode parent, UnlockableContent content){
		return new TechNode(parent, content, content.researchRequirements());
	}

	private static void branch(){
		TechNode here = TechTree.context();
		fill(here);
	}
}
