package newhorizon.expand.block.hiemara;

import mindustry.world.blocks.power.PowerNode;

/** A power node that can also be placed on deep water. */
public class FloatingNode extends PowerNode{
	public FloatingNode(String name){
		super(name);
		
		placeableLiquid = true;
	}
}
