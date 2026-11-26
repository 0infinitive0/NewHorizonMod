package newhorizon.content.tesla;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;

/** The Tesla Coil is researched under the Meltdown in the Serpulo tree. */
public class TeslaTechTree{
	public static void loadOtherPlanets(){
		if(TeslaBlocks.teslaCoil == null) return;
		TechNode meltdown = TechTree.all.find(n -> n.content == Blocks.meltdown);
		if(meltdown == null) return;
		new TechNode(meltdown, TeslaBlocks.teslaCoil, TeslaBlocks.teslaCoil.researchRequirements());
	}
}
