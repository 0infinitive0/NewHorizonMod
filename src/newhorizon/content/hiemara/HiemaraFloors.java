package newhorizon.content.hiemara;

import arc.struct.Seq;
import mindustry.content.Items;
import mindustry.type.Item;
import newhorizon.expand.block.env.RichWater;
import newhorizon.expand.block.production.filter.FilterBlock;

/** Deep water that is rich in one ore each, one for every ore of the base game. */
public class HiemaraFloors{
	/** Every ore that is filtered out of the water, in the order of their hardness. */
	public static Seq<Item> ores;
	
	public static void load(){
		ores = Seq.with(Items.sand, Items.scrap, Items.copper, Items.lead, Items.coal, Items.titanium, Items.beryllium, Items.thorium, Items.tungsten);
		
		FilterBlock.ores.clear();
		FilterBlock.ores.addAll(ores);
		
		for(Item ore : ores){
			new RichWater("rich-water-" + ore.name, ore);
		}
	}
}
