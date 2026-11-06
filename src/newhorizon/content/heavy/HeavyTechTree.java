package newhorizon.content.heavy;

import arc.struct.ObjectMap;
import mindustry.content.TechTree;
import mindustry.content.TechTree.TechNode;
import mindustry.world.Block;

/** Each heavy turret is researched under its original's node in the Serpulo tree. */
public class HeavyTechTree{
	public static void loadOtherPlanets(){
		for(ObjectMap.Entry<Block, Block> e : HeavyTurrets.heavyOf){
			TechNode node = TechTree.all.find(n -> n.content == e.key);
			if(node == null) continue;
			new TechNode(node, e.value, e.value.researchRequirements());
		}
	}
}
