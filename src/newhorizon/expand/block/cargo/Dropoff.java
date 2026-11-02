package newhorizon.expand.block.cargo;

import mindustry.type.Item;

/** A block that cargo units bring items to: the depot, and the supply hub for the items it is short of. */
public interface Dropoff{
	/** @return whether a unit may bring this item. */
	boolean wants(Item item);
	
	/** @return how many more items fit. */
	int room();
	
	/** @return whether a core is next to the block, so that it hands everything on to the core. */
	boolean nextToCore();
}
