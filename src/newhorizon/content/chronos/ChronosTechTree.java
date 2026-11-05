package newhorizon.content.chronos;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives;
import mindustry.type.ItemStack;
import newhorizon.content.NHBlocks;

import static mindustry.content.TechTree.node;

/**
 * Chronos is researched on the New Horizon planet only: its branch hangs from the New Horizon tree. It is not part of the Serpulo or Erekir
 * campaigns (it used to hang from those trees too, behind each planet's silicon block). What it builds still works anywhere, since everything is
 * made from silicon, graphite and what they make.
 */
public class ChronosTechTree{
	/** Attaches the whole branch under the New Horizon root. Must be called inside it. */
	public static void load(){
		node(ChronosBlocks.temporalFoundry, ItemStack.with(Items.silicon, 400, Items.graphite, 300), Seq.with(new Objectives.Research(NHBlocks.jumpGatePrimary)), ChronosTechTree::branch);
	}

	/** Chronos is no longer attached to the vanilla trees of the other planets: nothing to do. Kept so that callers need not change. */
	public static void loadOtherPlanets(){
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
