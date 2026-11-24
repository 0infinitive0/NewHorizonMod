package newhorizon.expand.units.ai;

import arc.func.Boolf;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

/**
 * Which item a miner goes for. The highest tier (the item's hardness) first; an item the core is already full of is left alone.
 *
 * <p>The base game's test for "the core is full" is {@code core.acceptStack(...) == 0}, but in the campaign the core incinerates what is over
 * its capacity, so it accepts anything and the test never says full. The real test is the stock against the storage capacity.
 */
public class MinerPriority{
	/** Whether the core holds as much of the item as it can store. */
	public static boolean full(Building core, Item item){
		int cap = core instanceof CoreBuild c ? c.storageCapacity : core.getMaximumAccepted(item);
		return core.items.get(item) >= cap;
	}

	/**
	 * The wanted item that is not full with the highest hardness; among equals, the one the core has least of. Null when there is none.
	 */
	public static Item pick(Iterable<Item> items, Building core, Boolf<Item> wanted){
		Item best = null;
		for(Item i : items){
			if(!wanted.get(i) || full(core, i)) continue;
			if(best == null || i.hardness > best.hardness || (i.hardness == best.hardness && core.items.get(i) < core.items.get(best))){
				best = i;
			}
		}
		return best;
	}
}
