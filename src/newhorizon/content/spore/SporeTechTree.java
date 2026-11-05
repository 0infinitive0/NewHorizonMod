package newhorizon.content.spore;

import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.content.UnitTypes;
import mindustry.game.Objectives;

/**
 * The Spore Mote is researched next to the crawler in the Serpulo tree: a sibling under the crawler's own parent, once the crawler is
 * researched. The crawler is not in the Erekir tree, so nothing is attached there.
 */
public class SporeTechTree{
	public static void loadOtherPlanets(){
		if(SporeContent.sporeMote == null) return;
		TechNode crawler = TechTree.all.find(n -> n.content == UnitTypes.crawler);
		if(crawler == null || crawler.parent == null) return;
		TechNode node = new TechNode(crawler.parent, SporeContent.sporeMote, SporeContent.sporeMote.researchRequirements());
		node.objectives.add(new Objectives.Research(UnitTypes.crawler));
	}
}
