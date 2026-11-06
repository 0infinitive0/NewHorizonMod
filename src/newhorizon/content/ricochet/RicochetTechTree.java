package newhorizon.content.ricochet;

import mindustry.content.Blocks;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;

/** The Ricochet is researched under the Salvo in the Serpulo tree. */
public class RicochetTechTree{
	public static void loadOtherPlanets(){
		if(RicochetBlocks.ricochet == null) return;
		TechNode salvo = TechTree.all.find(n -> n.content == Blocks.salvo);
		if(salvo == null) return;
		new TechNode(salvo, RicochetBlocks.ricochet, RicochetBlocks.ricochet.researchRequirements());
	}
}
